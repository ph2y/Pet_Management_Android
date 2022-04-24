package com.sju18001.petmanagement.ui.community.follow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class FollowViewModel : ViewModel() {
    var isApiLoading = MutableLiveData(false)

    var accountId = MutableLiveData<Long?>(null)
    var accountUsername = MutableLiveData("")
    var accountNickname = MutableLiveData("")
    var accountRepresentativePetId = MutableLiveData<Long?>(null)
    var accountPhotoUrl = MutableLiveData("")
    var accountPhotoByteArray = MutableLiveData(byteArrayOf())

    var pageIndex = 0
    var followerIdList = MutableLiveData<MutableList<Long>>(null)
    var followerTitle = MutableLiveData("")
    var followingTitle = MutableLiveData("")
    var searchKeyword = MutableLiveData("")

    fun addFollowerId(followerId: Long){
        followerIdList.value!!.add(followerId)
        // add가 observe되지 않기 때문에 대입 연산을 해야한다.
        followerIdList.value = followerIdList.value
    }
}