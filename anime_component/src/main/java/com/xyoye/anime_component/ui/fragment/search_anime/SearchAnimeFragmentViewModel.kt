package com.xyoye.anime_component.ui.fragment.search_anime

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.config.UserConfig
import com.xyoye.common_component.database.DatabaseManager
import com.xyoye.common_component.network.Retrofit
import com.xyoye.common_component.network.request.httpRequest
import com.xyoye.common_component.utils.stringCompare
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.data.AnimeData
import com.xyoye.data_component.data.CommonTypeData
import com.xyoye.data_component.data.SearchAnimeData
import com.xyoye.data_component.entity.AnimeSearchHistoryEntity
import com.xyoye.data_component.enums.AnimeSortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SearchAnimeFragmentViewModel : BaseViewModel() {
    private val animeTypeData = mutableListOf(
        CommonTypeData("TV Animation", "tvseries"),
        CommonTypeData("movie version", "movie"),
        CommonTypeData("OVA", "ova"),
        CommonTypeData("Japanese Drama", "jpdrama"),
        CommonTypeData("Japanese movie", "jpmovie"),
        CommonTypeData("Webcast", "web"),
        CommonTypeData("TV Special", "tvspecial"),
        CommonTypeData("unknown category", "unknown"),
        CommonTypeData("MV", "musicvideo"),
        CommonTypeData("other", "other")
    )

    private val sortTypeData = mutableListOf(
        CommonTypeData("release date", AnimeSortType.DATE.value),
        CommonTypeData("Name", AnimeSortType.NAME.value),
        CommonTypeData("Rating", AnimeSortType.RATING.value),
        CommonTypeData("follow", AnimeSortType.FOLLOW.value)
    )

    val screenSpanCount = 4

    private var sortType = AnimeSortType.NONE
    private var searchType: String? = null
    private lateinit var searchAnimeData: SearchAnimeData

    val searchText = ObservableField<String>()
    val isTypeExpanded = ObservableField(false)
    val isCheckedType = ObservableField(false)
    val checkedAnimeType = ObservableField("type: ")

    val animeTypeLiveData = MutableLiveData<MutableList<CommonTypeData>>()
    val animeSortLiveData = MutableLiveData<MutableList<CommonTypeData>>()
    val searchHistoryLiveData = DatabaseManager.instance.getAnimeSearchHistoryDao().getAll()

    val animeLiveData = MutableLiveData<MutableList<AnimeData>>()

    fun search() {
        val searchWord = searchText.get() ?: ""
        if (searchWord.length < 2) {
            ToastCenter.showWarning("The keyword is too short, the length must be at least 2")
            return
        }

        viewModelScope.launch(context = Dispatchers.Main) {
            DatabaseManager.instance
                .getAnimeSearchHistoryDao()
                .insert(AnimeSearchHistoryEntity(searchText.get()!!))
        }

        httpRequest(viewModelScope) {
            api {
                Retrofit.service.searchAnime(searchWord, searchType)
            }

            onSuccess {
                searchAnimeData = it
                showSearchResult()
            }

            onError {
                showNetworkError(it)
            }
        }
    }

    fun toggleExpand() {
        val isExpanded = isTypeExpanded.get() ?: false
        isTypeExpanded.set(!isExpanded)
        getAnimeType()
    }

    fun getAnimeType() {
        animeTypeLiveData.postValue(
            if (isTypeExpanded.get() == false && animeTypeData.size > screenSpanCount) {
                animeTypeData.subList(0, screenSpanCount)
            } else {
                animeTypeData
            }
        )
    }

    fun getAnimeSort() {
        animeSortLiveData.postValue(sortTypeData)
    }

    fun checkType(position: Int) {
        var checkedIndex = -1
        for (index in animeTypeData.indices) {
            if (animeTypeData[index].isChecked) {
                checkedIndex = index
                break
            }
        }

        when (checkedIndex) {
            -1 -> {
                animeTypeData[position].isChecked = true
                searchType = animeTypeData[position].typeId
                checkedAnimeType.set(animeTypeData[position].typeName)
                isCheckedType.set(true)
            }
            position -> {
                searchType = null
                animeTypeData[position].isChecked = false
                checkedAnimeType.set("type: ")
                isCheckedType.set(false)
            }
            else -> {
                animeTypeData[position].isChecked = true
                searchType = animeTypeData[position].typeId
                checkedAnimeType.set(animeTypeData[position].typeName)
                isCheckedType.set(true)

                animeTypeData[checkedIndex].isChecked = false
            }
        }
        getAnimeType()
        if (!searchText.get().isNullOrEmpty() && searchText.get()!!.length > 1) {
            search()
        }
    }

    fun checkSort(position: Int) {
        if (!this::searchAnimeData.isInitialized) {
            return
        }

        if (!UserConfig.isUserLoggedIn()
            && AnimeSortType.formValue(sortTypeData[position].typeId) == AnimeSortType.FOLLOW
        ) {
            ToastCenter.showWarning("Please log in to do this")
            return
        }

        var checkedIndex = -1
        for (index in sortTypeData.indices) {
            if (sortTypeData[index].isChecked) {
                checkedIndex = index
                break
            }
        }

        when (checkedIndex) {
            -1 -> {
                sortTypeData[position].isChecked = true
                sortType = AnimeSortType.formValue(sortTypeData[position].typeId)
            }
            position -> {
                sortTypeData[position].isChecked = false
                sortType = AnimeSortType.NONE
            }
            else -> {
                sortTypeData[position].isChecked = true
                sortType = AnimeSortType.formValue(sortTypeData[position].typeId)

                sortTypeData[checkedIndex].isChecked = false
            }
        }
        getAnimeSort()
        showSearchResult()
    }

    fun deleteSearchHistory(searchText: String){
        viewModelScope.launch(context = Dispatchers.Main) {
            DatabaseManager.instance
                .getAnimeSearchHistoryDao()
                .deleteByText(searchText)
        }
    }

    fun deleteAllSearchHistory(){
        viewModelScope.launch(context = Dispatchers.Main) {
            DatabaseManager.instance
                .getAnimeSearchHistoryDao()
                .deleteAll()
        }
    }

    private fun showSearchResult() {
        if (sortType == AnimeSortType.NONE) {
            animeLiveData.postValue(searchAnimeData.animes)
            return
        }
        if (!UserConfig.isUserLoggedIn() && sortType == AnimeSortType.FOLLOW) {
            animeLiveData.postValue(searchAnimeData.animes)
            return
        }
        if (this::searchAnimeData.isInitialized) {
            val sortedList = mutableListOf<AnimeData>().also {
                it.addAll(searchAnimeData.animes)
            }
            Collections.sort(sortedList, kotlin.Comparator { o1, o2 ->
                return@Comparator when (sortType) {
                    AnimeSortType.FOLLOW -> o1.isFavorited.compareTo(o2.isFavorited)

                    AnimeSortType.RATING -> o1.rating.compareTo(o2.rating)

                    AnimeSortType.DATE -> stringCompare(o2.startDate, o1.startDate)

                    AnimeSortType.NAME -> stringCompare(o1.animeTitle, o2.animeTitle)

                    else -> 0
                }
            })
            animeLiveData.postValue(sortedList)
        }
    }
}