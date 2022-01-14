package com.sju18001.petmanagement.restapi.fcm

import com.sju18001.petmanagement.restapi.dao.FcmMessage
import retrofit2.http.POST
import retrofit2.http.Body

import okhttp3.ResponseBody
import retrofit2.Call


interface FcmApi {
    @POST("fcm/send")
    fun sendNotification(@Body fcmMessage: FcmMessage): Call<ResponseBody?>?
}