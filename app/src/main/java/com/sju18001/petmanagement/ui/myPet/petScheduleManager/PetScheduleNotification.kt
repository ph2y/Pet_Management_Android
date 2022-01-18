package com.sju18001.petmanagement.ui.myPet.petScheduleManager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sju18001.petmanagement.controller.AlarmBroadcastReceiver
import com.sju18001.petmanagement.controller.SessionManager
import java.util.*

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
            val requestCode = id.toInt() // id로 알람을 구분한다.
            val alarmIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
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


            // requestCode를 따로 저장하여 관리한다.
            SessionManager.addRequestCodeOfAlarmManager(context, requestCode)
        }

        fun cancelAlarmManagerRepeating(context: Context, id: Long) {
            cancelAlarmManagerRepeating(context, id.toInt())
        }

        private fun cancelAlarmManagerRepeating(context: Context, requestCode: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_SEND
            }
            val alarmIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )

            // 취소
            if (alarmIntent != null && alarmManager != null) {
                alarmManager.cancel(alarmIntent)
            }


            // 저장되어있던 requestCode를 삭제한다.
            SessionManager.removeRequestCodeOfAlarmManager(context, requestCode)
        }

        /*
         * SessionManager에 저장된 request code들을 참조하여, 등록되어 있는
         * 알람들을 모두 cancel합니다.
         */
        fun cancelAll(context: Context) {
            for(requestCode in SessionManager.getRequestCodesOfAlarmManager(context)) {
                cancelAlarmManagerRepeating(context, requestCode)
            }
        }
    }
}