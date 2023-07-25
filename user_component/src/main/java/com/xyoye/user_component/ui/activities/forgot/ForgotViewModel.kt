package com.xyoye.user_component.ui.activities.forgot

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.network.Retrofit
import com.xyoye.common_component.network.request.httpRequest
import com.xyoye.common_component.utils.SecurityHelper
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.data.CommonJsonData

class ForgotViewModel : BaseViewModel() {

    val isForgotPassword = ObservableField<Boolean>()

    val accountField = ObservableField("")
    val emailField = ObservableField("")

    val accountErrorLiveData = MutableLiveData<String>()
    val emailErrorLiveData = MutableLiveData<String>()
    val requestLiveData = MutableLiveData<Boolean>()

    fun confirm() {
        if (isForgotPassword.get() == true)
            resetPassword()
        else
            retrieveAccount()
    }

    private fun resetPassword() {
        val account = accountField.get()
        val email = emailField.get()

        val allowReset = checkAccount(account) && checkEmail(email)
        if (!allowReset)
            return

        httpRequest<CommonJsonData>(viewModelScope) {
            onStart { showLoading() }

            api {
                val appId = SecurityHelper.getInstance().appId
                val unixTimestamp = System.currentTimeMillis() / 1000
                val hashInfo = appId + email + unixTimestamp + account
                val hash = SecurityHelper.getInstance().buildHash(hashInfo)

                val params = HashMap<String, String>()
                params["appId"] = appId
                params["userName"] = account!!
                params["email"] = email!!
                params["unixTimestamp"] = unixTimestamp.toString()
                params["hash"] = hash

                Retrofit.service.resetPassword(params)
            }

            onSuccess {
                ToastCenter.showSuccess("The reset is successful, the password has been sent to the mailbox")
                requestLiveData.postValue(true)
            }

            onError { showNetworkError(it) }

            onComplete { hideLoading() }
        }

    }

    private fun retrieveAccount() {
        val email = emailField.get()

        val allowRetrieve = checkEmail(email)
        if (!allowRetrieve)
            return
        httpRequest<CommonJsonData>(viewModelScope) {
            onStart { showLoading() }

            api {
                val appId = SecurityHelper.getInstance().appId
                val unixTimestamp = System.currentTimeMillis() / 1000
                val hashInfo = appId + email + unixTimestamp
                val hash = SecurityHelper.getInstance().buildHash(hashInfo)

                val params = HashMap<String, String>()
                params["appId"] = appId
                params["email"] = email!!
                params["unixTimestamp"] = unixTimestamp.toString()
                params["hash"] = hash

                Retrofit.service.retrieveAccount(params)
            }

            onSuccess {
                ToastCenter.showSuccess("Verification is successful, the account has been sent to the mailbox")
                requestLiveData.postValue(true)
            }

            onError { showNetworkError(it) }

            onComplete { hideLoading() }
        }
    }

    private fun checkAccount(account: String?): Boolean {
        if (account.isNullOrEmpty()) {
            accountErrorLiveData.postValue("please enter your account")
            return false
        }
        return true
    }

    private fun checkEmail(email: String?): Boolean {
        if (email.isNullOrEmpty()) {
            emailErrorLiveData.postValue("please input your email")
            return false
        }
        return true
    }
}