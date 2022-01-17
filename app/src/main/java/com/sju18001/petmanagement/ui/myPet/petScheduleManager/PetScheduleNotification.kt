package com.sju18001.petmanagement.ui.myPet.petScheduleManager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.*
import com.google.firebase.messaging.NotificationParams
import com.google.firebase.messaging.RemoteMessage
import com.sju18001.petmanagement.controller.AlarmBroadcastReceiver
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
import java.util.*
import java.util.concurrent.TimeUnit

class PetScheduleNotification {
    companion object{
        fun setAlarmManagerRepeating(context: Context, id: Long, time: String, petNames: String?, memo: String?) {
            // time format: 'hh:mm:ss'
            val hour = Integer.parseInt(time.substring(0, 2))
            val minute = Integer.parseInt(time.substring(3, 5))

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)

                // 이미 지났을 경우
                if(before(Calendar.getInstance())){
                    add(Calendar.DATE, 1)
                }
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_SEND
                // title, text를 전달
                putExtra("title", petNames)
                putExtra("text", memo)
            }
            val alarmIntent = PendingIntent.getBroadcast(
                context,
                id.toInt(), // id로 알람을 구분한다.
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )

            // 예약
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                alarmIntent
            )
        }

        fun cancelAlarmManagerRepeating(context: Context, id: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_DELETE
            }
            val alarmIntent = PendingIntent.getBroadcast(
                context,
                id.toInt(), // id로 알람을 구분한다.
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )

            // 취소
            if (alarmIntent != null && alarmManager != null) {
                alarmManager.cancel(alarmIntent)
            }
        }
    }
}