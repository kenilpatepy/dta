package com.xyoye.local_component.ui.activities.bilibili_danmu

import android.content.Intent
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.xyoye.common_component.base.BaseActivity
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.weight.BottomActionDialog
import com.xyoye.common_component.weight.dialog.CommonEditDialog
import com.xyoye.data_component.bean.EditBean
import com.xyoye.data_component.bean.SheetActionBean
import com.xyoye.local_component.BR
import com.xyoye.local_component.R
import com.xyoye.local_component.databinding.ActivityBilibiliDanmuBinding

@Route(path = RouteTable.Local.BiliBiliDanmu)
class BilibiliDanmuActivity : BaseActivity<BilibiliDanmuViewModel, ActivityBilibiliDanmuBinding>() {
    companion object {
        private const val REQUEST_CODE_SELECT_URL = 1001
    }

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            BilibiliDanmuViewModel::class.java
        )

    override fun getLayoutId() = R.layout.activity_bilibili_danmu

    override fun initView() {
        title = "BiliBili barrage download"

        val firstMessage = "Please select a download mode\n"
        dataBinding.downloadMessageEt.isFocusable = false
        dataBinding.downloadMessageEt.isFocusableInTouchMode = false
        dataBinding.downloadMessageEt.setText(firstMessage)

        dataBinding.addDownloadBt.setOnClickListener {
            showActionDialog()
        }

        viewModel.downloadMessageLiveData.observe(this) {
            dataBinding.downloadMessageEt.append("$it\n")
        }
        viewModel.clearMessageLiveData.observe(this) {
            dataBinding.downloadMessageEt.setText(firstMessage)
        }

        showActionDialog()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SELECT_URL) {
            if (resultCode == RESULT_OK) {
                data?.getStringExtra("url_data")?.let {
                    viewModel.downloadByUrl(it)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showActionDialog() {
        BottomActionDialog(
            this,
            DownloadType.values().map { it.toAction() },
            "Download barrage"
        ) {
            if (it.actionId == DownloadType.SELECT) {
                ARouter.getInstance().build(RouteTable.User.WebView)
                    .withString("titleText", "select link")
                    .withString("url", "http://www.bilibili.com")
                    .withBoolean("isSelectMode", true)
                    .navigation(this, REQUEST_CODE_SELECT_URL)
            } else {
                showInputDialog(it.actionId as DownloadType)
            }
            return@BottomActionDialog true
        }.show()
    }

    private fun showInputDialog(type: DownloadType) {
        val editBean = when (type) {
            DownloadType.URL -> EditBean("Enter link to download", "Link cannot be empty", "Fan drama link")
            DownloadType.AV_CODE -> EditBean("Enter AV number to download", "AV number cannot be empty", "Pure digital AV number")
            DownloadType.BV_CODE -> EditBean("Enter BV number to download", "BV number cannot be empty", "Complete BV number")
            else -> return
        }

        CommonEditDialog(
            this, editBean, type == DownloadType.AV_CODE
        ) {
            when (type) {
                DownloadType.URL -> viewModel.downloadByUrl(it)
                DownloadType.AV_CODE -> viewModel.downloadByCode(it, true)
                DownloadType.BV_CODE -> viewModel.downloadByCode(it, false)
                else -> {}
            }
        }.show()
    }

    private enum class DownloadType(val title: String, val icon: Int) {
        SELECT("Select link to download", R.drawable.ic_select_link),
        URL("Enter link to download", R.drawable.ic_input_code),
        AV_CODE("Enter av number to download", R.drawable.ic_input_code),
        BV_CODE("Enter bv number to download", R.drawable.ic_input_code);
        fun toAction() = SheetActionBean(this, title, icon)
    }
}