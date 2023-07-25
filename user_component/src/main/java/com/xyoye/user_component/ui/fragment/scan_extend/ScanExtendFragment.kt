package com.xyoye.user_component.ui.fragment.scan_extend

import com.xyoye.common_component.adapter.addItem
import com.xyoye.common_component.adapter.buildAdapter
import com.xyoye.common_component.base.BaseFragment
import com.xyoye.common_component.extension.setData
import com.xyoye.common_component.extension.vertical
import com.xyoye.common_component.utils.FastClickFilter
import com.xyoye.common_component.utils.getFolderName
import com.xyoye.common_component.weight.dialog.CommonDialog
import com.xyoye.common_component.weight.dialog.FileManagerDialog
import com.xyoye.data_component.entity.ExtendFolderEntity
import com.xyoye.data_component.enums.FileManagerAction
import com.xyoye.user_component.BR
import com.xyoye.user_component.R
import com.xyoye.user_component.databinding.*

class ScanExtendFragment : BaseFragment<ScanExtendFragmentViewModel, FragmentScanExtendBinding>() {

    companion object {
        fun newInstance() = ScanExtendFragment()
    }

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            ScanExtendFragmentViewModel::class.java
        )

    override fun getLayoutId() = R.layout.fragment_scan_extend

    override fun initView() {

        dataBinding.extendFolderRv.apply {
            layoutManager = vertical()

            adapter = buildAdapter {
                addItem<Int, ItemExtendFolderAddBinding>(R.layout.item_extend_folder_add) {
                    initView { _, _, _ ->
                        itemBinding.itemLayout.setOnClickListener {
                            if (FastClickFilter.isNeedFilter())
                                return@setOnClickListener
                            showExtendFolderDialog()
                        }
                    }
                }

                addItem<ExtendFolderEntity, ItemExtendFolderBinding>(R.layout.item_extend_folder) {
                    initView { data, _, _ ->
                        itemBinding.apply {
                            val fileCountText = "${data.childCount}video"

                            folderTv.text = getFolderName(data.folderPath)
                            fileCountTv.text = fileCountText

                            removeFolderIv.setOnClickListener {
                                if (FastClickFilter.isNeedFilter())
                                    return@setOnClickListener
                                showConfirmRemoveDialog(data)
                            }
                        }
                    }
                }

            }
        }

        viewModel.extendFolderLiveData.observe(this) {
            dataBinding.extendFolderRv.setData(it)
        }

        viewModel.getExtendFolder()
    }

    private fun showExtendFolderDialog() {
        FileManagerDialog(requireActivity(), FileManagerAction.ACTION_SELECT_DIRECTORY) {
            viewModel.addExtendFolder(it)
        }.show()
    }

    private fun showConfirmRemoveDialog(entity: ExtendFolderEntity) {
        CommonDialog.Builder(requireActivity()).apply {
            content = "Confirm folder removal？"
            addPositive {
                it.dismiss()
                viewModel.removeExtendFolder(entity)
            }
            addNegative { it.dismiss() }
        }.build().show()
    }
}