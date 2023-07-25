package com.xyoye.anime_component.ui.fragment.anime_intro

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.data_component.data.BangumiData
import com.xyoye.data_component.data.TagData
import java.text.DecimalFormat

class AnimeIntroFragmentViewModel : BaseViewModel() {
    val animeStatusField = ObservableField<String>()
    val animeTypeField = ObservableField<String>()
    val animeRatingField = ObservableField<String>()

    val animeIntroFiled = ObservableField<String>()
    val hideIntroFiled = ObservableField(true)

    val animeInfoFiled = ObservableField<String>()
    val hideInfoFiled = ObservableField(true)
    val expandInfoFiled = ObservableBoolean(false)
    val expandStatusFiled = ObservableField("expand")

    val tagLiveData = MutableLiveData<MutableList<TagData>>()

    fun setBangumiData(bangumiData: BangumiData) {
        bangumiData.apply {
            animeStatusField.set(getAnimeStatus(isOnAir, airDay))
            animeTypeField.set(typeDescription)
            animeRatingField.set(getRating(rating))

            animeIntroFiled.set(summary)
            hideIntroFiled.set(summary.isNullOrEmpty())

            val info = getAnimeInfo(metadata)
            hideInfoFiled.set(info.isNullOrEmpty())
            animeInfoFiled.set(info)

            tagLiveData.postValue(tags)
        }
    }

    fun toggleInfoExpend() {
        val expand = expandInfoFiled.get()
        expandStatusFiled.set(if (expand) "expand" else "collapse")
        expandInfoFiled.set(!expand)
    }

    private fun getAnimeStatus(isOnAir: Boolean, airDay: Int): String {
        if (!isOnAir)
            return "Completed"
        return when (airDay) {
            0 -> "Update every Sunday"
            1 -> "Update every Monday"
            2 -> "Update every Tuesday"
            3 -> "Update every Wednesday"
            4 -> "Update every Thursday"
            5 -> "Update every Friday"
            6 -> "Update every Saturday"
            else -> "Update date unknown"
        }
    }

    private fun getRating(rating: Double) =
        if (rating <= 0)
            "no yet"
        else
            DecimalFormat("0.0").format(rating)

    private fun getAnimeInfo(metadata: MutableList<String>?): String? {
        if (metadata == null)
            return null
        if (metadata.size == 0)
            return null

        val infoBuilder = StringBuilder()
        metadata.forEach {
            infoBuilder.append("$it\n")
        }
        return infoBuilder.toString()
    }
}