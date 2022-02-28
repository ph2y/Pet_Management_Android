package com.sju18001.petmanagement.ui.map

import com.sju18001.petmanagement.restapi.dao.Place


data class PlaceCard(
    var place: Place,
    var distance: String,
    var isBookmarked: Boolean
)
