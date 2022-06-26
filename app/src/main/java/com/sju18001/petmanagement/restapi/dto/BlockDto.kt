package com.sju18001.petmanagement.restapi.dto

import com.sju18001.petmanagement.restapi.global.DtoMetadata

data class CreateBlockReqDto (
    val id: Long
)

data class CreateBlockResDto (
    val _metadata: DtoMetadata
)