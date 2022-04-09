package com.sju18001.petmanagement.ui.myPet.petManager.petProfile

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PetProfileViewModel : ViewModel() {
    var fragmentType = MutableLiveData<Int>()
    var isPetRepresentative = MutableLiveData<Boolean>()
    var isViewsDetailed = MutableLiveData<Boolean>()
    var isOrientationPortrait = MutableLiveData<Boolean>()

    var isApiLoading = false

    var accountId: Long = -1
    var accountUsername: String = ""
    var accountPhotoByteArray: ByteArray? = null
    var accountNickname: String = ""
    var accountRepresentativePetId: Long = -1

    var petId = -1L
    var petPhotoByteArray: ByteArray? = null
    var petPhotoRotation: Float = 0f
    var petMessage: String = ""
    var petName: String = ""
    var petGender: Boolean = false
    var petSpecies: String = ""
    var petBreed: String = ""
    var petAge: Int = 0
    var yearOnly = false
    var petBirth: String = "0000-00-00"

    fun isFragmentTypePetProfileFromMyPet(): Boolean {
        return fragmentType.value == PetProfileActivity.FragmentType.PET_PROFILE_FROM_MY_PET.ordinal
    }

    fun isFragmentTypePetProfileFromCommunity(): Boolean {
        return fragmentType.value == PetProfileActivity.FragmentType.PET_PROFILE_FROM_COMMUNITY.ordinal
    }
}