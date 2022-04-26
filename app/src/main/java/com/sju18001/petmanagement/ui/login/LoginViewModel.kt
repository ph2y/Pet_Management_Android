package com.sju18001.petmanagement.ui.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class LoginViewModel: ViewModel() {
    var isApiLoading = MutableLiveData(false)

    var username = MutableLiveData("")
    var password = MutableLiveData("")
}