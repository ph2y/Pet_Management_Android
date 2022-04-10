package com.sju18001.petmanagement.ui.myPet.petManager.petProfile

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CreateUpdatePetViewModel : ViewModel() {
    var activityType = MutableLiveData<Int>()

    var isApiLoading = MutableLiveData(false)

    var petId = -1L
    var petPhotoByteArray = MutableLiveData<ByteArray>(null)
    var petPhotoPath = MutableLiveData("")
    var petPhotoRotation = MutableLiveData(0f)
    var petMessage = MutableLiveData("")
    var petName = MutableLiveData("")
    var petGender = MutableLiveData<Boolean>()
    var petSpecies = MutableLiveData("")
    var petBreed = MutableLiveData("")
    var yearOnly = MutableLiveData(false)
    var petBirthYear = MutableLiveData(2022)
    var petBirthMonth = MutableLiveData(1)
    var petBirthDay = MutableLiveData(1)

    fun isActivityTypeCreatePet(): Boolean {
        return activityType.value == CreateUpdatePetActivity.ActivityType.CREATE_PET.ordinal
    }

    fun isActivityTypeUpdatePet(): Boolean {
        return activityType.value == CreateUpdatePetActivity.ActivityType.UPDATE_PET.ordinal
    }
}