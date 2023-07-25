package com.xyoye.user_component.ui.activities.login

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.network.Retrofit
import com.xyoye.common_component.network.request.httpRequest
import com.xyoye.common_component.utils.SecurityHelper
import com.xyoye.common_component.utils.UserInfoHelper
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.data.LoginData

class LoginViewModel : BaseViewModel() {

    val accountField = ObservableField("")
    val passwordField = ObservableField("")

    val accountErrorLiveData = MutableLiveData<String>()
    val passwordErrorLiveData = MutableLiveData<String>()
    val loginLiveData = MutableLiveData<LoginData>()

    fun login() {

        val account = accountField.get()
        val password = passwordField.get()

        val allowLogin = checkAccount(account) && checkPassword(password)
        if (!allowLogin)
            return

        httpRequest<LoginData>(viewModelScope) {
            onStart { showLoading() }

            api {
                val appId = SecurityHelper.getInstance().appId
                val unixTimestamp = System.currentTimeMillis() / 1000
                val hashInfo = appId + password + unixTimestamp + account
                val hash = SecurityHelper.getInstance().buildHash(hashInfo)

                val params = HashMap<String, String>()
                params["userName"] = account!!
                params["password"] = password!!
                params["appId"] = appId
                params["unixTimestamp"] = unixTimestamp.toString()
                params["hash"] = hash

                Retrofit.service.login(params)
            }

            onSuccess {
                if (UserInfoHelper. login(it)){
                    ToastCenter.showSuccess("Successful login")
                    loginLiveData. postValue(it)
                } else {
                    ToastCenter.showError("Login error, please try again later")
                }
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

    private fun checkPassword(password: String?): Boolean {
        if (password.isNullOrEmpty()) {
            passwordErrorLiveData.postValue("please enter password")
            return false
        }
        return true
    }
}