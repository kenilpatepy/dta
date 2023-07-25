package com.xyoye.local_component.ui.fragment.bind_subtitle

import android.text.TextUtils
import androidx.core.view.isVisible
import androidx.paging.LoadState
import com.xyoye.common_component.adapter.paging.BasePagingAdapter
import com.xyoye.common_component.adapter.paging.PagingFooterAdapter
import com.xyoye.common_component.adapter.paging.addItem
import com.xyoye.common_component.adapter.paging.buildPagingAdapter
import com.xyoye.common_component.base.BaseFragment
import com.xyoye.common_component.config.SubtitleConfig
import com.xyoye.common_component.extension.isInvalid
import com.xyoye.common_component.extension.toFile
import com.xyoye.common_component.extension.vertical
import com.xyoye.common_component.storage.file.StorageFile
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.common_component.weight.dialog.FileManagerDialog
import com.xyoye.data_component.data.SubtitleSourceBean
import com.xyoye.data_component.enums.FileManagerAction
import com.xyoye.local_component.BR
import com.xyoye.local_component.R
import com.xyoye.local_component.databinding.FragmentBindSubtitleSourceBinding
import com.xyoye.local_component.databinding.ItemSubtitleSearchSourceBinding
import com.xyoye.local_component.listener.ExtraSourceListener
import com.xyoye.local_component.ui.activities.bind_source.BindExtraSourceActivity
import com.xyoye.local_component.ui.dialog.ShooterSecretDialog
import com.xyoye.local_component.ui.dialog.SubtitleDetailDialog
import com.xyoye.local_component.ui.dialog.SubtitleFileListDialog


/**
 * Created by xyoye on 2022/1/25
 */
class BindSubtitleSourceFragment :
    BaseFragment<BindSubtitleSourceFragmentViewModel, FragmentBindSubtitleSourceBinding>(),
    ExtraSourceListener {

    private lateinit var subtitleAdapter: BasePagingAdapter<SubtitleSourceBean>

    companion object {
        fun newInstance() = BindSubtitleSourceFragment()
    }

    override fun initViewModel() = ViewModelInit(
        BR.viewModel,
        BindSubtitleSourceFragmentViewModel::class.java
    )

    override fun getLayoutId() = R.layout.fragment_bind_subtitle_source

    override fun initView() {
        viewModel.storageFile = (activity as BindExtraSourceActivity).storageFile

        initActionView()

        initRv()

        initListener()

        viewModel.matchSubtitle()
    }

    private fun initActionView() {
        val boundSubtitle = viewModel.storageFile.playHistory?.subtitlePath?.isNotEmpty() == true
        dataBinding.tvUnbindSubtitle.isEnabled = boundSubtitle

        updateKeyActionView()
    }

    private fun initRv() {
        subtitleAdapter = buildPagingAdapter {

            addItem<SubtitleSourceBean, ItemSubtitleSearchSourceBinding>(R.layout.item_subtitle_search_source) {
                initView { data, position, _ ->
                    itemBinding.apply {
                        val describe = if (data. isMatch) {
                            "Source: ${data.source}"
                        } else {
                            "Language: ${data.language}"
                        }
                        val positionText = (position + 1).toString()

                        positionTv.text = positionText
                        subtitleNameTv.text = data.name
                        subtitleDescribeTv.text = describe
                        itemLayout.setOnClickListener {
                            if (data.isMatch) {
                                viewModel.downloadSearchSubtitle(data.name, data.matchUrl)
                            } else {
                                viewModel.detailSearchSubtitle(data)
                            }
                        }
                    }
                }
            }
        }

        val contactAdapter = subtitleAdapter.withLoadStateFooter(
            PagingFooterAdapter { subtitleAdapter.retry() }
        )

        dataBinding.subtitleRv.apply {
            layoutManager = vertical()

            adapter = contactAdapter
        }
    }

    private fun initListener() {
        subtitleAdapter.addLoadStateListener {
            val emptyData = it.refresh is LoadState.NotLoading && subtitleAdapter.itemCount == 0
            dataBinding.emptyCl.isVisible = emptyData
            dataBinding.subtitleRv.isVisible = emptyData.not()
        }

        viewModel.subtitleSearchLiveData.observe(this) {
            subtitleAdapter.submitPagingData(lifecycle, it)
        }
        viewModel.subtitleMatchLiveData.observe(this) {
            subtitleAdapter.submitPagingData(lifecycle, it)
        }

        viewModel.searchSubtitleDetailLiveData.observe(this) {
            SubtitleDetailDialog(
                requireActivity(),
                it,
                downloadOne = {
                    SubtitleFileListDialog(
                        requireActivity(),
                        it.filelist!!
                    ) { fileName, url ->
                        viewModel.downloadSearchSubtitle(fileName, url)
                    }.show()
                },
                downloadZip = { fileName, url ->
                    viewModel.downloadSearchSubtitle(fileName, url, true)
                }
            ).show()
        }

        viewModel.sourceRefreshLiveData.observe(this) {
            (mAttachActivity as BindExtraSourceActivity).onSourceChanged()
        }

        viewModel.unzipResultLiveData.observe(this) { dirPath ->
            FileManagerDialog(
                requireActivity(),
                FileManagerAction.ACTION_SELECT_SUBTITLE,
                dirPath
            ) {
                viewModel.databaseSubtitle(it)
                ToastCenter.showSuccess("Binding subtitles successfully！")
            }.show()
        }

        dataBinding.tvUnbindSubtitle.setOnClickListener {
            viewModel.unbindSubtitle()
        }
        dataBinding.tvSelectLocalSubtitle.setOnClickListener {
            selectLocalSubtitleFile()
        }
        dataBinding.tvSettingSubtitleKey.setOnClickListener {
            settingSubtitleKey()
        }
    }

    override fun search(searchText: String) {
        val shooterSecret = SubtitleConfig.getShooterSecret()
        if (shooterSecret.isNullOrEmpty()) {
            settingSubtitleKey()
        } else {
            viewModel.searchSubtitle(searchText)
        }
    }

    override fun onStorageFileChanged(storageFile: StorageFile) {
        viewModel.storageFile = storageFile
        initActionView()
    }

    private fun settingSubtitleKey() {
        val dialog = ShooterSecretDialog(requireActivity())
        dialog.setOnDismissListener { updateKeyActionView() }
        dialog.show()
    }

    private fun updateKeyActionView() {
        dataBinding.tvSettingSubtitleKey.isSelected =
            TextUtils.isEmpty(SubtitleConfig.getShooterSecret())
    }

    private fun selectLocalSubtitleFile() {
        FileManagerDialog(
            requireActivity(),
            FileManagerAction.ACTION_SELECT_SUBTITLE
        ) {
            if (it.toFile().isInvalid()) {
                ToastCenter.showError("Failed to bind the subtitle, the subtitle does not exist or the content is empty")
                return@FileManagerDialog
            }
            viewModel.databaseSubtitle(it)
        }.show()
    }
}