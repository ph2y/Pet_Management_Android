package com.sju18001.petmanagement.ui.login.createAccount

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CreateAccountViewModel: ViewModel() {
    var isApiLoading = MutableLiveData(false)

    var isAllChecked = MutableLiveData(false)
    var isTermsChecked = MutableLiveData(false)
    var isPrivacyChecked = MutableLiveData(false)
    var isMarketingChecked = MutableLiveData(false)

    var username = MutableLiveData("")
    var password = MutableLiveData("")
    var passwordCheck = MutableLiveData("")

    var isUsernameValid = MutableLiveData(false)
    var isUsernameOverlapped = MutableLiveData(false)
    var isPasswordValid = MutableLiveData(false)
    var isPasswordCheckValid = MutableLiveData(false)

    var phone = MutableLiveData("")
    var email = MutableLiveData("")
    var emailCode = MutableLiveData("")

    var isPhoneValid = MutableLiveData(false)
    var isEmailValid = MutableLiveData(false)
    var isPhoneOverlapped = MutableLiveData(false)
    var isEmailOverlapped = MutableLiveData(false)
    var isEmailVerified = MutableLiveData(false)

    var currentCodeRequestedEmail = MutableLiveData("")
    var chronometerBase = MutableLiveData(0L)
}