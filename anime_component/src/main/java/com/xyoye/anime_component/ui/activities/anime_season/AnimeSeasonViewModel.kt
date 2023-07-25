package com.xyoye.anime_component.ui.activities.anime_season

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.config.UserConfig
import com.xyoye.common_component.network.Retrofit
import com.xyoye.common_component.network.request.httpRequest
import com.xyoye.common_component.utils.stringCompare
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.data.AnimeData
import com.xyoye.data_component.data.BangumiAnimeData
import com.xyoye.data_component.data.CommonTypeData
import com.xyoye.data_component.enums.AnimeSortType
import java.util.*

class AnimeSeasonViewModel : BaseViewModel() {
    private val seasonData = mutableListOf(
        CommonTypeData("October", "10"),
        CommonTypeData("July", "7"),
        CommonTypeData("April", "4"),
        CommonTypeData("January", "1")
    )

    private val sortTypeData = mutableListOf(
        CommonTypeData("Name", AnimeSortType.NAME.value),
        CommonTypeData("Rating", AnimeSortType.RATING.value),
        CommonTypeData("follow", AnimeSortType.FOLLOW.value)
    )

    private var sortType = AnimeSortType.NONE
    private lateinit var seasonAnimeData: BangumiAnimeData
    private var years: MutableList<CommonTypeData>

    init {
        val nowYear = Calendar.getInstance().get(Calendar.YEAR)
        years = mutableListOf(
            CommonTypeData(nowYear.toString() + "Year", nowYear.toString()),
            CommonTypeData((nowYear - 1).toString() + "Year", (nowYear - 1).toString()),
            CommonTypeData((nowYear - 2).toString() + "Year", (nowYear - 2).toString()),
            CommonTypeData("More", "-1").also { it.isEnable = false }
        )
    }

    val yearsLiveData = MutableLiveData<MutableList<CommonTypeData>>()
    val seasonLiveData = MutableLiveData<MutableList<CommonTypeData>>()
    val animeLiveData = MutableLiveData<MutableList<AnimeData>>()
    val sortLiveData = MutableLiveData<MutableList<CommonTypeData>>()

    fun getYearsData() {
        yearsLiveData.postValue(years)
        checkYear(years[0].typeId)
    }

    fun getSortData() {
        sortLiveData.postValue(sortTypeData)
    }

    fun checkYear(year: String) {
        var selectLastItem = true
        //是否在默认的Year份中
        for (yearData in years) {
            if (year == yearData.typeId) {
                yearData.isChecked = true
                selectLastItem = false
                getSeasonData(year.toInt())
            } else {
                yearData.isChecked = false
            }
        }

        //选中最后一个item
        if (selectLastItem) {
            years.last().isChecked = true
            years.last().typeName = year + "Year"
            years.last().typeId = year
            getSeasonData(year.toInt())
        } else {
            years.last().isChecked = false
            years.last().typeName = "More"
            years.last().typeId = "-1"
        }

        yearsLiveData.postValue(years)
    }

    fun checkSeason(seasonId: String) {
        var yearId = years[0].typeId
        for (year in years) {
            if (year.isChecked) {
                yearId = year.typeId
                break
            }
        }

        for (season in seasonData) {
            if (season.typeId == seasonId) {
                season.isChecked = true
                getSeasonAnime(yearId, seasonId)
            } else {
                season.isChecked = false
            }
        }
        seasonLiveData.postValue(seasonData)
    }

    fun checkSort(position: Int) {
        if (!this::seasonAnimeData.isInitialized) {
            return
        }

        if (!UserConfig.isUserLoggedIn()
            && AnimeSortType.formValue(sortTypeData[position].typeId) == AnimeSortType.FOLLOW
        ) {
            ToastCenter.showWarning("Please log in to do this")
            return
        }

        var checkedIndex = -1
        for (index in sortTypeData.indices) {
            if (sortTypeData[index].isChecked) {
                checkedIndex = index
                break
            }
        }

        when (checkedIndex) {
            -1 -> {
                sortTypeData[position].isChecked = true
                sortType = AnimeSortType.formValue(sortTypeData[position].typeId)
            }
            position -> {
                sortTypeData[position].isChecked = false
                sortType = AnimeSortType.NONE
            }
            else -> {
                sortTypeData[position].isChecked = true
                sortType = AnimeSortType.formValue(sortTypeData[position].typeId)

                sortTypeData[checkedIndex].isChecked = false
            }
        }
        sortLiveData.postValue(sortTypeData)
        showSearchResult()
    }

    private fun getSeasonData(year: Int) {
        var isSeasonChecked = false
        //设置所有月份都是可用的
        for (season in seasonData) {
            season.isEnable = true
            //有已选中月份，直接使用
            if (season.isChecked) {
                isSeasonChecked = true
                getSeasonAnime(year.toString(), season.typeId)
            }
        }

        if (isSeasonChecked) {
            seasonLiveData.postValue(seasonData)
            return
        }

        val nowYear = Calendar.getInstance().get(Calendar.YEAR)
        //如果是当Year数据，某些月份可能需要不可点击
        if (year == nowYear) {
            //当前月份
            val nowMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            for (season in seasonData) {
                //可选月份是否大于当前月份
                if (season.typeId.toInt() > nowMonth) {
                    season.isEnable = false
                } else {
                    season.isChecked = true
                    getSeasonAnime(year.toString(), season.typeId)
                    break
                }
            }
            seasonLiveData.postValue(seasonData)
        } else {
            seasonData[0].isChecked = true
            seasonLiveData.postValue(seasonData)
            getSeasonAnime(year.toString(), seasonData[0].typeId)
        }
    }

    private fun getSeasonAnime(year: String, month: String) {
        httpRequest<BangumiAnimeData>(viewModelScope) {
            api {
                Retrofit.service.getSeasonAnime(year, month)
            }

            onSuccess {
                seasonAnimeData = it
                showSearchResult()
            }

            onError {
                showNetworkError(it)
            }
        }
    }

    private fun showSearchResult() {
        if (sortType == AnimeSortType.NONE) {
            animeLiveData.postValue(seasonAnimeData.bangumiList)
            return
        }
        if (!UserConfig.isUserLoggedIn() && sortType == AnimeSortType.FOLLOW) {
            animeLiveData.postValue(seasonAnimeData.bangumiList)
            return
        }
        if (this::seasonAnimeData.isInitialized) {
            val sortedList = mutableListOf<AnimeData>().also {
                it.addAll(seasonAnimeData.bangumiList)
            }
            Collections.sort(sortedList, kotlin.Comparator { o1, o2 ->
                return@Comparator when (sortType) {
                    AnimeSortType.FOLLOW -> o1.isFavorited.compareTo(o2.isFavorited)

                    AnimeSortType.RATING -> o2.rating.compareTo(o1.rating)

                    AnimeSortType.NAME -> stringCompare(o1.animeTitle, o2.animeTitle)

                    else -> 0
                }
            })
            animeLiveData.postValue(sortedList)
        }
    }
}