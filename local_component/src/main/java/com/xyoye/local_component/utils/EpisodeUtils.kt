package com.xyoye.local_component.utils

/**
 * Created by xyoye on 2020/11/26.
 */

private val animeType = mapOf(
    Pair("tvseries", "TV Animation"),
    Pair("movie", "movie version"),
    Pair("ova", "OVA"),
    Pair("jpdrama", "Japanese Drama"),
    Pair("jpmovie", "Japanese Movie"),
    Pair("web", "Webcast"),
    Pair("tvspecial", "TV special"),
    Pair("unknown", "unknown category"),
    Pair("musicvideo", "MV"),
    Pair("other", "other")
)

fun getAnimeType(type: String?): String {
    if (type == null)
        return "other"
    return animeType[type] ?: "Other"
}