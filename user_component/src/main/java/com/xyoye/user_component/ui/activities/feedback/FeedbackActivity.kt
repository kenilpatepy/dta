package com.xyoye.user_component.ui.activities.feedback

import android.content.Intent
import android.os.Build
import com.alibaba.android.arouter.facade.annotation.Route
import com.xyoye.common_component.adapter.addItem
import com.xyoye.common_component.adapter.buildAdapter
import com.xyoye.common_component.base.BaseActivity
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.extension.*
import com.xyoye.common_component.utils.AppUtils
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.user_component.BR
import com.xyoye.user_component.R
import com.xyoye.user_component.databinding.ActivityFeedbackBinding
import com.xyoye.user_component.databinding.ItemCommonQuestionBinding

@Route(path = RouteTable.User.Feedback)
class FeedbackActivity : BaseActivity<FeedbackViewModel, ActivityFeedbackBinding>() {

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            FeedbackViewModel::class.java
        )

    override fun getLayoutId() = R.layout.activity_feedback

    override fun initView() {

        title = "Feedback"

        initRv()

        dataBinding.copyQqTv.setOnClickListener {
            R.string.text_qq.toResString().addToClipboard()
            ToastCenter.showSuccess("QQ has been copied！")
        }

        dataBinding.sendMainTv.setOnClickListener {
            val email = R.string.text_email.toResString()
            val androidVersion = "Android ${Build.VERSION.RELEASE}"
            val phoneVersion = Build.MODEL
            val appVersion = AppUtils.getVersionName()

            val intent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, "bounce play - the feedback")
                putExtra(Intent.EXTRA_TEXT, "$phoneVersion\n$androidVersion\n\n$appVersion")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(intent, "Choose a mail client"))
        }

        dataBinding.editIssuesTv.setOnClickListener {
            startUrlActivity("https://github.com/xyoye/DanDanPlayForAndroid/issues")
        }
    }

    private fun initRv() {

        val question = mutableListOf(
            Pair(
                "1. Video resource playback failed",
                "1. Switch playback resources, because the video resources are not owned by Bomb, so the video quality cannot be guaranteed. Generally speaking, newer resources have a higher chance of being able to play.\n2. Switch networks, switch between mobile networks and WIFI\n Note: Playing outside the wall may not be possible"
            ),
            Pair(
                "2. Local video playback failed",
                "Try to switch the player kernel in the player settings, generally choose ijkplayer kernel or exoplayer kernel.\n\nIf you still can't play, please keep the video resource while ensuring that the resource is valid, and contact the developer. The developer may need to use this video resource for testing and improvement"
            ),
            Pair(
                "3. Video playback freezes",
                "Try to enable hard decoding or switch the pixel format type in the player settings, generally choose Yv12 or OpenGL ES2.\n\nIf the playback is still stuck, please keep the video resource and contact the developer. The developer may need to use this video resource for testing"
            ),
            Pair(
                "4. The video cannot be scanned",
                "Try to add this video separately in the scan settings, or add this video folder to the scan directory list.\n\nIn order to ensure a smooth experience in video scanning, the video collection method is to obtain the video inside the system, so some videos may not be scanned in time or cannot be scanned"
            ),
            Pair(
                "5. Barrage double speed related",
                "Currently, the double speed of the barrage only supports the ExoPlayer core, and the double speed is greater than 1"
            )
        )

        dataBinding.feedbackRv.apply {
            layoutManager = vertical()

            adapter = buildAdapter {

                addItem<Pair<String, String>, ItemCommonQuestionBinding>(R.layout.item_common_question) {
                    initView { data, _, _ ->
                        itemBinding.apply {
                            answerTitleTv.text = data.first
                            answerTv.text = data.second
                            expandableLayout.setExpansionObserver { expansionFraction, _ ->
                                arrowIv.rotation = expansionFraction * 90
                            }
                            itemLayout.setOnClickListener { expandableLayout.toggle() }
                        }
                    }
                }
            }

            setData(question)
        }

    }
}