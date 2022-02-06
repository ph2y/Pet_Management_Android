package com.sju18001.petmanagement.ui.community.followerFollowing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class FollowerFollowingViewModel(private val handle: SavedStateHandle) : ViewModel() {
    private val followerTitle: MutableLiveData<String> = MutableLiveData()
    private val followingTitle: MutableLiveData<String> = MutableLiveData()

    public fun setFollowerTitle(input: String) {
        followerTitle.value = input
    }
    public fun getFollowerTitle(): LiveData<String> {
        return followerTitle
    }
    public fun setFollowingTitle(input: String) {
        followingTitle.value = input
    }
    public fun getFollowingTitle(): LiveData<String> {
        return followingTitle
    }

    var followerIdList = handle.get<MutableList<Long>>("followerIdList")
        set(value) {
            handle.set("followerIdList", value)
            field = value
        }

    var searchEditText = handle.get<String>("searchEditText")?: ""
        set(value){
            handle.set("searchEditText", value)
            field = value
        }

    var accountPhotoUrl = handle.get<String>("accountPhotoUrl")
        set(value){
            handle.set("accountPhotoUrl", value)
            field = value
        }

    var accountPhotoByteArray = handle.get<ByteArray>("accountPhotoByteArray")
        set(value){
            handle.set("accountPhotoByteArray", value)
            field = value
        }

    var accountId = handle.get<Long>("accountId")
        set(value){
            handle.set("accountId", value)
            field = value
        }

    var accountUsername = handle.get<String>("accountUsername")
        set(value){
            handle.set("accountUsername", value)
            field = value
        }

    var accountNickname = handle.get<String>("accountNickname")
        set(value){
            handle.set("accountNickname", value)
            field = value
        }

    var accountRepresentativePetId = handle.get<Long>("accountRepresentativePetId")
        set(value){
            handle.set("accountRepresentativePetId", value)
            field = value
        }

    var apiIsLoading = handle.get<Boolean>("apiIsLoading")?: false
        set(value){
            handle.set("apiIsLoading", value)
            field = value
        }
}