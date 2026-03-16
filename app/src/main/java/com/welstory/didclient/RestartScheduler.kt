package com.welstory.didclient

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * 매일 지정 시간에 앱 재시작을 예약하는 스케줄러
 * 기본: 매일 새벽 3시
 */
object RestartScheduler {

    private const val TAG = "RestartScheduler"
    private const val REQUEST_CODE = 9001

    /**
     * 매일 재시작 알람 등록
     * @param hour 재시작 시각 (기본 3시)
     * @param minute 재시작 분 (기본 0분)
     */
    fun schedule(context: Context, hour: Int = 3, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AppRestartReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 오늘 지정 시각 계산
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 이미 지난 시각이면 내일로
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // 매일 반복 알람 등록
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        AppLogger.d(TAG, "앱 재시작 알람 등록: 매일 ${hour}시 ${minute}분 (다음 실행: ${calendar.time})")
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AppRestartReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        AppLogger.d(TAG, "앱 재시작 알람 취소")
    }
}
