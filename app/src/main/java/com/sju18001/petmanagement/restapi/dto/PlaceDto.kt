package com.sju18001.petmanagement.restapi.dto

import com.sju18001.petmanagement.restapi.dao.Place
import com.sju18001.petmanagement.restapi.global.DtoMetadata
import java.math.BigDecimal

data class FetchPlaceReqDto (
    val id: Long?,
    val keyword: String?,
    val currentLat: BigDecimal?,
    val currentLong: BigDecimal?,
    val range: BigDecimal?
)

data class FetchPlaceResDto(
    val _metadata: DtoMetadata,
    val placeList: List<Place>
)