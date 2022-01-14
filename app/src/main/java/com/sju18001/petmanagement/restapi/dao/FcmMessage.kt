package com.sju18001.petmanagement.restapi.dao

import com.google.firebase.messaging.RemoteMessage

data class FcmMessage(
    val to: String,
    val notification: Notification
)
