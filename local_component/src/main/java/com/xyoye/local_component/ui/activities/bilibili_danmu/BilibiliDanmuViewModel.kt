package com.xyoye.local_component.ui.activities.bilibili_danmu

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.network.Retrofit
import com.xyoye.common_component.utils.DanmuUtils
import com.xyoye.common_component.utils.IOUtils
import com.xyoye.common_component.utils.JsonHelper
import com.xyoye.common_component.utils.PathHelper
import com.xyoye.data_component.data.CidBungumiData
import com.xyoye.data_component.data.CidVideoBean
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.regex.Pattern
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

class BilibiliDanmuViewModel : BaseViewModel() {

    val downloadMessageLiveData = MutableLiveData<String>()
    val clearMessageLiveData = MutableLiveData<Boolean>()

    fun downloadByCode(code: String, isAvCode: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            clearDownloadMessage()

            val message = if (isAvCode) "Download with AV code, AV code: $code" else "Download with BV code, BV code: $code"

            sendDownloadMessage("$message\n\nGet CID")
            val cidInfo = getCodeCid(isAvCode, code)
            if (cidInfo == null) {
                sendDownloadMessage("Failed to get CID")
                return@launch
            }

            sendDownloadMessage("Obtain the CID successfully\n\nStart to get the barrage content")
            val xmlContent = getXmlContentByCid(cidInfo.first)
            if (xmlContent. isNullOrEmpty()) {
                sendDownloadMessage("Failed to get barrage content")
                return@launch
            }

            sendDownloadMessage("Obtain the barrage content successfully\n\nStart saving the barrage content")
            val danmuFileName = cidInfo.second + ".xml"
            val danmuPath = DanmuUtils.saveDanmu(danmuFileName, null, xmlContent)
            if (danmuPath.isNullOrEmpty()) {
                sendDownloadMessage("Failed to save barrage content")
                return@launch
            }
            sendDownloadMessage("Save the barrage content successfully\n\nThe file has been saved to: $danmuPath")
        }
    }

    fun downloadByUrl(url: String) {
        viewModelScope. launch(Dispatchers.IO) {
            clearDownloadMessage()

            val message = "Download in video link mode, video link: $url"

            if (isBangumiUrl(url)) {
                sendDownloadMessage("$message\n\nGet the CID of the drama list")
                val cidInfo = getBungumiCid(url)
                if (cidInfo == null) {
                    sendDownloadMessage("Failed to get the CID of the drama list")
                    return@launch
                }
                sendDownloadMessage("Get the CID of the drama list successfully, the number of CID: ${cidInfo.first.size}\n\nStart downloading the barrage")

                for ((index, cid) in cidInfo. first. withIndex()) {
                    var downloadMessage = "episode ${index + 1}"
                    val xmlContent = getXmlContentByCid(cid)
                    if (xmlContent == null) {
                        downloadMessage += "Failed to get barrage content"
                        sendDownloadMessage(downloadMessage)
                        continue
                    }
                    downloadMessage += "Get the barrage content successfully"
                    val fileName = "${index + 1}.xml"
                    val danmuPath = DanmuUtils.saveDanmu(fileName, cidInfo.second, xmlContent)
                    if (danmuPath == null) {
                        downloadMessage += "Failed to save barrage"
                        sendDownloadMessage(downloadMessage)
                        continue
                    }
                    downloadMessage += "Save the barrage successfully"
                    sendDownloadMessage(downloadMessage)
                }
                val danmuFolder = File(PathHelper.getDanmuDirectory(), cidInfo.second).absolutePath
                sendDownloadMessage("\nThe barrage download of the drama list is complete\nThe file has been saved to: $danmuFolder\"")
            } else {
                sendDownloadMessage("$message\n\nGet video CID")
                val cidInfo = getVideoCid(url)
                if (cidInfo == null) {
                    sendDownloadMessage("Failed to get video CID")
                    return@launch
                }

                sendDownloadMessage("Obtain video CID successfully\n\nStart to obtain barrage content")
                val xmlContent = getXmlContentByCid(cidInfo.first)
                if (xmlContent. isNullOrEmpty()) {
                    sendDownloadMessage("Failed to get barrage content")
                    return@launch
                }

                sendDownloadMessage("Obtain the barrage content successfully\n\nStart saving the barrage content")
                val danmuFileName = cidInfo.second + ".xml"
                val danmuPath = DanmuUtils. saveDanmu(danmuFileName, null, xmlContent)
                if (danmuPath. isNullOrEmpty()) {
                    sendDownloadMessage("Failed to save barrage content")
                    return@launch
                }
                sendDownloadMessage("Save the barrage content successfully\n\nThe file has been saved to: $danmuPath")
            }
        }
    }
    private suspend fun getVideoCid(url: String): Pair<Long, String>? {
        return viewModelScope.async(Dispatchers.IO) {

            try {
                val htmlElement = Jsoup.connect(url).timeout(10 * 1000).get().toString()
                //匹配java script里的json数据
                val pattern = Pattern.compile("(__INITIAL_STATE__=).*(;\\(function)")
                val matcher = pattern.matcher(htmlElement)
                if (matcher.find()) {
                    var jsonText = matcher.group(0) ?: return@async null
                    if (jsonText.isNotEmpty()) {
                        jsonText = jsonText.substring(18)
                        jsonText = jsonText.substring(0, jsonText.length - 10)
                        val cidBean =
                            JsonHelper.parseJson<CidVideoBean>(jsonText) ?: return@async null
                        //只需要标题和cid
                        return@async Pair(cidBean.videoData.cid, cidBean.videoData.title)
                    }
                }
            } catch (t: Throwable) {
                sendDownloadMessage("mistake：${t.message}")
                t.printStackTrace()
            }
            null
        }.await()
    }

    private suspend fun getBungumiCid(url: String): Pair<MutableList<Long>, String>? {
        return viewModelScope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
            try {
                val htmlElement = Jsoup.connect(url).timeout(10 * 1000).get().toString()
                val pattern = Pattern.compile("(__INITIAL_STATE__=).*(;\\(function)")
                val matcher = pattern.matcher(htmlElement)
                if (matcher.find()) {
                    var jsonText = matcher.group(0) ?: return@async null
                    if (jsonText.isNotEmpty()) {
                        jsonText = jsonText.substring(18)
                        jsonText = jsonText.substring(0, jsonText.length - 10)
                        val cidBean =
                            JsonHelper.parseJson<CidBungumiData>(jsonText) ?: return@async null
                        //只需要标题和cid
                        val cidList = mutableListOf<Long>()
                        cidBean.epList.forEach {
                            cidList.add(it.cid)
                        }
                        return@async Pair(cidList, cidBean.mediaInfo.title)
                    }
                }
            } catch (t: Throwable) {
                sendDownloadMessage("mistake：${t.message}")
                t.printStackTrace()
            }
            null
        }.await()

    }

    private suspend fun getCodeCid(isAvCode: Boolean, value: String): Pair<Long, String>? {
        return viewModelScope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
            val key = if (isAvCode) "aid" else "bvid"
            val apiUrl = "https://api.bilibili.com/x/web-interface/view?$key=$value"
            try {
                val cidData = Retrofit.extService.getCidInfo(apiUrl)
                if (cidData.code == 0 && cidData.data != null) {
                    val videoTitle = cidData.data!!.title ?: "Unknown barrage"
                    return@async Pair(cidData.data!!.cid, videoTitle)
                }
            } catch (t: Throwable) {
                sendDownloadMessage("mistake：${t.message}")
                t.printStackTrace()
            }
            null
        }.await()
    }

    private suspend fun getXmlContentByCid(cid: Long): String? {
        return viewModelScope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
            val url = "http://comment.bilibili.com/$cid.xml"
            val header = mapOf(Pair("Accept-Encoding", "gzip,deflate"))

            var inputStream: InputStream? = null
            var inflaterInputStream: InflaterInputStream? = null
            var scanner: Scanner? = null

            var xmlContent: String? = null
            try {
                val responseBody = Retrofit.extService.downloadResource(url, header)

                inputStream = responseBody.byteStream()
                inflaterInputStream = InflaterInputStream(inputStream, Inflater(true))
                scanner = Scanner(inflaterInputStream, Charsets.UTF_8.name())

                val contentBuilder = StringBuilder()
                while (scanner.hasNext()) {
                    contentBuilder.append(scanner.nextLine())
                }

                xmlContent = contentBuilder.toString()
            } catch (e: Throwable) {
                sendDownloadMessage("mistake：${e.message}")
                e.printStackTrace()
            } finally {
                IOUtils.closeIO(scanner)
                IOUtils.closeIO(inflaterInputStream)
                IOUtils.closeIO(inputStream)
            }
            xmlContent
        }.await()
    }

    private fun isBangumiUrl(url: String): Boolean {
        return url.contains("www.bilibili.com/bangumi") || url.contains("m.bilibili.com/bangumi")
    }

    private fun sendDownloadMessage(message: String) {
        downloadMessageLiveData.postValue(message)
    }

    private fun clearDownloadMessage() {
        clearMessageLiveData.postValue(true)
    }
}