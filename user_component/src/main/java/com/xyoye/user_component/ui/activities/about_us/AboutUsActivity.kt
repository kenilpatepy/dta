package com.xyoye.user_component.ui.activities.about_us

import android.annotation.SuppressLint
import com.alibaba.android.arouter.facade.annotation.Route
import com.xyoye.common_component.base.BaseActivity
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.extension.startUrlActivity
import com.xyoye.common_component.utils.AppUtils
import com.xyoye.user_component.BR
import com.xyoye.user_component.BuildConfig
import com.xyoye.user_component.R
import com.xyoye.user_component.databinding.ActivityAboutUsBinding

@Route(path = RouteTable.User.AboutUs)
class AboutUsActivity : BaseActivity<AboutUsViewModel, ActivityAboutUsBinding>() {

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            AboutUsViewModel::class.java
        )

    override fun getLayoutId() = R.layout.activity_about_us

    @SuppressLint("SetTextI18n")
    override fun initView() {

        title = ""

       // val describe = "Dandan Play Concept Edition" is a local video player, which is the realization of the Android platform of the DanDanPlay series of applications. It is dedicated to the playback of video + bullet screen, and brings you a pleasant viewing experience." + "\n\n"Dandan Play Concept Edition" is also a free and open source Android project, following the Apache 2.0 protocol. The project is only for personal study and research, and does not participate in any commercial activities."

        val describe = "data"
        dataBinding.appDescribeTv.text = describe

        dataBinding.versionTv.text = "v${AppUtils.getVersionName()}  ${BuildConfig.BUILD_COMMIT}"

        dataBinding.officialAddressTv.setOnClickListener {
            startUrlActivity("https://dandanplay.com")
        }

        dataBinding.sourceAddressTv.setOnClickListener {
            startUrlActivity("https://github.com/xyoye/DanDanPlayForAndroid")
        }
    }
}