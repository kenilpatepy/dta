package com.xyoye.local_component.ui.activities.shooter_subtitle

import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.config.SubtitleConfig
import com.xyoye.common_component.network.Retrofit
import com.xyoye.common_component.network.request.httpRequest
import com.xyoye.common_component.utils.subtitle.SubtitleSearchHelper
import com.xyoye.common_component.utils.subtitle.SubtitleUtils
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.data.SubDetailData
import com.xyoye.data_component.data.SubtitleSubData

class ShooterSubtitleViewModel : BaseViewModel() {

    private val searchSubtitleRepository = SubtitleSearchHelper(viewModelScope)
    val searchSubDetailLiveData = MutableLiveData<SubDetailData>()

    val searchSubtitleLiveData = searchSubtitleRepository.subtitleLiveData

    /**
     * 搜索字幕
     */
    fun searchSubtitle(videoName: String) {
        searchSubtitleRepository.search(videoName)
    }

    /**
     * 获取搜索字幕详情
     */
    fun getSearchSubDetail(subtitleId: Int) {
        val shooterSecret = SubtitleConfig.getShooterSecret() ?: ""
        httpRequest<SubtitleSubData>(viewModelScope) {
            onStart { showLoading() }

            api { Retrofit.extService.searchSubtitleDetail(shooterSecret, subtitleId.toString()) }

            onSuccess {
                if (it.sub?.subs == null || it.sub!!.subs!!.size == 0) {
                    ToastCenter.showError("Failed to get subtitle details")
                    return@onSuccess
                }
                searchSubDetailLiveData.postValue(it.sub!!.subs!![0])
            }

            onError { showNetworkError(it) }

            onComplete { hideLoading() }
        }
    }

    fun downloadSubtitle(fileName: String, downloadUrl: String) {
        httpRequest<String?>(viewModelScope) {
            onStart { showLoading() }

            api {
                val responseBody = Retrofit.extService.downloadResource(downloadUrl)
                SubtitleUtils.saveSubtitle(fileName, responseBody.byteStream())
            }

            onSuccess {
                if (it != null) {
                    ToastCenter.showSuccess("Subtitles downloaded successfully：$it", Toast.LENGTH_LONG)
                } else {
                    ToastCenter.showError("Failed to save subtitles")
                }
            }

            onError { showNetworkError(it) }

            onComplete { hideLoading() }
        }
    }

    /**
     * 下载压缩文件，并解压
     */
    fun downloadAndUnzipFile(fileName: String, url: String) {
        httpRequest<Any>(viewModelScope) {
            onStart { showLoading() }

            api {
                val responseBody = Retrofit.extService.downloadResource(url)
                val unzipDirPath = SubtitleUtils.saveAndUnzipFile(fileName, responseBody.byteStream())
                if (unzipDirPath. isNullOrEmpty()) {
                    ToastCenter.showError("Failed to decompress the subtitle file, please try to decompress it manually")
                } else {
                    ToastCenter.showSuccess("Subtitle download successful: $unzipDirPath", Toast.LENGTH_LONG)
                }
            }

            onError { showNetworkError(it) }

            onComplete { hideLoading() }
        }
    }
}