package com.sju18001.petmanagement.ui.login.recover

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RecoverViewModel: ViewModel() {
    var isApiLoading = MutableLiveData(false)

    var emailForRecoverUsername = MutableLiveData("")
    var isEmailForRecoverUsernameValid = MutableLiveData(false)
    var isUsernameShowing = MutableLiveData(false)
    var username = MutableLiveData("")

    // 0: 이메일 입력, 1: 아이디 및 인증코드 입력, 2: 결과
    var recoverPasswordPhase = MutableLiveData(0)
    var emailForRecoverPassword = MutableLiveData("")
    var usernameForRecoverPassword = MutableLiveData("")
    var authCodeForRecoverPassword = MutableLiveData("")
    var isEmailForRecoverPasswordValid = MutableLiveData(false)
    var isUsernameForRecoverPasswordValid = MutableLiveData(false)
}