package com.xyoye.user_component.ui.activities.cache_manager

import com.alibaba.android.arouter.facade.annotation.Route
import com.xyoye.common_component.adapter.addItem
import com.xyoye.common_component.adapter.buildAdapter
import com.xyoye.common_component.base.BaseActivity
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.extension.setData
import com.xyoye.common_component.extension.vertical
import com.xyoye.common_component.utils.formatFileSize
import com.xyoye.common_component.weight.dialog.CommonDialog
import com.xyoye.data_component.bean.CacheBean
import com.xyoye.data_component.enums.CacheType
import com.xyoye.user_component.BR
import com.xyoye.user_component.R
import com.xyoye.user_component.databinding.ActivityCacheManagerBinding
import com.xyoye.user_component.databinding.ItemCacheTypeBinding

@Route(path = RouteTable.User.CacheManager)
class CacheManagerActivity : BaseActivity<CacheManagerViewModel, ActivityCacheManagerBinding>() {

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            CacheManagerViewModel::class.java
        )

    override fun getLayoutId() = R.layout.activity_cache_manager

    override fun initView() {

        title = "Cache directory management"

        dataBinding.appCacheLl.setOnClickListener {
            considerClearSystemCache()
        }

        dataBinding.rvCache.apply {
            layoutManager = vertical()
            adapter = buildAdapter {
                addItem<CacheBean, ItemCacheTypeBinding>(R.layout.item_cache_type) {
                    initView { data, _, _ ->
                        var cacheTypeName = data.cacheType?.displayName ?: "other documents"
                        if (data.fileCount > 0) {
                            cacheTypeName += "（${data.fileCount}）"
                        }
                        var cacheDirName = ""
                        if (data.cacheType != null) {
                            cacheDirName = "Folder name：${data.cacheType!!.dirName}"
                        }

                        itemBinding.cacheTypeNameTv.text = cacheTypeName
                        itemBinding.cacheDirNameTv.text = cacheDirName
                        itemBinding.cacheSizeTv.text = formatFileSize(data.totalSize)

                        itemBinding.root.setOnClickListener {
                            considerClearCache(data.cacheType)
                        }
                    }
                }
            }
        }

        viewModel.cacheDirsLiveData.observe(this) {
            dataBinding.rvCache.setData(it)
        }

        viewModel.refreshCache()
    }

    private fun considerClearSystemCache() {
        CommonDialog.Builder(this).run {
            tips = "Clear System Cache"
            content = "System cache includes image cache and log cache. Reloading images after clearing will consume traffic. Confirm clearing?"
            addPositive { dialog ->
                dialog.dismiss()
                viewModel.clearAppCache()
            }
            addNegative { dialog -> dialog.dismiss() }
            build()
        }.show()
    }

    private fun considerClearCache(cacheType: CacheType?) {
        val title = if (cacheType == CacheType. PLAY_CACHE) {
            "Clear ${cacheType.displayName}"
        } else {
            "Clear ${cacheType?.displayName ?: "other files"} cache"
        }
        val message = cacheType?.clearTips ?: "Are you sure to clear other caches?"
        val delay = cacheType == CacheType.DANMU_CACHE || cacheType == CacheType.SUBTITLE_CACHE

        CommonDialog.Builder(this).run {
            tips = title
            content = message
            delayConfirm = delay
            addPositive {
                it.dismiss()
                viewModel.clearCacheByType(cacheType)
            }
            addNegative { it.dismiss() }
            build()
        }.show()
    }
}