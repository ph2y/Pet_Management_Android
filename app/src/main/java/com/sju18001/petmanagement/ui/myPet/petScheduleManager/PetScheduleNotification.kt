package com.sju18001.petmanagement.ui.myPet.petScheduleManager

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.work.*
import com.google.firebase.messaging.NotificationParams
import com.google.firebase.messaging.RemoteMessage
import com.sju18001.petmanagement.restapi.dao.FcmMessage
import com.sju18001.petmanagement.restapi.dao.Notification
import com.sju18001.petmanagement.restapi.fcm.FcmRetrofitBuilder
import com.sju18001.petmanagement.restapi.fcm.FcmUtil
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class PetScheduleNotification {
    companion object{
        @RequiresApi(Build.VERSION_CODES.O)
        fun enqueueNotificationWorkManager(context: Context, time: String, memo: String?) {
            // Get time difference in Minutes
            // 가령, 현재 시간이 12시이고, time이 13시이면 minDiff는 60입니다.
            // 즉, minDiff분 뒤에 Notification을 시작합니다.
            val now = LocalTime.now()
            val parsedTime = LocalTime.parse(time)

            var minDiff = Duration.between(now, parsedTime).toMinutes()
            if(minDiff < 0){
                minDiff += 60 * 24
            }

            // Build WorkRequest
            val notificationWorkRequest: PeriodicWorkRequest =
                PeriodicWorkRequestBuilder<PetScheduleWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(minDiff, TimeUnit.MINUTES)
                    .setInputData(workDataOf("MEMO" to memo))
                    .build()

            // Enqueue the work
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                time,
                ExistingPeriodicWorkPolicy.KEEP,
                notificationWorkRequest
            )
        }

        fun cancelNotificationWorkManager(context:Context, time: String?) {
            time?.let{
                WorkManager.getInstance(context).cancelAllWorkByTag(it)
            }
        }

        fun cancelAllWorkManager(context:Context) {
            WorkManager.getInstance(context).cancelAllWork()
        }

        fun sendFcmMessage(notification: Notification) {
            FcmUtil.getFirebaseMessagingToken { token ->
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