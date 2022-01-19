package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class CreateUpdatePostViewModel(private val handle: SavedStateHandle) : ViewModel() {
    var isFetched = handle.get<Boolean>("isFetched")?: false
        set(value) {
            handle.set("isFetched", value)
            field = value
        }

    // for pet
    var petList = handle.get<MutableList<PetListItem>>("petList")?: mutableListOf()
        set(value) {
            handle.set("petList", value)
            field = value
        }
    var selectedPetId = handle.get<Long>("selectedPetId")
        set(value) {
            handle.set("selectedPetId", value)
            field = value
        }
    var selectedPetIndex = handle.get<Int>("selectedPetIndex")?: -1
        set(value) {
            handle.set("selectedPetIndex", value)
            field = value
        }

    // for location
    var isUsingLocation = handle.get<Boolean>("isUsingLocation")?: false // TODO: set default value to true
        set(value) {
            handle.set("isUsingLocation", value)
            field = value
        }

    // for media files
    var photoPathList = handle.get<MutableList<String>>("photoPathList")?: mutableListOf()
        set(value) {
            handle.set("photoPathList", value)
            field = value
        }
    var videoPathList = handle.get<MutableList<String>>("videoPathList")?: mutableListOf()
        set(value) {
            handle.set("videoPathList", value)
            field = value
        }
    var mediaList = handle.get<MutableList<MediaListItem>>("mediaList")?: mutableListOf()
        set(value) {
            handle.set("mediaList", value)
            field = value
        }

    // for general files
    var generalFilePathList = handle.get<MutableList<String>>("generalFilePathList")?: mutableListOf()
        set(value) {
            handle.set("generalFilePathList", value)
            field = value
        }
    var generalFileNameList = handle.get<MutableList<String>>("generalFileNameList")?: mutableListOf()
        set(value) {
            handle.set("generalFileNameList", value)
            field = value
        }

    // for disclosure
    var disclosure = handle.get<String>("disclosure")?: "PUBLIC"
        set(value) {
            handle.set("disclosure", value)
            field = value
        }

    // for hashtag
    var hashtagEditText = handle.get<String>("hashtagEditText")?: ""
        set(value) {
            handle.set("hashtagEditText", value)
            field = value
        }
    var hashtagList = handle.get<MutableList<String>>("hashtagList")?: mutableListOf()
        set(value) {
            handle.set("hashtagList", value)
            field = value
        }

    // for post EditText
    var postEditText = handle.get<String>("postEditText")?: ""
        set(value) {
            handle.set("postEditText", value)
            field = value
        }

    // for API
    var apiIsLoading = handle.get<Boolean>("apiIsLoading")?: false
        set(value) {
            handle.set("apiIsLoading", value)
        }

    // for update
    var updatedPostPhotoData = handle.get<Boolean>("updatedPostPhotoData")?: false
        set(value) {
            handle.set("updatedPostPhotoData", value)
            field = value
        }
    var updatedPostVideoData = handle.get<Boolean>("updatedPostVideoData")?: false
        set(value) {
            handle.set("updatedPostVideoData", value)
            field = value
        }
    var updatedPostGeneralFileData = handle.get<Boolean>("updatedPostGeneralFileData")?: false
        set(value) {
            handle.set("updatedPostGeneralFileData", value)
            field = value
        }
    var deletedPostPhotoData = handle.get<Boolean>("deletedPostPhotoData")?: false
        set(value) {
            handle.set("deletedPostPhotoData", value)
            field = value
        }
    var deletedPostVideoData = handle.get<Boolean>("deletedPostVideoData")?: false
        set(value) {
            handle.set("deletedPostVideoData", value)
            field = value
        }
    var deletedPostGeneralFileData = handle.get<Boolean>("deletedPostGeneralFileData")?: false
        set(value) {
            handle.set("deletedPostGeneralFileData", value)
            field = value
        }
    var postId = handle.get<Long>("postId")
        set(value) {
            handle.set("postId", value)
            field = value
        }
}