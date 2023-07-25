package com.xyoye.local_component.ui.dialog

import android.app.Activity
import androidx.core.view.isVisible
import com.xyoye.common_component.utils.formatFileSize
import com.xyoye.common_component.utils.getFileExtension
import com.xyoye.common_component.utils.getFileNameNoExtension
import com.xyoye.common_component.utils.seven_zip.SevenZipUtils
import com.xyoye.common_component.weight.dialog.BaseBottomDialog
import com.xyoye.data_component.data.SubDetailData
import com.xyoye.local_component.R
import com.xyoye.local_component.databinding.DialogSubtitleDetailBinding

/**
 * Created by xyoye on 2020/12/9.
 */

class SubtitleDetailDialog(
    activity: Activity,
    private val subDetailData: SubDetailData,
    private val downloadOne: () -> Unit,
    private val downloadZip: (fileName: String, url: String) -> Unit
) : BaseBottomDialog<DialogSubtitleDetailBinding>(activity) {

    private var extension: String? = null

    override fun getChildLayoutId() = R.layout.dialog_subtitle_detail

    override fun initView(binding: DialogSubtitleDetailBinding) {
        setTitle("download subtitles")

        val fileName = subDetailData.filename ?: ""
        val fileNameText = getFileNameNoExtension(fileName)
        binding.fileNameEt.setText(fileNameText)
        binding.fileNameEt.setSelection(fileNameText.length)

        if (!subDetailData.url.isNullOrEmpty()) {
            val fileExtension = getFileExtension(fileName)
            if (SevenZipUtils.getArchiveFormat(fileExtension) != null) {
                extension = ".$fileExtension"
                binding.fileExtensionTv.text = extension
            }
        }

        if (extension == null) {
            setPositiveVisible(false)
            binding.fileNameTips.isVisible = false
            binding.fileNameEt.isVisible = false
            binding.fileExtensionTv.isVisible = false
        }

        val fileSizeText = "File size: ${formatFileSize(subDetailData.size ?: 0)}"
        binding.fileSizeTv.text = fileSizeText
        val fileCountText = "Number of files: ${(subDetailData.filelist?.size ?: 0)}"
        binding.fileCountTv.text = fileCountText
        val subtitleLanguageText = "Subtitle language: ${subDetailData.lang?.desc}"
        binding.subtitleLanguageTv.text = subtitleLanguageText

        var uploadTime = subDetailData.upload_time ?: ""
        if (uploadTime.contains(" ".toRegex())) {
            uploadTime = uploadTime.split(" ".toRegex())[0]
        }
        val uploadTimeText = "Upload Time: $uploadTime"
        binding.subtitleTimeTv.text = uploadTimeText

        setNegativeListener { dismiss() }
        setPositiveText("Download compressed package")

        if (subDetailData. filelist != null && (subDetailData. filelist?. size ?: 0) > 0) {
            addNeutralButton("Download a single subtitle") {
                dismiss()
                downloadOne. invoke()
            }
        }

        setPositiveListener {
            dismiss()
            val zipFileName = binding.fileNameEt.text.toString() + extension
            downloadZip.invoke(zipFileName, subDetailData.url!!)
        }
    }
}