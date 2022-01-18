package com.sju18001.petmanagement.controller.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.AlarmBroadcastReceiver
import com.sju18001.petmanagement.controller.SessionManager

class MyFirebaseMessagingService: FirebaseMessagingService() {
    private val TAG = "MyFirebaseMessagingService"
    override fun onNewToken(p0: String) {
        Log.d(TAG, "Refreshed token: $p0")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        // sendRegistrationToServer(p0)
        // TODO: 토큰을 서버에 업데이트
        SessionManager.saveFcmRegistrationToken(applicationContext, p0)
    }

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${p0.from}")

        // Check if message contains a data payload.
        if (p0.data.isNotEmpty()) {
            // TODO: 데이터 페이로드를 통해 post_id 등 전달 -> 알림 클릭 시 페이지 전환
            Log.d(TAG, "Message data payload: ${p0.data}")

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                // scheduleJob()
            } else {
                // Handle message within 10 seconds
                // handleNow()
            }
        }

        // Check if message contains a notification payload.
        p0.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it)
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    private fun sendNotification(notification: RemoteMessage.Notification?) {
        // 알림 채널
        val channel = NotificationChannel(AlarmBroadcastReceiver.CHANNEL_ID, AlarmBroadcastReceiver.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        channel.description = "반려동물의 일정 알림 기능입니다."

        // 채널 등록
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        // 알림 빌드
        val builder = NotificationCompat.Builder(applicationContext, AlarmBroadcastReceiver.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_pets_24)
            .setContentTitle(notification?.title)
            .setContentText(notification?.body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // 알림 띄우기
        with(NotificationManagerCompat.from(applicationContext)){
            notify(AlarmBroadcastReceiver.NOTIFICATION_ID, builder.build())
        }
    }
}