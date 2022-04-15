package com.sju18001.petmanagement.ui.community.follow

import android.graphics.Bitmap

class FollowItem {
    // item elements
    private var mHasPhoto: Boolean? = null
    private var mPhoto: Bitmap? = null
    private var mId: Long? = null
    private var mUsername: String? = null
    private var mNickname: String? = null
    private var mIsFollowing: Boolean? = null
    private var mRepresentativePetId: Long? = null

    // set values for the item
    fun setValues(hasPhoto: Boolean, photo: Bitmap?, id: Long, username: String,
                         nickname: String, isFollowing: Boolean, representativePetId: Long?) {
        mHasPhoto = hasPhoto
        mPhoto = photo
        mId = id
        mUsername = username
        mNickname = nickname
        mIsFollowing = isFollowing
        mRepresentativePetId = representativePetId
    }

    // get values from the item
    fun getHasPhoto(): Boolean {
        return mHasPhoto!!
    }
    fun getPhoto(): Bitmap? {
        return mPhoto
    }
    fun getId(): Long {
        return mId!!
    }
    fun getUsername(): String {
        return mUsername!!
    }
    fun getNickname(): String {
        return mNickname!!
    }
    fun getIsFollowing(): Boolean {
        return mIsFollowing!!
    }
    fun getRepresentativePetId(): Long? {
        return mRepresentativePetId
    }
}