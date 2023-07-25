package com.xyoye.player.kernel.impl.ijk

import android.content.ContentResolver
import android.content.Context
import android.graphics.Point
import android.media.AudioManager
import android.net.Uri
import android.view.Surface
import com.xyoye.common_component.utils.SupervisorScope
import com.xyoye.data_component.bean.VideoStreamBean
import com.xyoye.data_component.enums.PixelFormat
import com.xyoye.player.info.PlayerInitializer
import com.xyoye.player.kernel.inter.AbstractVideoPlayer
import com.xyoye.player.utils.PlayerConstant
import com.xyoye.player.utils.VideoLog
import com.xyoye.subtitle.MixedSubtitle
import kotlinx.coroutines.launch
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import tv.danmaku.ijk.media.player.misc.ITrackInfo
import tv.danmaku.ijk.media.player.misc.IjkTrackInfo

/**
 * Created by xyoye on 2020/10/29.
 */

class IjkVideoPlayer(private val mContext: Context) : AbstractVideoPlayer() {
    private val TAG = IjkVideoPlayer::class.java.simpleName

    protected lateinit var mMediaPlayer: IjkMediaPlayer
    private var mBufferPercent = 0

    override fun initPlayer() {
        mMediaPlayer = IjkMediaPlayer()
        setOptions()
        initIjkEventListener()

        IjkMediaPlayer.native_setLogLevel(
            if (PlayerInitializer.isPrintLog)
                IjkMediaPlayer.IJK_LOG_DEBUG
            else
                IjkMediaPlayer.IJK_LOG_SILENT
        )
    }

    override fun setDataSource(path: String, headers: Map<String, String>?) {
        if (path.isEmpty()) {
            mPlayerEventListener.onInfo(PlayerConstant.MEDIA_INFO_URL_EMPTY, 0)
            return
        }

        val uri = Uri.parse(path)

        try {
            if (ContentResolver.SCHEME_ANDROID_RESOURCE == uri.scheme) {
                val rawProvider = RawDataSourceProvider.create(mContext, uri)
                mMediaPlayer.setDataSource(rawProvider)
            } else {
                headers?.get("User-Agent")?.let {
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user_agent", it)
                }

                mMediaPlayer.setDataSource(mContext, uri, headers)
            }
        } catch (e: Exception) {
            mPlayerEventListener.onError(e)
        }
    }

    override fun setSurface(surface: Surface) {
        mMediaPlayer.setSurface(surface)
    }

    override fun prepareAsync() {
        try {
            mMediaPlayer.prepareAsync()
        } catch (e: IllegalStateException) {
            mPlayerEventListener.onError(e)
        }
    }

    override fun start() {
        try {
            mMediaPlayer.start()
        } catch (e: IllegalStateException) {
            mPlayerEventListener.onError(e)
        }
    }

    override fun pause() {
        try {
            mMediaPlayer.pause()
        } catch (e: IllegalStateException) {
            mPlayerEventListener.onError(e)
        }
    }

    override fun stop() {
        try {
            mMediaPlayer.stop()
        } catch (e: IllegalStateException) {
            mPlayerEventListener.onError(e)
        }
    }

    override fun reset() {
        mMediaPlayer.reset()
        setOptions()
    }

    override fun release() {
        mMediaPlayer.setOnErrorListener(null)
        mMediaPlayer.setOnCompletionListener(null)
        mMediaPlayer.setOnInfoListener(null)
        mMediaPlayer.setOnBufferingUpdateListener(null)
        mMediaPlayer.setOnPreparedListener(null)
        mMediaPlayer.setOnVideoSizeChangedListener(null)

        SupervisorScope.IO.launch {
            mMediaPlayer.release()
        }
    }

    override fun seekTo(timeMs: Long) {
        try {
            mMediaPlayer.seekTo(timeMs)
        } catch (e: java.lang.IllegalStateException) {
            mPlayerEventListener.onError(e)
        }
    }

    override fun setSpeed(speed: Float) {
        mMediaPlayer.speed = speed
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        mMediaPlayer.setVolume(leftVolume, rightVolume)
    }

    override fun setLooping(isLooping: Boolean) {
        mMediaPlayer.isLooping = isLooping
    }

    override fun setOptions() {
        mMediaPlayer.apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            //硬解码
            if (PlayerInitializer.Player.isMediaCodeCEnabled) {
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)
                setOption(
                    IjkMediaPlayer.OPT_CATEGORY_PLAYER,
                    "mediacodec-handle-resolution-change",
                    1
                )
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "analyzeduration", "2000000")
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "probsize", "4096")
            } else {
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0)
                setOption(
                    IjkMediaPlayer.OPT_CATEGORY_PLAYER,
                    "mediacodec-handle-resolution-change",
                    0
                )
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0)
            }

            //h265硬解码
            if (PlayerInitializer.Player.isMediaCodeCH265Enabled) {
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", 1)
            } else {
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", 0)
            }

            if (PlayerInitializer.Player.isOpenSLESEnabled) {
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1)
            } else {
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0)
            }

            var pixelFormat = PlayerInitializer.Player.pixelFormat.value
            pixelFormat = if (pixelFormat.isEmpty()) PixelFormat.PIXEL_RGB888.value else pixelFormat
            //像素格式（设置为空无字幕输出）
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", pixelFormat)

            //跳帧处理,放CPU处理较慢时，进行跳帧处理，保证播放流程，画面和声音同步
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1)
            //设置是否开启环路过滤: 0开启，画面质量高，解码开销大，48关闭，画面质量差点，解码开销小
            setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 0)
            //设置是否开启字幕: 1开启，虽说开启字幕，但具体字幕显示还是要自己处理
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "subtitle", 1)
            //SeekTo设置优化
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1)

