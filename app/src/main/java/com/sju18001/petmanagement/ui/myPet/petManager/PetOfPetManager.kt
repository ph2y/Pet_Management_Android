package com.sju18001.petmanagement.ui.myPet.petManager

data class PetOfPetManager(
    val id: Long,
    val ownername: String,
    val name: String,
    val species: String,
    val breed: String,
    val birth: String?,
    val yearOnly: Boolean?,
    val gender: Boolean,
    val message: String?,
    val photoUrl: String?,
    // 기존 Pet에서 추가된 내용
    val isRepresentative: Boolean
)
