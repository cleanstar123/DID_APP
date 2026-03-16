package com.welstory.didclient

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 파일 + Logcat 동시 로깅 유틸
 * Windows 클라이언트의 LogFile.cs와 동일한 구조
 * 저장 위치: /Android/data/com.welstory.didclient/files/Log/Log_yyyyMMdd.dat
 */
object AppLogger {

    private const val TAG = "AppLogger"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    private var logDir: File? = null
    var isLogEnabled = true

    /** Application 또는 MainActivity.onCreate() 에서 한 번 호출 */
    fun init(context: Context) {
        logDir = File(context.getExternalFilesDir(null), "Log").also {
            if (!it.exists()) it.mkdirs()
        }
        Log.d(TAG, "로그 디렉토리: ${logDir?.absolutePath}")
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        write("DEBUG", tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        write("INFO", tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        write("WARN", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
        val fullMessage = if (throwable != null) "$message | ${throwable.stackTraceToString()}" else message
        write("ERROR", tag, fullMessage)
    }

    private fun write(level: String, tag: String, message: String) {
        if (!isLogEnabled) return
        val dir = logDir ?: return

        try {
            val logFile = File(dir, "Log_${fileNameFormat.format(Date())}.dat")
            FileWriter(logFile, true).use { writer ->
                // Windows LogFile.cs 와 동일한 포맷
                writer.appendLine("${dateFormat.format(Date())} - [Level: $level] - [Tag: $tag] - [Message: $message]")
            }
        } catch (e: Exception) {
            Log.e(TAG, "로그 파일 쓰기 실패: ${e.message}")
        }
    }

    /** 30일 이상 된 로그 파일 자동 삭제 */
    fun cleanOldLogs(keepDays: Int = 30) {
        val dir = logDir ?: return
        val cutoff = System.currentTimeMillis() - keepDays * 24 * 60 * 60 * 1000L
        dir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
    }
}
