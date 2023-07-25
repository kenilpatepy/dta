package com.xyoye.user_component.ui.activities.commonly_folder

import android.os.Environment
import com.alibaba.android.arouter.facade.annotation.Route
import com.xyoye.common_component.base.BaseActivity
import com.xyoye.common_component.config.AppConfig
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.weight.dialog.CommonDialog
import com.xyoye.common_component.weight.dialog.FileManagerDialog
import com.xyoye.data_component.enums.FileManagerAction

import com.xyoye.user_component.BR
import com.xyoye.user_component.R
import com.xyoye.user_component.databinding.ActivityCommonlyFolderBinding

@Route(path = RouteTable.User.CommonManager)
class CommonlyFolderActivity :
    BaseActivity<CommonlyFolderViewModel, ActivityCommonlyFolderBinding>() {

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            CommonlyFolderViewModel::class.java
        )

    override fun getLayoutId() = R.layout.activity_commonly_folder

    override fun initView() {

        var commonFolder1Path = AppConfig.getCommonlyFolder1()
        commonFolder1Path =
            if (commonFolder1Path.isNullOrEmpty()) "Path: Not set" else "Path: $commonFolder1Path"
        dataBinding.commonlyFolder1Tv.text = commonFolder1Path

        var commonFolder2Path = AppConfig.getCommonlyFolder2()
        commonFolder2Path =
            if (commonFolder2Path.isNullOrEmpty()) "Path: Not set" else "Path: $commonFolder2Path"
        dataBinding.commonlyFolder2Tv.text = commonFolder2Path

        dataBinding.lastOpenFolderSw.isChecked = AppConfig.isLastOpenFolderEnable()

        initListener()
    }

    private fun initListener() {

        val defaultPath = Environment.getExternalStorageDirectory().absolutePath

        dataBinding.commonlyFolder1Ll.setOnClickListener {
            FileManagerDialog(
                this,
                FileManagerAction.ACTION_SELECT_DIRECTORY,
                defaultPath
            ) {
                AppConfig.putCommonlyFolder1(it)
                val path = "path：$it"
                dataBinding.commonlyFolder1Tv.text = path
            }.show()
        }

        dataBinding.commonlyFolder2Ll.setOnClickListener {
            FileManagerDialog(
                this,
                FileManagerAction.ACTION_SELECT_DIRECTORY,
                defaultPath
            ) {
                AppConfig.putCommonlyFolder2(it)
                val path = "path：$it"
                dataBinding.commonlyFolder2Tv.text = path
            }.show()
        }

        dataBinding.commonlyFolder1Ll.setOnLongClickListener {
            CommonDialog.Builder(this).apply {
                content = "Confirm deletion of common folder 1？"
                addPositive {
                    AppConfig.putCommonlyFolder1("")
                    dataBinding.commonlyFolder1Tv.text = "path: not set"
                    it.dismiss()
                }
                addNegative { it.dismiss() }
            }.build().show()
            return@setOnLongClickListener true
        }

        dataBinding.commonlyFolder2Ll.setOnLongClickListener {
            CommonDialog.Builder(this).apply {
                content = "Confirm delete common folder 2？"
                addPositive {
                    AppConfig.putCommonlyFolder2("")
                    dataBinding.commonlyFolder2Tv.text = "path: not set"
                    it.dismiss()
                }
                addNegative { it.dismiss() }
            }.build().show()
            return@setOnLongClickListener true
        }


        dataBinding.lastOpenFolderSw.setOnCheckedChangeListener { _, isChecked ->
            AppConfig.putLastOpenFolderEnable(isChecked)
        }
    }
}