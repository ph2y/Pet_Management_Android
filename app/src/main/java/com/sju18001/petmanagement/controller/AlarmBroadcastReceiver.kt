package com.sju18001.petmanagement.controller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.SplashActivity
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dto.FetchPetReqDto
import com.sju18001.petmanagement.ui.myPet.petScheduleManager.PetScheduleNotification
import java.util.HashMap

class AlarmBroadcastReceiver : BroadcastReceiver() {
    companion object{
        const val CHANNEL_NAME = "일정 알림"
        const val CHANNEL_ID = "PET_SCHEDULE"
        const val NOTIFICATION_ID = 1000
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action) {
            Intent.ACTION_SEND -> {
                // 알림 채널
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
                channel.description = "반려동물의 일정 알림 기능입니다."

                // 채널 등록
                val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)

                // 액티비티 인텐트
                val notifyIntent = Intent(context, SplashActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val notifyPendingIntent = PendingIntent.getActivity(
                    context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
                )
                
                // 알림 빌드
                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_icon)
                    .setContentTitle(intent?.getStringExtra("title"))
                    .setContentText(intent?.getStringExtra("text"))
                    .setContentIntent(notifyPendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)

                // 알림 띄우기
                with(NotificationManagerCompat.from(context)){
                    notify(NOTIFICATION_ID, builder.build())
                }
            }
            "android.intent.action.BOOT_COMPLETED" -> {
                if(context != null){
                    /*
                     * 재부팅 시 알람이 모두 삭제되었으므로, 다시 등록합니다.
                     * 1. fetch pet -> petNameForId 생성
                     * 2. fetch pet schedule -> enabled 상태인 스케줄들 모두 등록
                     */

                    // Fetch pet
                    val fetchPetCall = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
                        .fetchPetReq(FetchPetReqDto( null , null))
                    ServerUtil.enqueueApiCall(fetchPetCall, {false}, context, { petResponse ->
                        // Set petNameForId
                        val petNameForId = HashMap<Long, String>()
                        petResponse.body()?.petList?.map {
                            petNameForId[it.id] = it.name
                        }

                        // Fetch pet schedule and Add the schedules to alarm manager
                        val fetchPetScheduleCall = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
                            .fetchPetScheduleReq(ServerUtil.getEmptyBody())
                        ServerUtil.enqueueApiCall(fetchPetScheduleCall, {false}, context, { petScheduleResponse ->
                            // ON인 것들에 대해 알림 설정
                            petScheduleResponse.body()?.petScheduleList?.map{
                                if(it.enabled){
                                    PetScheduleNotification.setAlarmManagerRepeating(
                                        context,
                                        it.id,
                                        it.time,
                                        Util.getPetNamesFromPetIdList(petNameForId, it.petIdList),
                                        it.memo
                                    )
                                }
                            }
                        }, {}, {})
                    },{},{})
                }
            }
        }
    }
}