package com.sju18001.petmanagement.restapi.global

object FileType {
    // file types
    const val GENERAL_FILE: String = "GENERAL_FILE"
    const val IMAGE_FILE: String = "IMAGE_FILE"
    const val VIDEO_FILE: String = "VIDEO_FILE"

    // image types
    const val ORIGINAL_IMAGE: Int = 1
    const val GENERAL_IMAGE: Int = 2
    const val THUMBNAIL_IMAGE: Int = 3

    // file size limits
    const val FILE_SIZE_LIMIT_PHOTO: Long = (20 * 1000000).toLong()
    const val FILE_SIZE_LIMIT_VIDEO: Long = (100 * 1000000).toLong()
    const val FILE_SIZE_LIMIT_GENERAL: Long = (100 * 1000000).toLong()
}