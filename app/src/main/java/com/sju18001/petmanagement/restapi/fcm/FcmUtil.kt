package com.sju18001.petmanagement.restapi.fcm

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging

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

        fun deleteFirebaseMessagingToken() {
            FirebaseMessaging.getInstance().deleteToken()
        }
    }
}