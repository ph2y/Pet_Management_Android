package com.sju18001.petmanagement.restapi.dao

data class Place(
    val id: Long,
    val name: String,
    val categoryCode: String,
    val latitude: Double,
    val longitude: Double,
    val description: String?,
    val phone: String,
    val averageRating: Double,
    val operationDay: String?,
    val operationHour: String?
)
