package com.xyoye.data_component.enums

/**
 * Created by xyoye on 2021/3/4.
 */

enum class CacheType(val dirName: String, val displayName: String, val clearTips: String) {
    DANMU_CACHE(
        "danmu",
        "Barrage file",
        "The danmaku cache includes all matching, binding, and downloaded danmaku. After clearing, the binding will be removed and the danmaku will need to be downloaded again. Confirm clearing?"
    ),
    SUBTITLE_CACHE(
        "subtitle",
        "Subtitle File",
        "The subtitle cache includes all matching, binding, and downloaded subtitles. After clearing, the binding will be removed and subtitles will need to be downloaded again. Confirm clearing?"
    ),
    PLAY_CACHE(
        "play_cache",
        "Play cache",
        "The playback cache is mainly a temporary cache for playing online videos. Are you sure to clear it?"
    ),
    VIDEO_COVER_CACHE(
        "video_cover",
        "Video Cover",
        "Clear the video cover cache, the video cover will be re-cached after the next playback, confirm clearing?"
    ),
    SCREEN_SHOT_CACHE(
        "screen_shot",
        "Video Screenshot",
        "The screenshot cache is a video screenshot cache, are you sure to clear it?"
    ),
    TORRENT_FILE_CACHE(
        "torrent",
        "Seed File",
        "After clearing the torrent file cache, the playback resource file will re-download the torrent file, confirm clearing?"
    )
}