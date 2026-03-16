package com.welstory.didclient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

/**
 * AlarmManager에서 트리거하는 기기 재부팅 리시버
 * 시스템 앱으로 설치된 경우에만 REBOOT 권한 사용 가능
 */
class AppRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppLogger.d("AppRestartReceiver", "재부팅 알람 수신 - 기기 재부팅 시작")

        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.reboot(null)
        } catch (e: Exception) {
            // REBOOT 권한이 없는 경우 앱 프로세스 재시작으로 폴백
            AppLogger.e("AppRestartReceiver", "기기 재부팅 실패 - 앱 프로세스 재시작으로 대체", e)
            val restartIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(restartIntent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
