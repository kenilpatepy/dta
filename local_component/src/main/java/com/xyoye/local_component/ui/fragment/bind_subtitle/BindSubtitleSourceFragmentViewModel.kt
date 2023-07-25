package com.xyoye.local_component.ui.fragment.bind_subtitle

import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.config.SubtitleConfig
import com.xyoye.common_component.database.DatabaseManager
import com.xyoye.common_component.network.Retrofit
import com.xyoye.common_component.network.request.httpRequest
import com.xyoye.common_component.storage.file.StorageFile
import com.xyoye.common_component.utils.getFileNameNoExtension
import com.xyoye.common_component.utils.subtitle.SubtitleMatchHelper
import com.xyoye.common_component.utils.subtitle.SubtitleSearchHelper
import com.xyoye.common_component.utils.subtitle.SubtitleUtils
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.data.SubDetailData
import com.xyoye.data_component.data.SubtitleSourceBean
import com.xyoye.data_component.data.SubtitleSubData
import com.xyoye.data_component.entity.PlayHistoryEntity
import com.xyoye.data_component.enums.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody


/**
 * Created by xyoye on 2022/1/25
 */
class BindSubtitleSourceFragmentViewModel : BaseViewModel() {

    private val searchSubtitleRepository = SubtitleSearchHelper(viewModelScope)

    lateinit var storageFile: StorageFile

    val subtitleSearchLiveData = searchSubtitleRepository.subtitleLiveData
    val subtitleMatchLiveData = MutableLiveData<PagingData<SubtitleSourceBean>>()
    val searchSubtitleDetailLiveData = MutableLiveData<SubDetailData>()
    val sourceRefreshLiveData = MutableLiveData<Any>()
    val unzipResultLiveData = MutableLiveData<String>()

    fun matchSubtitle() {
        if (storageFile.storage.library.mediaType != MediaType.LOCAL_STORAGE) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            showLoading()
            val subtitleSources = SubtitleMatchHelper.matchSubtitle(storageFile.filePath())
            val matchPagingData = PagingData.from(subtitleSources)
            hideLoading()
            subtitleMatchLiveData.postValue(matchPagingData)
        }
    }

    fun searchSubtitle(text: String) {
        searchSubtitleRepository.search(text)
    }

    fun detailSearchSubtitle(sourceBean: SubtitleSourceBean) {
        val shooterSecret = SubtitleConfig.getShooterSecret().orEmpty()
        httpRequest<SubtitleSubData>(viewModelScope) {
            onStart { showLoading() }

            api {
                Retrofit.extService.searchSubtitleDetail(
                    shooterSecret,
                    sourceBean.id.toString()
                )
            }

            onSuccess {
                if (it.sub?.subs == null || it.sub!!.subs!!.size == 0) {
                    ToastCenter.showError("Failed to get subtitle details")
                    return@onSuccess
                }
                searchSubtitleDetailLiveData.postValue(it.sub!!.subs!![0])
            }

            onError { showNetworkError(it) }

            onComplete { hideLoading() }
        }
    }

    fun downloadSearchSubtitle(fileName: String?, sourceUrl: String, unzip: Boolean = false) {
        httpRequest(viewModelScope) {
            onStart { showLoading() }

            api {
                val name = if (TextUtils.isEmpty(fileName)) {
                    "${getFileNameNoExtension(storageFile.filePath())}.ass"
                } else {
                    fileName!!
                }

                val responseBody = Retrofit.extService.downloadResource(sourceUrl)
                if (unzip) {
                    unzipSaveSubtitle(name, responseBody)
                } else {
                    saveSubtitle(name, responseBody)
                }
            }

            onSuccess {

            }

            onError { showNetworkError(it) }

            onComplete { hideLoading() }
        }
    }

    private suspend fun unzipSaveSubtitle(fileName: String, responseBody: ResponseBody) {
        val unzipDirPath = SubtitleUtils.saveAndUnzipFile(fileName, responseBody.byteStream()).orEmpty()
        if (unzipDirPath.isEmpty()) {

            ToastCenter.showError("Failed to decompress the subtitle file, please try to decompress it manually")
            return
        }
        unzipResultLiveData.postValue(unzipDirPath)
    }

    private fun saveSubtitle(fileName: String, responseBody: ResponseBody) {
        val subtitlePath = SubtitleUtils.saveSubtitle(fileName, responseBody.byteStream())
        if (subtitlePath != null) {
            databaseSubtitle(subtitlePath)
            ToastCenter.showSuccess("Binding subtitles successfully!")        }
    }

    fun unbindSubtitle() {
        viewModelScope.launch(Dispatchers.IO) {
            databaseSubtitle(null)
        }
    }

    fun databaseSubtitle(filePath: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val storageId = storageFile.storage.library.id
            val history = DatabaseManager.instance.getPlayHistoryDao()
                .getPlayHistory(storageFile.uniqueKey(), storageId)

            if (history != null) {
                history.subtitlePath = filePath
                DatabaseManager.instance.getPlayHistoryDao().insert(history)
                sourceRefreshLiveData.postValue(Any())
                return@launch
            }

            val newHistory = PlayHistoryEntity(
                0,
                "",
                "",
                mediaType = storageFile.storage.library.mediaType,
                uniqueKey = storageFile.uniqueKey(),
                subtitlePath = filePath,
                storageId = storageId,
            )
            DatabaseManager.instance.getPlayHistoryDao().insert(newHistory)
            sourceRefreshLiveData.postValue(Any())
        }
    }
}