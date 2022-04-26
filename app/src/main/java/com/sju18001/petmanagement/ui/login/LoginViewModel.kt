package com.sju18001.petmanagement.ui.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class LoginViewModel: ViewModel() {
    var isApiLoading = MutableLiveData(false)

    var loginUsername = MutableLiveData("")
    var loginPassword = MutableLiveData("")

    var isAllChecked = MutableLiveData(false)
    var isTermsChecked = MutableLiveData(false)
    var isPrivacyChecked = MutableLiveData(false)
    var isMarketingChecked = MutableLiveData(false)

    var createAccountUsername = MutableLiveData("")
    var createAccountPassword = MutableLiveData("")
    var createAccountPasswordCheck = MutableLiveData("")

    var isUsernameValid = MutableLiveData(false)
    var isUsernameOverlapped = MutableLiveData(false)
    var isPasswordValid = MutableLiveData(false)
    var isPasswordCheckValid = MutableLiveData(false)

    var createAccountPhone = MutableLiveData("")
    var createAccountEmail = MutableLiveData("")
    var createAccountEmailCode = MutableLiveData("")

    var isPhoneValid = MutableLiveData(false)
    var isEmailValid = MutableLiveData(false)
    var isPhoneOverlapped = MutableLiveData(false)
    var isEmailOverlapped = MutableLiveData(false)

    var isEmailLocked = MutableLiveData(false)
    var currentCodeRequestedEmail = MutableLiveData("")
    var chronometerBase = MutableLiveData(0L)
}