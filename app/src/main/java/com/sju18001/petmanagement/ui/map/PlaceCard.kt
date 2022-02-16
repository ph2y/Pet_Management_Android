package com.sju18001.petmanagement.ui.map

import com.sju18001.petmanagement.restapi.kakaoapi.Place

data class PlaceCard(
    var place: Place,
    var isBookmarked: Boolean
)
