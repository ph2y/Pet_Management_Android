package com.sju18001.petmanagement.ui.setting

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

class SettingViewModel(private val handle: SavedStateHandle) : ViewModel() {
    // variables for account profile fetch in setting
    var accountNicknameProfileValue = handle.get<String>("accountNicknameProfileValue")?: ""
        set(value){
            handle.set("accountNicknameProfileValue", value)
            field = value
        }
    var accountPhotoProfileByteArray = handle.get<ByteArray>("accountPhotoProfileByteArray")
        set(value){
            handle.set("accountPhotoProfileByteArray", value)
            field = value
        }

    // variables for account profile fetch/update
    var loadedFromIntent = handle.get<Boolean>("loadedFromIntent")?: false
        set(value){
            handle.set("loadedFromIntent", value)
            field = value
        }
    var accountEmailValue = handle.get<String>("accountEmailValue")?: ""
        set(value){
            handle.set("accountEmailValue", value)
            field = value
        }
    var accountPhoneValue = handle.get<String>("accountPhoneValue")?: ""
        set(value){
            handle.set("accountPhoneValue", value)
            field = value
        }
    var accountMarketingValue = handle.get<Boolean>("accountMarketingValue")
        set(value){
            handle.set("accountMarketingValue", value)
            field = value
        }
    var accountNicknameValue = handle.get<String>("accountNicknameValue")?: ""
        set(value){
            handle.set("accountNicknameValue", value)
            field = value
        }
    var accountPhotoByteArray = handle.get<ByteArray>("accountPhotoByteArray")
        set(value){
            handle.set("accountPhotoByteArray", value)
            field = value
        }
    var accountPhotoPathValue = handle.get<String>("accountPhotoPathValue")?: ""
        set(value){
            handle.set("accountPhotoPathValue", value)
            field = value
        }
    var isDeletePhoto = handle.get<Boolean>("isDeletePhoto")?: false
        set(value) {
            handle.set("isDeletePhoto", value)
            field = value
        }
    var accountUserMessageValue = handle.get<String>("accountUserMessageValue")?: ""
        set(value){
            handle.set("accountUserMessageValue", value)
            field = value
        }
    var representativePetId = handle.get<Long>("representativePetId")?: 0
        set(value){
            handle.set("representativePetId", value)
            field = value
        }
    var fcmRegistrationToken = handle.get<String>("fcmRegistrationToken")?: null
        set(value){
            handle.set("fcmRegistrationToken", value)
            field = value
        }
    var notification = handle.get<Boolean>("notification")?: true
        set(value){
            handle.set("notification", value)
            field = value
        }
    var mapSearchRadius = handle.get<Double>("mapSearchRadius")?:0.0
        set(value){
            handle.set("mapSearchRadius", value)
            field = value
        }
    var accountPwValid = handle.get<Boolean>("createAccountPwValid")?: false
        set(value){
            handle.set("createAccountPwValid", value)
            field = value
        }
    var accountPwCheckValid = handle.get<Boolean>("createAccountPwCheckValid")?: false
        set(value){
            handle.set("createAccountPwCheckValid", value)
            field = value
        }

    // ViewModel for radius preference fragment
    private val isViewModelInitializedForRadius: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(false)
    }
    val isApiLoading: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
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

    fun updateAccountWithNewRadius(context: Context, isViewDestroyed: Boolean, mapSearchRadius: Double) {
        val loggedInAccount = SessionManager.fetchLoggedInAccount(context)!!
        val updateAccountReqDto = UpdateAccountReqDto(
            loggedInAccount.email, loggedInAccount.phone, loggedInAccount.nickname, loggedInAccount.marketing,
            loggedInAccount.userMessage, loggedInAccount.representativePetId, loggedInAccount.notification,
            mapSearchRadius
        )

        isApiLoading.value = true
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
            .updateAccountReq(updateAccountReqDto)
        ServerUtil.enqueueApiCall(call, { isViewDestroyed }, context, { response ->
            if (response.body()?._metadata?.status == true) {
                updateLoggedInAccount(context, loggedInAccount, mapSearchRadius)
                radiusSlider.value = mapSearchRadius
                isApiLoading.value = false

                Toast.makeText(context, R.string.radius_update_success_message, Toast.LENGTH_SHORT).show()
            }
        }, { isApiLoading.value = false }, { isApiLoading.value = false })
    }

    private fun updateLoggedInAccount(context: Context, prevAccount: Account, mapSearchRadius: Double) {
        val updatedAccount = Account(
            prevAccount.id, prevAccount.username, prevAccount.email, prevAccount.phone, prevAccount.password,
            prevAccount.marketing, prevAccount.nickname, prevAccount.photoUrl, prevAccount.userMessage,
            prevAccount.representativePetId, prevAccount.fcmRegistrationToken, prevAccount.notification,
            mapSearchRadius
        )

        SessionManager.saveLoggedInAccount(context, updatedAccount)
    }
}