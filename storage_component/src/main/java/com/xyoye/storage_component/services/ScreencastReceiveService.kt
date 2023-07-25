package com.xyoye.storage_component.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.alibaba.android.arouter.launcher.ARouter
import com.xyoye.common_component.bridge.ServiceLifecycleBridge
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.config.ScreencastConfig
import com.xyoye.common_component.extension.isServiceRunning
import com.xyoye.common_component.extension.resumeWhenAlive
import com.xyoye.common_component.notification.Notifications
import com.xyoye.common_component.source.VideoSourceManager
import com.xyoye.common_component.source.factory.StorageVideoSourceFactory
import com.xyoye.common_component.storage.StorageFactory
import com.xyoye.common_component.storage.impl.ScreencastStorage
import com.xyoye.common_component.utils.ActivityHelper
import com.xyoye.common_component.utils.screencast.ScreencastHandler
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.data.screeencast.ScreencastData
import com.xyoye.data_component.data.screeencast.ScreencastVideoData
import com.xyoye.data_component.entity.MediaLibraryEntity
import com.xyoye.data_component.enums.MediaType
import com.xyoye.storage_component.utils.screencast.receiver.HttpServer
import com.xyoye.storage_component.utils.screencast.receiver.UdpServer
import kotlinx.coroutines.*
import kotlin.coroutines.resume

/**
 * Created by xyoye on 2022/9/16
 */

interface ScreencastReceiveHandler {
    fun onReceiveVideo(screencastData: ScreencastData)
}

class ScreencastReceiveService : Service(), ScreencastReceiveHandler {
    private var httpServer: HttpServer? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var notifier: ScreencastReceiverNotifier
    private lateinit var multicastLock: WifiManager.MulticastLock
    private var multicastJob: Job? = null

    companion object {
        private const val KEY_SERVER_PORT = "key_server_port"
        private const val KEY_SERVER_PASSWORD = "key_server_password"

        fun isRunning(context: Context): Boolean {
            return context.isServiceRunning(ScreencastReceiveService::class.java)
        }

        fun start(context: Context, httpPort: Int, password: String?) {
            val intent = Intent(context, ScreencastReceiveService::class.java)
            intent.putExtra(KEY_SERVER_PORT, httpPort)
            intent.putExtra(KEY_SERVER_PASSWORD, password)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreencastReceiveService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        ServiceLifecycleBridge.onScreencastReceiveLifeChange(true)

        val manager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = manager.createMulticastLock("udp_multicast")
        multicastLock.acquire()

        notifier = ScreencastReceiverNotifier(this)
        startForeground(Notifications.Id.SCREENCAST_RECEIVE, notifier.notificationBuilder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val httpPort: Int? = intent?.extras?.getInt(KEY_SERVER_PORT)
        val password: String? = intent?.extras?.getString(KEY_SERVER_PASSWORD)
        //必须设置端口
        if (httpPort == null || httpPort == 0) {
            return super.onStartCommand(intent, flags, startId)
        }
        //已启动则不再启动
        if (httpServer?.isAlive == true) {
            return super.onStartCommand(intent, flags, startId)
        }
        //停止旧服务
        stopHttpServer()
        //创建新服务
        httpServer = createHttpServer(httpPort, password)
        if (httpServer == null) {
            stopSelf()
            return START_STICKY
        }
        //设置投屏接收处理回调
        httpServer!!.setScreenReceiveHandler(this)
        //启动UDP组播
        startMulticast(password)
        ToastCenter.showSuccess("Screencast receiving service has started")
        return START_STICKY
    }

    override fun onDestroy() {
        multicastLock.release()
        stopHttpServer()
        UdpServer.stopMulticastEmit()
        multicastJob?.cancel()
        ioScope.cancel()
        ServiceLifecycleBridge.onScreencastReceiveLifeChange(false)
        super.onDestroy()
    }

    /**
     * 处理投屏
     */
    override fun onReceiveVideo(screencastData: ScreencastData) {
        ioScope.launch {
            val targetVideo = screencastData.videos.getOrNull(screencastData.playIndex)
                ?: return@launch

            //询问是否接收投屏
            if (considerAcceptScreencast(targetVideo).not()) {
                return@launch
            }

            //投屏源IP地址不能为空
            if (TextUtils.isEmpty(screencastData.ip)) {
                ToastCenter.showError("Failed to play, unknown screencasting source address")
                return@launch
            }

            //创建投屏资源
            val mediaSource = MediaLibraryEntity.HISTORY.copy(mediaType = MediaType.SCREEN_CAST)
                .run { StorageFactory.createStorage(this) }
                .run { this as? ScreencastStorage }
                ?.apply { setupScreencastData(screencastData) }
                ?.run { getRootFile() }
                ?.run { StorageVideoSourceFactory.create(this) }
            if (mediaSource == null) {
                ToastCenter.showError("Playing failed, unable to open playback resource")
                return@launch
            }

            notifier.showReceivedVideo(targetVideo.videoTitle)

            //正在播放器页面时，不打开新的播放器
            val topActivity = ActivityHelper.instance.getTopActivity()
            if (topActivity != null && topActivity is ScreencastHandler) {
                topActivity.playScreencast(mediaSource)
                return@launch
            }

            VideoSourceManager.getInstance().setSource(mediaSource)
            ARouter.getInstance()
                .build(RouteTable.Player.Player)
                .navigation()
        }
    }

    /**
     * 启动UDP组播
     */
    private fun startMulticast(password: String?) {
        multicastJob?.cancel()
        multicastJob = ioScope.launch {
            UdpServer.startMulticastEmit(
                httpServer!!.listeningPort,
                needPassword = password.isNullOrEmpty().not()
            )
        }
    }

    /**
     * 弹窗确认接收投屏
     */
    private suspend fun considerAcceptScreencast(videoData: ScreencastVideoData): Boolean {
        //设置了自动接收
        if (ScreencastConfig.isReceiveNeedConfirm().not()) {
            return true
        }

        //获取当前正在展示的Activity
        val topActivity = ActivityHelper.instance.getTopActivity()
        if (topActivity == null) {
            ToastCenter.showError("Received screen mirroring request, application processing failed")
            return false
        }

        //展示弹窗询问是否接收投屏
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                AlertDialog.Builder(topActivity)
                    .setTitle("Received a screen casting request, do you want to play it?")
                    .setMessage("Cast content：${videoData.videoTitle}")
                    .setNegativeButton("neglect") { dialog, _ ->
                        dialog.dismiss()
                        continuation.resume(false)
                    }
                    .setPositiveButton("play") { dialog, _ ->
                        dialog.dismiss()
                        continuation.resume(true)
                    }
                    .show()
                    .setOnDismissListener {
                        continuation.resumeWhenAlive(false)
                    }
            }
        }
    }

    private fun stopHttpServer() {
        httpServer?.setScreenReceiveHandler(null)
        httpServer?.stop()
    }

    private fun createHttpServer(port: Int, password: String?): HttpServer? {
        return try {
            val httpServer = HttpServer(password, port)
            httpServer.start(2000)
            httpServer
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}