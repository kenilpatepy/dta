package com.xyoye.common_component.weight.dialog

import android.app.Activity
import android.os.CountDownTimer
import androidx.core.view.isVisible
import com.xyoye.common_component.R
import com.xyoye.common_component.databinding.DialogCommonBinding
import com.xyoye.common_component.extension.setTextColorRes
import com.xyoye.common_component.extension.toResString

/**
 * Created by xyoye on 2020/10/28.
 */

open class CommonDialog private constructor(
    activity: Activity,
    private val builder: Builder
) : BaseBottomDialog<DialogCommonBinding>(activity) {

    private val delayTimer = object : CountDownTimer(5000L, 1000L) {
        override fun onTick(millisUntilFinished: Long) {
            rootViewBinding.positiveBt.isEnabled = false
            val time = (millisUntilFinished / 1000L).toInt() + 1
            val timeText = "$time S"
            rootViewBinding.positiveBt.setTextColorRes(R.color.text_gray)
            setPositiveText(timeText)
        }

        override fun onFinish() {
            rootViewBinding.positiveBt.isEnabled = true
            rootViewBinding.positiveBt.setTextColorRes(R.color.text_theme)
            setPositiveText("Sure")
        }
    }

    open class Builder(private val activity: Activity) {
        var tips: String? = null
        var content: String = ""
        var cancelable = true
        var touchCancelable = true
        var delayConfirm = false
        var negativeText: String? = null
        var negativeClickListener: ((CommonDialog) -> Unit)? = null
        var positiveText: String? = null
        var positiveClickListener: ((CommonDialog) -> Unit)? = null
        var noShowAgain: Boolean = false
        var noShowAgainTips: String = ""
        var noShowAgainListener: ((Boolean) -> Unit)? = null
        var doOnDismiss: (() -> Unit)? = null

        open fun addNegative(
            negativeText: String = "Cancel",
            negativeClickListener: ((CommonDialog) -> Unit) = { dialog -> dialog.dismiss() }
        ): Builder {
            this.negativeClickListener = negativeClickListener
            this.negativeText = negativeText
            return this
        }

        open fun addPositive(
            positiveText: String = "Sure",
            positiveClickListener: ((CommonDialog) -> Unit)
        ): Builder {
            this.positiveClickListener = positiveClickListener
            this.positiveText = positiveText
            return this
        }

        open fun addNoShowAgain(
            tips: String = R.string.check_not_show_again.toResString(),
            listener: (Boolean) -> Unit
        ): Builder {
            noShowAgain = true
            noShowAgainTips = tips
            noShowAgainListener = listener
            return this
        }

        open fun doOnDismiss(action: () -> Unit): Builder {
            doOnDismiss = action
            return this
        }

        fun build(): CommonDialog = CommonDialog(activity, this)
    }

    override fun getChildLayoutId() = R.layout.dialog_common

    override fun initView(binding: DialogCommonBinding) {

        builder.apply {

            setTitle(tips ?: "hint")

            binding.contentTv.text = content

            setDialogCancelable(touchCancelable, cancelable)

            setPositiveVisible(false)
            setNegativeVisible(false)

            negativeText?.let {
                setNegativeVisible(true)
                setNegativeText(it)
            }
            positiveText?.let {
                setPositiveVisible(true)
                setPositiveText(it)
            }

            binding.noShowAgainCb.text = noShowAgainTips
            binding.noShowAgainCb.isVisible = noShowAgain
            binding.noShowAgainCb.setOnCheckedChangeListener { _, isChecked ->
                noShowAgainListener?.invoke(isChecked)
            }

            setNegativeListener { negativeClickListener?.invoke(this@CommonDialog) }

            setPositiveListener { positiveClickListener?.invoke(this@CommonDialog) }

            setOnDismissListener {
                delayTimer.cancel()
                doOnDismiss?.invoke()
            }

            if (delayConfirm) {
                delayTimer.start()
            }
        }
    }
}