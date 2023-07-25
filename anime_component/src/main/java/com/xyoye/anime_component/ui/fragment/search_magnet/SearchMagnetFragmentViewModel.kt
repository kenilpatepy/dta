package com.xyoye.anime_component.ui.fragment.search_magnet

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.config.AppConfig
import com.xyoye.common_component.database.DatabaseManager
import com.xyoye.common_component.network.Retrofit
import com.xyoye.common_component.network.request.httpRequest
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.data.MagnetData
import com.xyoye.data_component.data.MagnetResourceData
import com.xyoye.data_component.data.MagnetSubgroupData
import com.xyoye.data_component.data.MagnetTypeData
import com.xyoye.data_component.entity.MagnetScreenEntity
import com.xyoye.data_component.entity.MagnetSearchHistoryEntity
import com.xyoye.data_component.enums.MagnetScreenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchMagnetFragmentViewModel : BaseViewModel() {

    val magnetTypeId = ObservableField<Int>()
    val magnetTypeText = ObservableField("All Categories")
    val magnetSubgroupId = ObservableField<Int>()
    val magnetSubgroupText = ObservableField("All subtitle groups")

    val searchText = ObservableField<String>()

    val magnetLiveData = MutableLiveData<MutableList<MagnetData>>()

    val searchHistoryLiveData = DatabaseManager.instance.getMagnetSearchHistoryDao().getAll()

    var magnetSubgroupData = MutableLiveData<MutableList<MagnetScreenEntity>>()
    var magnetTypeData = MutableLiveData<MutableList<MagnetScreenEntity>>()

    val domainErrorLiveData = MutableLiveData<Boolean>()

    fun search() {
        val searchTextStr = searchText.get() ?: ""
        if (searchTextStr.isEmpty()) {
            ToastCenter.showWarning("Please enter search criteria")
            return
        }

        if (AppConfig.getMagnetResDomain() == null){
            domainErrorLiveData.postValue(true)
            return
        }

        viewModelScope.launch(context = Dispatchers.Main) {
            DatabaseManager.instance
                .getMagnetSearchHistoryDao()
                .insert(MagnetSearchHistoryEntity(searchTextStr))
        }

        httpRequest<MagnetResourceData>(viewModelScope) {
            onStart { showLoading() }

            api {
                val subgroupId = magnetSubgroupId.get() ?: -1
                val typeId = magnetTypeId.get() ?: -1
                val subgroupIdText = if (subgroupId < 0) "" else subgroupId.toString()
                val typeIdText = if (typeId < 0) "" else typeId.toString()

                //搜索结果
                Retrofit.resService.searchMagnet(
                    searchTextStr,
                    typeIdText,
                    subgroupIdText
                )
            }

            onSuccess {
                val resources = it.Resources ?: mutableListOf()
                magnetLiveData.postValue(resources)
            }

            onError {
                showNetworkError(it)
            }

            onComplete { hideLoading() }
        }
    }

    fun deleteSearchHistory(searchText: String) {
        viewModelScope.launch(context = Dispatchers.Main) {
            DatabaseManager.instance
                .getMagnetSearchHistoryDao()
                .deleteByText(searchText)
        }
    }

    fun deleteAllSearchHistory() {
        viewModelScope.launch(context = Dispatchers.Main) {
            DatabaseManager.instance
                .getMagnetSearchHistoryDao()
                .deleteAll()
        }
    }

    fun getMagnetSubgroup() {
        val subgroupData = magnetSubgroupData.value
        if (subgroupData != null) {
            magnetSubgroupData.postValue(subgroupData)
            return
        }

        if (AppConfig.getMagnetResDomain() == null){
            domainErrorLiveData.postValue(true)
            return
        }

        httpRequest<MagnetSubgroupData>(viewModelScope) {

            onStart { showLoading() }

            api {
                Retrofit.resService.getMagnetSubgroup()
            }

            onSuccess { subgroupData ->
                val screenEntities = subgroupData.Subgroups.map {
                    MagnetScreenEntity(it.Id, it.Name, MagnetScreenType.SUBGROUP)
                }
                magnetSubgroupData.postValue(screenEntities.toMutableList())
            }

            onError { showNetworkError(it) }

            onComplete { hideLoading() }
        }
    }

    fun getMagnetType() {
        val typeData = magnetTypeData.value
        if (typeData != null) {
            magnetTypeData.postValue(typeData)
            return
        }

        if (AppConfig.getMagnetResDomain() == null){
            domainErrorLiveData.postValue(true)
            return
        }

        httpRequest<MagnetTypeData>(viewModelScope) {

            onStart { showLoading() }

            api {
                Retrofit.resService.getMagnetType()
            }

            onSuccess { typeData ->
                val screenEntities = typeData.Types.map {
                    MagnetScreenEntity(it.Id, it.Name, MagnetScreenType.TYPE)
                }
                magnetTypeData.postValue(screenEntities.toMutableList())
            }

            onError { showNetworkError(it) }

            onComplete { hideLoading() }
        }
    }
}