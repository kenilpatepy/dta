package com.xyoye.local_component.ui.fragment.media

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.bridge.ServiceLifecycleBridge
import com.xyoye.common_component.database.DatabaseManager
import com.xyoye.common_component.network.Retrofit
import com.xyoye.common_component.network.request.httpRequest
import com.xyoye.common_component.utils.getFileName
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.data.CommonJsonData
import com.xyoye.data_component.entity.MediaLibraryEntity
import com.xyoye.data_component.enums.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by xyoye on 2020/7/27.
 */

class MediaViewModel : BaseViewModel() {

    val mediaLibWithStatusLiveData = MediatorLiveData<MutableList<MediaLibraryEntity>>().apply {
        val mediaLibrariesLiveData = DatabaseManager.instance.getMediaLibraryDao().getAll()
        val serviceStatusLiveData = ServiceLifecycleBridge.getScreencastProvideLiveData()
        //媒体库数据源
        addSource(mediaLibrariesLiveData) { libraries ->
            libraries.onEach {
                it.running =
                    it.mediaType == MediaType.SCREEN_CAST && it == serviceStatusLiveData.value
            }
            this.postValue(libraries)
        }
        //投屏服务状态数据源
        addSource(serviceStatusLiveData) { running ->
            val newData = this.value?.onEach {
                it.running = it.mediaType == MediaType.SCREEN_CAST && it == running
            } ?: mutableListOf()
            this.postValue(newData)
        }
    }

    fun initLocalStorage() {
        viewModelScope.launch(context = Dispatchers.IO) {
            //播放历史首条记录
            DatabaseManager.instance.getPlayHistoryDao().gitLastPlay(
                MediaType.LOCAL_STORAGE,
                MediaType.OTHER_STORAGE,
                MediaType.FTP_SERVER,
                MediaType.SMB_SERVER,
                MediaType.REMOTE_STORAGE,
                MediaType.WEBDAV_SERVER
            )?.apply {
                MediaLibraryEntity.HISTORY.url = url
            }

            //磁链播放首条记录
            DatabaseManager.instance.getPlayHistoryDao().gitLastPlay(MediaType.MAGNET_LINK)?.apply {
                MediaLibraryEntity.TORRENT.describe = getFileName(torrentPath)
            }

            //串流播放首条记录
            DatabaseManager.instance.getPlayHistoryDao().gitLastPlay(MediaType.STREAM_LINK)?.apply {
                MediaLibraryEntity.STREAM.describe = url
            }

            DatabaseManager.instance.getMediaLibraryDao()
                .insert(
                    MediaLibraryEntity.LOCAL,
                    MediaLibraryEntity.STREAM,
                    MediaLibraryEntity.TORRENT,
                    MediaLibraryEntity.HISTORY
                )
        }
    }

    fun deleteStorage(data: MediaLibraryEntity) {
        viewModelScope.launch(context = Dispatchers.IO) {
            DatabaseManager.instance.getMediaLibraryDao()
                .delete(data.url, data.mediaType)
        }
    }

    fun checkScreenDeviceRunning(receiver: MediaLibraryEntity) {
        httpRequest<CommonJsonData>(viewModelScope) {

            api {
                Retrofit.screencastService.init(
                    host = receiver.screencastAddress,
                    port = receiver.port,
                    authorization = receiver.password,
                )
            }

            onStart { showLoading() }

            onSuccess {
                if (it. success) {
                    ToastCenter.showSuccess("The screen projection device is connected normally, please go to other media libraries to select files to cast the screen")
                } else {
                    ToastCenter.showError(it.errorMessage ?: "Failed to connect to the projection device, please confirm that the projection device has enabled receiving service")
                }
            }

            onError {
                ToastCenter.showError("Failed to connect to the projection device, please confirm that the projection device has enabled receiving service")
            }

            onComplete { hideLoading() }
        }
    }
}