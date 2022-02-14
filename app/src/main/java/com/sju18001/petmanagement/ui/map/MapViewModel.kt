package com.sju18001.petmanagement.ui.map

import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MapViewModel : ViewModel() {
    var searchText: ObservableField<String> = ObservableField("")
    var placeIdInLocationInformation: ObservableField<Int> = ObservableField(1)
}