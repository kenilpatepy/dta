package com.xyoye.player_component.ui.activities.player

import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.database.DatabaseManager
import com.xyoye.common_component.network.Retrofit
import com.xyoye.common_component.network.request.httpRequest
import com.xyoye.common_component.source.base.BaseVideoSource
import com.xyoye.common_component.utils.DDLog
import com.xyoye.common_component.utils.DanmuUtils
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.bean.SendDanmuBean
import com.xyoye.data_component.data.SendDanmuData
import com.xyoye.data_component.entity.DanmuBlockEntity
import com.xyoye.data_component.enums.MediaType
import kotlinx.coroutines.launch
import master.flame.danmaku.danmaku.model.BaseDanmaku
import java.math.BigDecimal

class PlayerViewModel : BaseViewModel() {

    val localDanmuBlockLiveData = DatabaseManager.instance.getDanmuBlockDao().getAll(false)
    val cloudDanmuBlockLiveData = DatabaseManager.instance.getDanmuBlockDao().getAll(true)

    fun storeDanmuSourceChange(videoSource: BaseVideoSource) {
        viewModelScope.launch {
            DatabaseManager.instance.getPlayHistoryDao().updateDanmu(
                videoSource.getUniqueKey(),
                videoSource.getMediaType(),
                videoSource.getDanmuPath(),
                videoSource.getEpisodeId()
            )
        }
    }

    fun storeSubtitleSourceChange(videoSource: BaseVideoSource) {
        viewModelScope.launch {
            DatabaseManager.instance.getPlayHistoryDao().updateSubtitle(
                videoSource.getUniqueKey(),
                videoSource.getMediaType(),
                videoSource.getSubtitlePath()
            )
        }
    }

    fun addDanmuBlock(keyword: String, isRegex: Boolean) {
        viewModelScope.launch {
            DatabaseManager.instance.getDanmuBlockDao().insert(
                DanmuBlockEntity(0, keyword, isRegex)
            )
        }
    }

    fun removeDanmuBlock(id: Int) {
        viewModelScope.launch {
            DatabaseManager.instance.getDanmuBlockDao().delete(id)
        }
    }

    fun sendDanmu(episodeId: Int, danmuPath: String?, sendDanmuBean: SendDanmuBean) {
        if (episodeId > 0) {
            sendDanmuToServer(sendDanmuBean, episodeId)
        }
        if (danmuPath != null) {
            writeDanmuToFile(sendDanmuBean, danmuPath)
        }
    }

    private fun sendDanmuToServer(sendDanmuBean: SendDanmuBean, episodeId: Int) {
        httpRequest<SendDanmuData>(viewModelScope) {

            api {
                val timeDecimal = BigDecimal(sendDanmuBean.position.toDouble() / 1000)
                    .setScale(2, BigDecimal.ROUND_HALF_UP)

                val mode = when {
                    sendDanmuBean.isScroll -> BaseDanmaku.TYPE_SCROLL_RL
                    sendDanmuBean.isTop -> BaseDanmaku.TYPE_FIX_TOP
                    else -> BaseDanmaku.TYPE_FIX_BOTTOM
                }
                val color = sendDanmuBean.color and 0x00FFFFFF

                val params = hashMapOf<String, String>()
                params["time"] = timeDecimal.toString()
                params["mode"] = mode.toString()
                params["color"] = color.toString()
                params["comment"] = sendDanmuBean.text
                Retrofit.service.sendDanmu(episodeId.toString(), params)
            }

            onSuccess {
                DDLog.i("Send barrage successfully")
            }

            onError {
                ToastCenter.showOriginalToast("Failed to send barrage\n x${it.code} ${it.msg}")            }
        }
    }

    private fun writeDanmuToFile(sendDanmuBean: SendDanmuBean, danmuPath: String) {
        val time = BigDecimal(sendDanmuBean.position.toDouble() / 1000)
            .setScale(2, BigDecimal.ROUND_HALF_UP)
            .toString()

        val mode = when {
            sendDanmuBean.isScroll -> BaseDanmaku.TYPE_SCROLL_RL
            sendDanmuBean.isTop -> BaseDanmaku.TYPE_FIX_TOP
            else -> BaseDanmaku.TYPE_FIX_BOTTOM
        }

        val unixTime = (System.currentTimeMillis() / 1000f).toInt().toString()
        val color = sendDanmuBean.color and 0x00FFFFFF

        val danmuText = "<d p=\"$time,$mode,25,$color,$unixTime,0,0,0\">${sendDanmuBean.text}</d>"

        DanmuUtils.appendDanmu(danmuPath, danmuText)
    }
}