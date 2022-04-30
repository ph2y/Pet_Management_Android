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
    var nickname = MutableLiveData("")
    var photoUrl = MutableLiveData("")

    var photoByteArray = MutableLiveData(byteArrayOf())
}