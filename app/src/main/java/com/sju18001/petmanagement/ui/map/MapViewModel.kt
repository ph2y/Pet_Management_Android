package com.sju18001.petmanagement.ui.map

import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sju18001.petmanagement.restapi.dao.Bookmark
import com.sju18001.petmanagement.restapi.kakaoapi.Place

class MapViewModel : ViewModel() {
    var searchText: ObservableField<String> = ObservableField("")
    var placeCard: ObservableField<PlaceCard> = ObservableField(
        PlaceCard(
            Place("0", "def", "def", "",
                "", "0", "def", "def",
                "0", "0", "", "0"),
            false
        )
    )

    var isBookmarkFetched: ObservableField<Boolean> = ObservableField(false)

    fun getIsBookmarked() = placeCard.get()!!.isBookmarked
    fun setIsBookmarked(flag: Boolean) {
        val place = placeCard.get()!!.place
        placeCard.set(PlaceCard(place, flag))
    }
}