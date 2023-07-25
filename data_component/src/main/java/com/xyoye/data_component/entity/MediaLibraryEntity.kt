package com.xyoye.data_component.entity

import android.os.Parcelable
import android.provider.MediaStore
import androidx.room.*
import com.xyoye.data_component.enums.MediaType
import com.xyoye.data_component.helper.BooleanConverter
import com.xyoye.data_component.helper.MediaTypeConverter
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Created by xyoye on 2021/1/18.
 */

@Parcelize
@Entity(tableName = "media_library", indices = [Index(value = arrayOf("url", "media_type"), unique = true)])
@TypeConverters(BooleanConverter::class, MediaTypeConverter::class)
data class MediaLibraryEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    @ColumnInfo(name = "display_name")
    var displayName: String,

    @ColumnInfo(name = "url")
    var url: String,

    @ColumnInfo(name = "media_type")
    var mediaType: MediaType,

    @ColumnInfo(name = "account")
    var account: String? = null,

    @ColumnInfo(name = "password")
    var password: String? = null,

    @ColumnInfo(name = "is_anonymous")
    var isAnonymous: Boolean = false,

    @ColumnInfo(name = "port")
    var port: Int = 0,

    @ColumnInfo(name = "describe")
    var describe: String? = null,

    @ColumnInfo(name = "ftp_mode")
    var isActiveFTP: Boolean = false,

    @ColumnInfo(name = "ftp_address")
    var ftpAddress: String = "",

    @ColumnInfo(name = "ftp_encoding")
    var ftpEncoding: String = "UTF-8",

    @ColumnInfo(name = "smb_v2")
    var smbV2: Boolean = true,

    @ColumnInfo(name = "smb_share_path")
    var smbSharePath: String? = null,

    @ColumnInfo(name = "remote_secret")
    var remoteSecret: String? = null,

    @ColumnInfo(name = "web_dav_strict")
    var webDavStrict: Boolean = true,

    @ColumnInfo(name = "screencast_address")
    var screencastAddress: String = "",

    @ColumnInfo(name = "remote_anime_grouping")
    var remoteAnimeGrouping: Boolean = false
) : Parcelable {

    companion object {
        val LOCAL = MediaLibraryEntity(
            id = 1,
            displayName = "本地媒体库",
            url = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString(),
            mediaType = MediaType.LOCAL_STORAGE
        )
        val STREAM = MediaLibraryEntity(
            id = 2,
            displayName = "串流播放",
            url = "url://dandanplay_steam_link",
            mediaType = MediaType.STREAM_LINK,
            describe = "https://"
        )
        val TORRENT = MediaLibraryEntity(
            id = 3,
            displayName = "磁链播放",
            url = "url://dandanplay_magnet_link",
            mediaType = MediaType.MAGNET_LINK,
            describe = "magnet:?xt=urn:btih:"
        )
        val HISTORY = MediaLibraryEntity(
            id = 4,
            displayName = "播放历史",
            url = "",
            mediaType = MediaType.OTHER_STORAGE
        )
    }

    @Ignore
    @IgnoredOnParcel
    var running: Boolean = false
}