package com.sju18001.petmanagement.ui.myPet.petManager.petProfile

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CreateUpdatePetViewModel : ViewModel() {
    var fragmentType = MutableLiveData<Int>()

    var isViewModelInitializedByBundle = false
    var isApiLoading = false

    var petId = -1L

    var petPhotoByteArray: ByteArray? = null
    var petPhotoPath: String = ""
    var petPhotoRotation: Float = 0f
    var isDeletePhoto = false
    var petMessage: String? = null
    var petName: String? = null
    var petGender: Boolean? = null
    var petSpecies: String? = null
    var petBreed: String? = null
    var yearOnly = false
    var petBirth: String = "0000-00-00"
}