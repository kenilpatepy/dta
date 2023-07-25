package com.xyoye.common_component.utils.subtitle

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.paging.*
import com.xyoye.common_component.config.SubtitleConfig
import com.xyoye.common_component.network.Retrofit
import com.xyoye.common_component.network.request.RequestError
import com.xyoye.common_component.network.request.RequestErrorHandler
import com.xyoye.data_component.data.SubtitleSourceBean
import com.xyoye.data_component.data.SubtitleSubData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flatMapLatest
import retrofit2.HttpException

/**
 * Created by xyoye on 2021/3/25.
 */

class SubtitleSearchHelper(private val scope: CoroutineScope) {

    private val searchKeyLiveData = MutableLiveData<String>()

    val subtitleLiveData = searchKeyLiveData.asFlow()
        .flatMapLatest {
            getPager(it).flow.cachedIn(scope)
        }.asLiveData()

    fun search(keyword: String) {
        searchKeyLiveData.postValue(keyword)
    }

    private fun getPager(keyword: String): Pager<Int, SubtitleSourceBean> {
        return Pager(
            config = PagingConfig(15, 15),
            pagingSourceFactory = { SearchSubtitleSource(keyword) }
        )
    }

    private inner class SearchSubtitleSource(private val keyword: String) :
        PagingSource<Int, SubtitleSourceBean>() {

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SubtitleSourceBean> {
            if (params is LoadParams.Prepend) {
                return LoadResult.Page(
                    data = listOf(),
                    prevKey = null,
                    nextKey = null
                )
            }

            val page = if (params.key == null) 1 else params.key as Int

            return try {
                val shooterSecret = SubtitleConfig.getShooterSecret() ?: ""
                val subData =
                    Retrofit.extService.searchSubtitle(shooterSecret, keyword, page)
                LoadResult.Page(sub2SubtitleSearchData(subData), null, page + 1)
            } catch (e: Exception) {
                //处理509异常
                if (e is HttpException && e.code() == 509) {
                    val limitError = RequestError(509, "Request frequency is too high")
                    LoadResult.Error(limitError)
                } else {
                    LoadResult.Error(RequestErrorHandler(e).handlerError())
                }
            }
        }

        /**
         * 搜索字幕转显示数据类型
         */
        private fun sub2SubtitleSearchData(subData: SubtitleSubData?): MutableList<SubtitleSourceBean> {
            return mutableListOf<SubtitleSourceBean>().apply {
                val subList = subData?.sub?.subs
                if ((subList?.size ?: 0) > 0) {
                    for (subDetailData in subList!!) {
                        val subtitleName =
                            if (subDetailData.native_name.isNullOrEmpty())
                                subDetailData.videoname
                            else
                                subDetailData.native_name
                        add(
                            SubtitleSourceBean(
                                subDetailData.id,
                                subtitleName,
                                subDetailData.upload_time,
                                subDetailData.subtype,
                                subDetailData.lang?.desc
                            )
                        )
                    }
                }
            }
        }

        override fun getRefreshKey(state: PagingState<Int, SubtitleSourceBean>): Int? {
           return null
        }
    }
}