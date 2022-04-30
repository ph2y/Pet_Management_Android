package com.sju18001.petmanagement.ui.setting.detailedSetting

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dto.UpdateAccountReqDto

class DetailedSettingViewModel() : ViewModel() {
    val isApiLoading = MutableLiveData(false)
    var isViewModelInitialized = false

    var id = MutableLiveData(-1L)
    var username = MutableLiveData("")
    var email = MutableLiveData("")
    var phone = MutableLiveData("")
    var marketing = MutableLiveData(false)
    var nickname = MutableLiveData("")
    var photoUrl = MutableLiveData("")
    var userMessage = MutableLiveData("")
    var representativePetId = MutableLiveData(-1L)

    var photoByteArray = MutableLiveData(byteArrayOf())
    var photoPath = MutableLiveData("")
    var hasPhotoChanged = false


    var notification = true

    // ViewModel for radius preferences fragment
    private val isViewModelInitializedForRadius: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(false)
    }
    val radiusSlider: MutableLiveData<Double> by lazy {
        MutableLiveData<Double>()
    }

    fun initializeViewModelForRadius(context: Context) {
        if (isViewModelInitializedForRadius.value!!) {
            return
        }

        isApiLoading.value = false
        radiusSlider.value = SessionManager.fetchLoggedInAccount(context)!!.mapSearchRadius

        isViewModelInitializedForRadius.value = true
    }

    fun updateAccountWithNewRadius(context: Context, isViewDestroyed: Boolean, newRadius: Double) {
        val loggedInAccount = SessionManager.fetchLoggedInAccount(context)!!
        val updateAccountReqDto = UpdateAccountReqDto(
            loggedInAccount.email, loggedInAccount.phone, loggedInAccount.nickname, loggedInAccount.marketing,
            loggedInAccount.userMessage, loggedInAccount.representativePetId, loggedInAccount.notification,
            newRadius
        )

        isApiLoading.value = true
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
            .updateAccountReq(updateAccountReqDto)
        ServerUtil.enqueueApiCall(call, { isViewDestroyed }, context, { response ->
            if (response.body()?._metadata?.status == true) {
                updateLoggedInAccountWithNewRadius(context, loggedInAccount, newRadius)
                radiusSlider.value = newRadius
                isApiLoading.value = false

                Toast.makeText(context, R.string.update_success_message, Toast.LENGTH_SHORT).show()
            }
        }, { isApiLoading.value = false }, { isApiLoading.value = false })
    }

    private fun updateLoggedInAccountWithNewRadius(context: Context, prevAccount: Account, newRadius: Double) {
        val updatedAccount = Account(
            prevAccount.id, prevAccount.username, prevAccount.email, prevAccount.phone, prevAccount.password,
            prevAccount.marketing, prevAccount.nickname, prevAccount.photoUrl, prevAccount.userMessage,
            prevAccount.representativePetId, prevAccount.fcmRegistrationToken, prevAccount.notification,
            newRadius
        )

        SessionManager.saveLoggedInAccount(context, updatedAccount)
    }


    // For notification preferences fragment
    fun updateAccountWithNewNotification(context: Context, isViewDestroyed: Boolean, newNotification: Boolean) {
        val loggedInAccount = SessionManager.fetchLoggedInAccount(context)!!
        val updateAccountReqDto = UpdateAccountReqDto(
            loggedInAccount.email, loggedInAccount.phone, loggedInAccount.nickname, loggedInAccount.marketing,
            loggedInAccount.userMessage, loggedInAccount.representativePetId, newNotification,
            loggedInAccount.mapSearchRadius
        )

        isApiLoading.value = true
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
            .updateAccountReq(updateAccountReqDto)
        ServerUtil.enqueueApiCall(call, { isViewDestroyed }, context, { response ->
            updateLoggedInAccountWithNewNotification(context, loggedInAccount, newNotification)
            isApiLoading.value = false

            Toast.makeText(context, R.string.update_success_message, Toast.LENGTH_SHORT).show()
        }, { isApiLoading.value = false }, { isApiLoading.value = false })
    }

    private fun updateLoggedInAccountWithNewNotification(context: Context, prevAccount: Account, newNotification: Boolean) {
        val updatedAccount = Account(
            prevAccount.id, prevAccount.username, prevAccount.email, prevAccount.phone, prevAccount.password,
            prevAccount.marketing, prevAccount.nickname, prevAccount.photoUrl, prevAccount.userMessage,
            prevAccount.representativePetId, prevAccount.fcmRegistrationToken, newNotification,
            prevAccount.mapSearchRadius
        )

        SessionManager.saveLoggedInAccount(context, updatedAccount)
    }
}