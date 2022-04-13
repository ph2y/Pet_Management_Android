package com.sju18001.petmanagement.ui.myPet.petScheduleManager.createUpdatePetSchedule

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalTime

class CreateUpdatePetScheduleViewModel: ViewModel() {
    var activityType = MutableLiveData<Int>()
    var isApiLoading = MutableLiveData(false)
    var checkedPetCount = MutableLiveData(0)

    var hour = MutableLiveData(0)
    var minute = MutableLiveData(0)
    var memo = MutableLiveData("")
    var isPetChecked = MutableLiveData<MutableList<Boolean>>(mutableListOf())

    fun isActivityTypeCreatePetSchedule(): Boolean {
        return activityType.value == CreateUpdatePetScheduleActivity.ActivityType.CREATE_PET_SCHEDULE.ordinal
    }

    fun isActivityTypeUpdatePetSchedule(): Boolean {
        return activityType.value == CreateUpdatePetScheduleActivity.ActivityType.UPDATE_PET_SCHEDULE.ordinal
    }
}