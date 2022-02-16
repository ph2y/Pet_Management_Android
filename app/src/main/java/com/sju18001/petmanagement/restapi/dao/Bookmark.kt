package com.sju18001.petmanagement.restapi.dao

data class Bookmark(
    val id: Long,
    val author: Account,
    val place: Place,
    val name: String,
    val description: String,
    val folder: String
)
