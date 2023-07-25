package com.xyoye.user_component.ui.activities.forgot

import androidx.core.widget.addTextChangedListener
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.xyoye.common_component.base.BaseActivity
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.utils.showKeyboard

import com.xyoye.user_component.BR
import com.xyoye.user_component.R
import com.xyoye.user_component.databinding.ActivityForgotBinding

@Route(path = RouteTable.User.UserForgot)
class ForgotActivity : BaseActivity<ForgotViewModel, ActivityForgotBinding>() {

    @Autowired
    @JvmField
    var isForgotPassword: Boolean = false

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            ForgotViewModel::class.java
        )

    override fun getLayoutId() = R.layout.activity_forgot

    override fun initView() {
        ARouter.getInstance().inject(this)

        title = ""

        dataBinding.titleTv.text = if (isForgotPassword) "Reset password" else "Retrieve account"
        dataBinding.confirmBt.text = if (isForgotPassword) "Reset" else "OK"

        val tips =
            resources.getString(if (isForgotPassword) R.string.tips_reset_password else R.string.tips_retrieve_account)
        dataBinding.tipsTv.text = tips

        viewModel.isForgotPassword.set(isForgotPassword)

        dataBinding.apply {
            if (isForgotPassword) {
                userAccountEt.postDelayed({
                    showKeyboard(userAccountEt)
                }, 200)
            } else {
                userEmailEt.postDelayed({
                    showKeyboard(userEmailEt)
                }, 200)
            }

            userAccountEt.addTextChangedListener {
                userAccountLayout.error = ""
            }
            userEmailEt.addTextChangedListener {
                userAccountLayout.error = ""
            }
        }

        viewModel.accountErrorLiveData.observe(this) {
            dataBinding.userAccountLayout.error = it
        }
        viewModel.emailErrorLiveData.observe(this) {
            dataBinding.userEmailLayout.error = it
        }
        viewModel.requestLiveData.observe(this) {
            finish()
        }
    }
}