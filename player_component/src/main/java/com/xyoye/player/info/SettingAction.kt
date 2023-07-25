package com.xyoye.player.info

import com.xyoye.player_component.R

enum class SettingAction(val type: SettingActionType, val display: String, val icon: Int) {
    VIDEO_ASPECT(SettingActionType.VIDEO, "Proportion", R.drawable.ic_setting_video_aspect),

    VIDEO_SPEED(SettingActionType.VIDEO, "double speed", R.drawable.ic_setting_video_speed),

    AUDIO_STREAM(SettingActionType.VIDEO, "audio track", R.drawable.ic_setting_audio_stream),

    BACKGROUND_PLAY(SettingActionType.VIDEO, "background play", R.drawable.ic_setting_background_play),

    DANMU_LOAD(SettingActionType.DANMU, "load", R.drawable.ic_setting_add_source),

    DANMU_CONFIG(SettingActionType.DANMU, "configuration", R.drawable.ic_setting_danmu_block),

    DANMU_STYLE(SettingActionType.DANMU, "style", R.drawable.ic_setting_style),

    DANMU_TIME(SettingActionType.DANMU, "time", R.drawable.ic_setting_time),

    SUBTITLE_LOAD(SettingActionType.SUBTITLE, "load", R.drawable.ic_setting_add_source),

    SUBTITLE_STREAM(SettingActionType.SUBTITLE, "subtitle track", R.drawable.ic_setting_subtitle_stream),

    SUBTITLE_STYLE(SettingActionType.SUBTITLE, "style", R.drawable.ic_setting_style),

    SUBTITLE_TIME(SettingActionType.SUBTITLE, "time", R.drawable.ic_setting_time),

    SCREEN_ORIENTATION(SettingActionType.OTHER, "screen flip", R.drawable.ic_setting_rotate),

    NEXT_EPISODE(SettingActionType.OTHER, "screen flip", R.drawable.ic_setting_order_play),

    SCREEN_SHOT(SettingActionType.OTHER, "screenshot", R.mipmap.ic_video_screenshot),
}