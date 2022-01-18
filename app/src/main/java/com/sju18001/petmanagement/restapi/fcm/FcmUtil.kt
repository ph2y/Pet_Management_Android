package com.sju18001.petmanagement.restapi.fcm

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.sju18001.petmanagement.restapi.dao.FcmMessage
import com.sju18001.petmanagement.restapi.dao.Notification
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FcmUtil {
    companion object {
        fun getFirebaseMessagingToken(callback: (String)->Unit) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FcmUtil", "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result

                // Log
                Log.d("FcmUtil", token)

                // Invoke callback
                callback.invoke(token)
            })
        }

        fun sendFcmMessage(notification: Notification) {
            getFirebaseMessagingToken { token ->
                val body = FcmMessage(token, notification)
                FcmRetrofitBuilder.api.sendNotification(body)
                    ?.enqueue(object: Callback<ResponseBody?> {
                        override fun onResponse(
                            call: Call<ResponseBody?>,
                            response: Response<ResponseBody?>
                        ) {}

                        override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {}
                    })
            }
        }
    }
}