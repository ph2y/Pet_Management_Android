package com.sju18001.petmanagement.ui.login.recover

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RecoverViewModel: ViewModel() {
    var isApiLoading = MutableLiveData(false)

    var emailForRecoverUsername = MutableLiveData("")
    var isEmailForRecoverUsernameValid = MutableLiveData(false)
    var isUsernameShowing = MutableLiveData(false)
    var username = MutableLiveData("")
}