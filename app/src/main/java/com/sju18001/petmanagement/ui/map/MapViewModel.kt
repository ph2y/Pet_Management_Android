package com.sju18001.petmanagement.ui.map

import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sju18001.petmanagement.restapi.dao.Place

class MapViewModel : ViewModel() {
    var searchText = MutableLiveData("")
    var placeCard = MutableLiveData(PlaceCard(
        Place(
            -1, "def", "def", 0.0, 0.0, "def",
            "010-0000-0000", 0.0, 0, "def", "def"
        ),
        "0", false
    ))

    var isBookmarkFetched = MutableLiveData(false)

    fun getIsBookmarked() = placeCard.value!!.isBookmarked
    fun setIsBookmarked(flag: Boolean) {
        val place = placeCard.value!!.place
        val distance = placeCard.value!!.distance
        placeCard.value = PlaceCard(place, distance, flag)
    }
}