package com.xyoye.storage_component.ui.activities.screencast.receiver

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.alibaba.android.arouter.facade.annotation.Route
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsBuildBitmapOption
import com.huawei.hms.ml.scan.HmsScan
import com.xyoye.common_component.base.BaseActivity
import com.xyoye.common_component.bridge.ServiceLifecycleBridge
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.config.ScreencastConfig
import com.xyoye.common_component.extension.toResColor
import com.xyoye.common_component.utils.JsonHelper
import com.xyoye.common_component.utils.dp2px
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.common_component.weight.dialog.CommonDialog
import com.xyoye.data_component.data.RemoteScanData
import com.xyoye.storage_component.BR
import com.xyoye.storage_component.R
import com.xyoye.storage_component.databinding.ActivityScreenCastBinding
import com.xyoye.storage_component.services.ScreencastReceiveService
import kotlin.random.Random


@Route(path = RouteTable.Stream.ScreencastReceiver)
class ScreencastActivity : BaseActivity<ScreencastViewModel, ActivityScreenCastBinding>() {

    private var httpPort = 0

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            ScreencastViewModel::class.java
        )

    override fun getLayoutId() = R.layout.activity_screen_cast

    override fun initView() {

        title = "Screen projection receiver"

        initListener()

        initPort()

        initPassword()

        if (ScreencastReceiveService.isRunning(this)) {
            setupEnabledStyle()
        } else {
            setupDisableStyle()
        }

        dataBinding.needConfirmSwitch.isChecked = ScreencastConfig.isReceiveNeedConfirm()

        dataBinding.autoStartSwitch.isChecked = ScreencastConfig.isStartReceiveWhenLaunch()

        viewModel.initIpPort()
    }

    private fun initListener() {
        ServiceLifecycleBridge.getScreencastReceiveObserver().observe(this) {
            if (it) {
                setupEnabledStyle()
            } else {
                setupDisableStyle()
            }
        }

        dataBinding.serverSwitchTv.setOnClickListener {
            if (ScreencastReceiveService.isRunning(this)) {
                stopService()
                return@setOnClickListener
            }

            startService()
        }

        dataBinding.passwordRadioGp.setOnCheckedChangeListener { _, checkedId ->
            val isNoPassword = dataBinding.noPasswordRb.id == checkedId
            dataBinding.passwordEt.isEnabled = isNoPassword.not()
            dataBinding.refreshPasswordIv.isInvisible = isNoPassword

            if (isNoPassword.not()) {
                var password = dataBinding.passwordEt.text.toString()
                if (password.isEmpty()) {
                    password = viewModel.createRandomPwd()
                    dataBinding.passwordEt.setText(password)
                }
            }
        }

        dataBinding.refreshPasswordIv.setOnClickListener {
            val newPassword = viewModel.createRandomPwd()
            dataBinding.passwordEt.setText(newPassword)
        }

        dataBinding.refreshPortIv.setOnClickListener {
            CommonDialog.Builder(this).run {
                title = "hint"
                content = "Are you sure to change the port number?\n" +
                        "\n" +
                        "Connected devices need to be reconnected after replacement"
                addPositive {
                    httpPort = Random.nextInt(20000, 30000)
                    ScreencastConfig.putReceiverPort(httpPort)
                    dataBinding.portTv.text = httpPort.toString()
                    it.dismiss()
                }
                addNegative { it.dismiss() }
                build()
            }.show()
        }

        dataBinding.needConfirmSwitch.setOnCheckedChangeListener { _, isChecked ->
            ScreencastConfig.putReceiveNeedConfirm(isChecked)
        }

        dataBinding.autoStartSwitch.setOnCheckedChangeListener { _, isChecked ->
            ScreencastConfig.putStartReceiveWhenLaunch(isChecked)
        }
    }

    private fun initPort() {
        httpPort = ScreencastConfig.getReceiverPort()
        if (httpPort == 0) {
            httpPort = Random.nextInt(20000, 30000)
            ScreencastConfig.putReceiverPort(httpPort)
        }
        dataBinding.portTv.text = httpPort.toString()
    }

    private fun initPassword() {
        val isUsePwd = ScreencastConfig.isUseReceiverPassword()
        dataBinding.noPasswordRb.isChecked = isUsePwd.not()
        dataBinding.setPasswordRb.isChecked = isUsePwd
        dataBinding.refreshPasswordIv.isInvisible = isUsePwd.not()

        val receiverPwd = ScreencastConfig.getReceiverPassword()
        if (receiverPwd.isNullOrEmpty() && isUsePwd) {
            val randomPwd = viewModel.createRandomPwd()
            dataBinding.passwordEt.setText(randomPwd)
        } else if (receiverPwd.isNullOrEmpty().not()) {
            dataBinding.passwordEt.setText(receiverPwd)
        }
    }

    private fun startService() {
        var password: String? = null
        if (dataBinding.setPasswordRb.isChecked) {
            val inputPwd = dataBinding.passwordEt.text.toString()
            if (inputPwd.isEmpty() || inputPwd.length != 8) {
                ToastCenter.showWarning("The password needs to be set to 8 characters")
                return
            }
            password = inputPwd
        }

        ScreencastConfig.putReceiverPassword(password ?: "")
        ScreencastConfig.putUseReceiverPassword(password.isNullOrEmpty().not())
        ScreencastReceiveService.start(this, httpPort, password)
    }

    private fun stopService() {
        ScreencastReceiveService.stop(this)
    }

    private fun setupDisableStyle() {
        createQRCode("Please activate the screencasting receiving service", false)?.let {
            dataBinding.qrCodeIv.setImageBitmap(it)
        }
        dataBinding.serverStatusTv.text = "have not started"
        dataBinding.serverStatusTv.setTextColor(R.color.text_red.toResColor())

        dataBinding.serverSwitchTv.text = "start service"
        dataBinding.serverSwitchTv.setTextColor(R.color.text_white.toResColor())
        dataBinding.serverSwitchTv.isSelected = false

        dataBinding.setPasswordRb.isEnabled = true
        dataBinding.noPasswordRb.isEnabled = true
        dataBinding.passwordEt.isEnabled = true
        dataBinding.refreshPasswordIv.isEnabled = true
        dataBinding.refreshPortIv.isVisible = true
    }

    private fun setupEnabledStyle() {
        val qrCodeContent = RemoteScanData(
            viewModel.ipList,
            httpPort,
            Build.MODEL,
            dataBinding.setPasswordRb.isChecked,
            null
        )
        val qrCodeJson = JsonHelper.toJson(qrCodeContent) ?: ""
        createQRCode(qrCodeJson, true)?.let {
            dataBinding.qrCodeIv.setImageBitmap(it)
        }

        dataBinding.serverStatusTv.text = "activated"
        dataBinding.serverStatusTv.setTextColor(R.color.text_theme.toResColor())

        dataBinding.serverSwitchTv.text = "Out of service"
        dataBinding.serverSwitchTv.setTextColor(R.color.text_black.toResColor())
        dataBinding.serverSwitchTv.isSelected = true

        dataBinding.setPasswordRb.isEnabled = false
        dataBinding.noPasswordRb.isEnabled = false
        dataBinding.passwordEt.isEnabled = false
        dataBinding.refreshPasswordIv.isEnabled = false
        dataBinding.refreshPortIv.isVisible = false
    }

    private fun createQRCode(content: String, enable: Boolean = true): Bitmap? {
        val logoRes = if (enable) R.mipmap.ic_logo else R.mipmap.ic_logo_gray
        val bmpColor = if (enable) R.color.text_black else R.color.text_gray

        try {
            val logo = BitmapFactory.decodeResource(resources, logoRes)
            val options = HmsBuildBitmapOption.Creator()
                .setQRLogoBitmap(logo)
                .setBitmapColor(bmpColor.toResColor())
                .create()
            return ScanUtil.buildBitmap(
                content,
                HmsScan.QRCODE_SCAN_TYPE,
                dp2px(200),
                dp2px(200),
                options
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}