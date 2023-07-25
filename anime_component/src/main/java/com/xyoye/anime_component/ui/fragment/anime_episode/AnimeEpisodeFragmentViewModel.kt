package com.xyoye.anime_component.ui.fragment.anime_episode

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.data_component.data.BangumiData
import com.xyoye.data_component.data.EpisodeData

class AnimeEpisodeFragmentViewModel : BaseViewModel() {
    val animeTitleField = ObservableField<String>()
    val animeSearchWordField = ObservableField<String>()
    val episodeCountField = ObservableField<String>()

    val episodeLiveData = MutableLiveData<MutableList<EpisodeData>>()
    val episodeSortLiveData = MutableLiveData<Any>()

    private lateinit var bangumiData: BangumiData

    fun setBangumiData(bangumiData: BangumiData) {
        this.bangumiData = bangumiData
        animeTitleField.set(bangumiData.animeTitle)
        animeSearchWordField.set(bangumiData.searchKeyword)

        episodeCountField.set("common${bangumiData.episodes.size}set")

        episodeLiveData.postValue(bangumiData.episodes)
    }

    fun changeSort() {
        episodeSortLiveData.postValue(Any())
    }
}