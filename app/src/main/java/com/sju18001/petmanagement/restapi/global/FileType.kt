package com.sju18001.petmanagement.restapi.global

object FileType {
    const val GENERAL_FILE: String = "GENERAL_FILE"
    const val IMAGE_FILE: String = "IMAGE_FILE"
    const val VIDEO_FILE: String = "VIDEO_FILE"
    const val AUDIO_FILE: String = "AUDIO_FILE"

    const val FILE_SIZE_LIMIT_PHOTO: Long = (20 * 1000000).toLong()
    const val FILE_SIZE_LIMIT_VIDEO: Long = (100 * 1000000).toLong()
    const val FILE_SIZE_LIMIT_GENERAL: Long = (100 * 1000000).toLong()
}