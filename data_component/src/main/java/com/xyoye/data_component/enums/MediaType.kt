package com.xyoye.data_component.enums

import com.xyoye.data_component.R
import com.xyoye.data_component.bean.SheetActionBean

/**
 * Created by xyoye on 2021/1/18.
 */

enum class MediaType(
    val value: String,
    val storageName: String,
    val cover: Int = 0
) {
    LOCAL_STORAGE(
        "local_storage",
        "Local Media Library",
        cover = R.drawable.ic_local_storage
    ),

    OTHER_STORAGE(
        "other_storage",
        "External Media Library",
        cover = R.drawable.ic_play_history
    ),

    STREAM_LINK(
        "stream_link",
        "Streaming Video",
        cover = R.drawable.ic_stream_link
    ),

    MAGNET_LINK(
        "magnet_link",
        "Magnet video",
        cover = R.drawable.ic_magnet_link
    ),

    FTP_SERVER(
        "ftp_server",
        "FTP Media Library",
        cover = R.drawable.ic_ftp_storage
    ),

    WEBDAV_SERVER(
        "webdav_server",
        "WebDav Media Library",
        cover = R.drawable.ic_webdav_storage
    ),

    SMB_SERVER(
        "smb_server",
        "SMB Media Library",
        cover = R.drawable.ic_smb_storage
    ),

    REMOTE_STORAGE(
        "remote_storage",
        "Remote Media Library",
        cover = R.drawable.ic_remote_storage
    ),

    SCREEN_CAST(
        "screen_cast",
        "Remote projection",
        cover = R.drawable.ic_screencast
    ),

    EXTERNAL_STORAGE(
        "external_storage",
        "Device Repository",
        cover = R.drawable.ic_external_storage
    );

    companion object {
        fun fromValue(value: String): MediaType {
            return when (value) {
                "local_storage" -> LOCAL_STORAGE
                "stream_link" -> STREAM_LINK
                "magnet_link" -> MAGNET_LINK
                "ftp_server" -> FTP_SERVER
                "webdav_server" -> WEBDAV_SERVER
                "smb_server" -> SMB_SERVER
                "remote_storage" -> REMOTE_STORAGE
                "screen_cast" -> SCREEN_CAST
                "external_storage" -> EXTERNAL_STORAGE
                else -> OTHER_STORAGE
            }
        }
    }

    fun toAction() = SheetActionBean(this, storageName, cover)
}