package com.sju18001.petmanagement.ui.myPet.petManager.petProfile

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PetProfileViewModel : ViewModel() {
    var activityType = MutableLiveData<Int>()
    var isPetRepresentative = MutableLiveData(false)
    var isViewsDetailed = MutableLiveData(true)
    var isOrientationPortrait = MutableLiveData<Boolean>()
    var isFollowing = MutableLiveData(false)

    var isApiLoading = false

    var accountId: Long = -1
    var accountUsername: String = ""
    var accountPhotoByteArray = MutableLiveData<ByteArray?>()
    var accountNickname: String = ""
    var accountRepresentativePetId: Long = -1

    var petId = -1L
    var petPhotoByteArray = MutableLiveData<ByteArray?>()
    var petPhotoRotation = MutableLiveData(0f)
    var petMessage = MutableLiveData("")
    var petName = MutableLiveData("")
    var petGender = MutableLiveData(true)
    var petSpecies = MutableLiveData("")
    var petBreed = MutableLiveData("")
    var petAge = MutableLiveData(0)
    var yearOnly = MutableLiveData(false)
    var petBirth = MutableLiveData("0000-00-00")

    fun isActivityTypePetProfile(): Boolean {
        return activityType.value == PetProfileActivity.ActivityType.PET_PROFILE.ordinal
    }

    fun isActivityTypeCommunity(): Boolean {
        return activityType.value == PetProfileActivity.ActivityType.COMMUNITY.ordinal
    }
}