//            //播放重连次数
//            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "reconnect", 5)
//            //最大缓冲大小,单位kb
//            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"max-buffer-size", 10240L)
//            //最大fps
//            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-fps", 30L)
//            //设置是否开启变调
//            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"soundtouch",0);
        }

    }

    //not support
    override fun setSubtitleOffset(offsetMs: Long) {
    }

    override fun isPlaying() = mMediaPlayer.isPlaying

    override fun getCurrentPosition() = mMediaPlayer.currentPosition

    override fun getDuration() = mMediaPlayer.duration

    override fun getSpeed() = mMediaPlayer.speed

    override fun getVideoSize(): Point {
        return Point(
            mMediaPlayer.videoWidth,
            mMediaPlayer.videoHeight
        )
    }

    override fun getBufferedPercentage() = mBufferPercent

    override fun getTcpSpeed() = mMediaPlayer.tcpSpeed

    override fun getAudioStream(): List<VideoStreamBean> {
        return getStreams(true)
    }

    override fun getSubtitleStream(): List<VideoStreamBean> {
        return getStreams(false)
    }

    override fun selectStream(stream: VideoStreamBean) {
        if (stream.isExternalStream) {
            // 使用外挂流时，禁用内部流
            disableStream(stream)
            return
        }

        mMediaPlayer.selectTrack(stream.trackId)
        mMediaPlayer.seekTo(getCurrentPosition())
    }

    private fun initIjkEventListener() {
        mMediaPlayer.apply {
            //视频播放准备完成检查
            setOnPreparedListener {
                mPlayerEventListener.onPrepared()
                VideoLog.d("$TAG--listener--onPrepared--> STATE_PREPARED")
            }

            //视频播放错误监听
            setOnErrorListener { _, framework_err, impl_err ->
                val exception =
                    RuntimeException("video start error: what: $framework_err, extra: $impl_err")
                mPlayerEventListener.onError(exception)
                VideoLog.d("$TAG--listener--onError--> STATE_ERROR: what: $framework_err, extra: $impl_err")
                true
            }

            //视频播放完成监听
            setOnCompletionListener {
                mPlayerEventListener.onCompletion()
                VideoLog.d("$TAG--listener--onCompletion--> STATE_COMPLETION")
            }

            //视频缓冲更新监听
            setOnBufferingUpdateListener { _, percent -> mBufferPercent = percent }

            //视频大小改变监听
            setOnVideoSizeChangedListener { iMediaPlayer, _, _, _, _ ->
                val videoWidth = iMediaPlayer.videoWidth
                val videoHeight = iMediaPlayer.videoHeight
                if (videoWidth != 0 && videoHeight != 0) {
                    mPlayerEventListener.onVideoSizeChange(videoWidth, videoHeight)
                }
                VideoLog.d("$TAG--listener--onVideoSizeChange--> STATE_SIZE_CHANGED: width: $videoWidth, height: $videoHeight")
            }

            //设置视频信息输出监听
            setOnInfoListener { _, what, extra ->
                mPlayerEventListener.onInfo(what, extra)
                VideoLog.d("$TAG--listener--onInfo--> STATE_INFO: what: $what, extra: $extra")
                true
            }

            //视频跳转完成监听
            setOnSeekCompleteListener {

            }

            //视频字幕输出监听
            setOnTimedTextListener { _, timedText ->
                val subtitle = MixedSubtitle.fromText(timedText?.text)
                mPlayerEventListener.onSubtitleTextOutput(subtitle)
                VideoLog.d("$TAG--listener--onTextOutput--> ${subtitle.text}")
            }

            setOnNativeInvokeListener { _, _ -> true }
        }
    }

    private fun getStreams(isAudio: Boolean): List<VideoStreamBean> {
        val targetType = if (isAudio)
            IjkTrackInfo.MEDIA_TRACK_TYPE_AUDIO
        else
            IjkTrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT

        val streams = mutableListOf<VideoStreamBean>()
        val selectedStreamId = mMediaPlayer.getSelectedTrack(targetType)

        for ((index, info) in mMediaPlayer.trackInfo.withIndex()) {
            if (info.trackType != targetType) {
                continue
            }
            val trackName = "${info.title}[${info.language}, ${info.codecName}]"

            val stream = VideoStreamBean(
                trackName = trackName,
                isAudio = isAudio,
                trackId = index,
                isChecked = index == selectedStreamId
            )
            streams.add(stream)
        }
        // 添加自定义的禁用流
        streams.add(0, VideoStreamBean.disableStream(isAudio))
        return streams
    }

    /**
     * 禁用流
     */
    private fun disableStream(stream: VideoStreamBean) {
        val type = if (stream.isAudio) ITrackInfo.MEDIA_TRACK_TYPE_AUDIO else ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT
        val selectedTrackId = mMediaPlayer.getSelectedTrack(type)
        if (selectedTrackId >= 0) {
            mMediaPlayer.deselectTrack(selectedTrackId)
        }
    }
}