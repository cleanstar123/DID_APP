package com.welstory.didclient

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var etServerUrl: EditText
    private lateinit var etRestaurantCode: EditText
    private lateinit var etDisplayId: EditText
    private lateinit var etRestartHour: EditText
    private lateinit var etRestartMinute: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    companion object {
        const val PREFS_NAME = "DIDClientSettings"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_RESTAURANT_CODE = "restaurant_code"
        const val KEY_DISPLAY_ID = "display_id"
        const val KEY_RESTART_HOUR = "restart_hour"
        const val KEY_RESTART_MINUTE = "restart_minute"

        const val DEFAULT_SERVER_URL = "http://210.219.229.46:8082"
        const val DEFAULT_RESTAURANT_CODE = "RST001"
        const val DEFAULT_DISPLAY_ID = "1"
        const val DEFAULT_RESTART_HOUR = 3
        const val DEFAULT_RESTART_MINUTE = 0
        const val REFRESH_INTERVAL = 60_000L  // 60초 고정
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        initViews()
        loadSettings()
        setupClickListeners()
    }

    private fun initViews() {
        etServerUrl      = findViewById(R.id.etServerUrl)
        etRestaurantCode = findViewById(R.id.etRestaurantCode)
        etDisplayId      = findViewById(R.id.etDisplayId)
        etRestartHour    = findViewById(R.id.etRestartHour)
        etRestartMinute  = findViewById(R.id.etRestartMinute)
        btnSave          = findViewById(R.id.btnSave)
        btnCancel        = findViewById(R.id.btnCancel)
    }

    private fun loadSettings() {
        etServerUrl.setText(sharedPreferences.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL))
        etRestaurantCode.setText(sharedPreferences.getString(KEY_RESTAURANT_CODE, DEFAULT_RESTAURANT_CODE))
        etDisplayId.setText(sharedPreferences.getString(KEY_DISPLAY_ID, DEFAULT_DISPLAY_ID))

        val h = sharedPreferences.getInt(KEY_RESTART_HOUR, DEFAULT_RESTART_HOUR)
        val m = sharedPreferences.getInt(KEY_RESTART_MINUTE, DEFAULT_RESTART_MINUTE)
        etRestartHour.setText(if (h == -1) "" else h.toString())
        etRestartMinute.setText(if (h == -1) "" else m.toString())
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener { saveSettings() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun saveSettings() {
        val serverUrl      = etServerUrl.text.toString().trim()
        val restaurantCode = etRestaurantCode.text.toString().trim()
        val displayId      = etDisplayId.text.toString().trim()
        val hourStr        = etRestartHour.text.toString().trim()
        val minuteStr      = etRestartMinute.text.toString().trim()

        if (serverUrl.isEmpty())      { toast("서버 URL을 입력하세요"); return }
        if (restaurantCode.isEmpty()) { toast("레스토랑 코드를 입력하세요"); return }
        if (displayId.isEmpty())      { toast("디스플레이 ID를 입력하세요"); return }

        val restartHour: Int
        val restartMinute: Int
        if (hourStr.isEmpty()) {
            restartHour = -1
            restartMinute = 0
        } else {
            val h = hourStr.toIntOrNull()
                ?: run { toast("재시작 시간은 0~23 숫자로 입력하세요"); return }
            if (h < 0 || h > 23) { toast("재시작 시간은 0~23 사이여야 합니다"); return }
            val m = if (minuteStr.isEmpty()) 0
                    else minuteStr.toIntOrNull()
                        ?: run { toast("재시작 분은 0~59 숫자로 입력하세요"); return }
            if (m < 0 || m > 59) { toast("재시작 분은 0~59 사이여야 합니다"); return }
            restartHour = h
            restartMinute = m
        }

        with(sharedPreferences.edit()) {
            putString(KEY_SERVER_URL, serverUrl)
            putString(KEY_RESTAURANT_CODE, restaurantCode)
            putString(KEY_DISPLAY_ID, displayId)
            putInt(KEY_RESTART_HOUR, restartHour)
            putInt(KEY_RESTART_MINUTE, restartMinute)
            apply()
        }

        if (restartHour == -1) {
            RestartScheduler.cancel(this)
        } else {
            RestartScheduler.schedule(this, hour = restartHour, minute = restartMinute)
        }

        toast("설정이 저장되었습니다")
        setResult(RESULT_OK)
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
