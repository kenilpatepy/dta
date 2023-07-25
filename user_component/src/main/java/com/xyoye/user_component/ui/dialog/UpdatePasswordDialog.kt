package com.xyoye.user_component.ui.dialog

import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.appcompat.app.AppCompatActivity
import com.xyoye.common_component.utils.hideKeyboard
import com.xyoye.common_component.utils.showKeyboard
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.common_component.weight.dialog.BaseBottomDialog
import com.xyoye.user_component.R
import com.xyoye.user_component.databinding.DialogUpdatePasswordBinding

/**
 * Created by xyoye on 2021/1/11.
 */

class UpdatePasswordDialog(
    activity: AppCompatActivity,
    private val callback: (old: String, new: String) -> Boolean
) : BaseBottomDialog<DialogUpdatePasswordBinding>(activity) {

    private lateinit var binding: DialogUpdatePasswordBinding

    override fun getChildLayoutId() = R.layout.dialog_update_password

    override fun initView(binding: DialogUpdatePasswordBinding) {
        this.binding = binding

        setTitle("change Password")

        setNegativeListener { dismiss() }

        setPositiveListener {
            val oldPassword = binding.oldPasswordEt.text.toString()
            val newPassword = binding.newPasswordEt.text.toString()
            if (oldPassword.isEmpty()) {
                ToastCenter.showWarning("Old password cannot be empty")
                return@setPositiveListener
            }
            if (newPassword.isEmpty()) {
                ToastCenter.showWarning("New password cannot be empty")
                return@setPositiveListener
            }
            if (oldPassword.length < 5 || newPassword.length < 5) {
                ToastCenter.showWarning("Password length should be 5-20")
                return@setPositiveListener
            }

            if (callback.invoke(oldPassword, newPassword)) {
                dismiss()
            }
        }

        binding.newPasswordVisibleIv.isSelected = false
        binding.newPasswordVisibleIv.setOnClickListener {
            if (binding.newPasswordVisibleIv.isSelected) {
                binding.newPasswordVisibleIv.isSelected = false
                binding.newPasswordEt.transformationMethod =
                    PasswordTransformationMethod.getInstance()
            } else {
                binding.newPasswordVisibleIv.isSelected = true
                binding.newPasswordEt.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
            }
        }

        binding.oldPasswordEt.postDelayed({ showKeyboard(binding.oldPasswordEt) }, 200)
    }

    override fun dismiss() {
        hideKeyboard(binding.oldPasswordEt)
        hideKeyboard(binding.newPasswordEt)
        super.dismiss()
    }
}