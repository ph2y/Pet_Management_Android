package com.sju18001.petmanagement.ui.community.follow

import android.graphics.Bitmap

data class FollowItem(
    var hasPhoto: Boolean,
    var photo: Bitmap?,
    var id: Long,
    var username: String,
    var nickname: String,
    var isFollowing: Boolean,
    var representativePetId: Long?
)