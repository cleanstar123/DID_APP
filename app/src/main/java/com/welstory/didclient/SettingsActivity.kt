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
    private lateinit var etRefreshInterval: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    
    companion object {
        const val PREFS_NAME = "DIDClientSettings"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_RESTAURANT_CODE = "restaurant_code"
        const val KEY_DISPLAY_ID = "display_id"
        const val KEY_REFRESH_INTERVAL = "refresh_interval"
        
        // 기본값
        const val DEFAULT_SERVER_URL = "http://210.219.229.46:8081"
        const val DEFAULT_RESTAURANT_CODE = "RST001"
        const val DEFAULT_DISPLAY_ID = "1"
        const val DEFAULT_REFRESH_INTERVAL = 60
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
        etServerUrl = findViewById(R.id.etServerUrl)
        etRestaurantCode = findViewById(R.id.etRestaurantCode)
        etDisplayId = findViewById(R.id.etDisplayId)
        etRefreshInterval = findViewById(R.id.etRefreshInterval)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
    }
    
    private fun loadSettings() {
        etServerUrl.setText(sharedPreferences.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL))
        etRestaurantCode.setText(sharedPreferences.getString(KEY_RESTAURANT_CODE, DEFAULT_RESTAURANT_CODE))
        etDisplayId.setText(sharedPreferences.getString(KEY_DISPLAY_ID, DEFAULT_DISPLAY_ID))
        etRefreshInterval.setText(sharedPreferences.getInt(KEY_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL).toString())
    }
    
    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            saveSettings()
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun saveSettings() {
        val serverUrl = etServerUrl.text.toString().trim()
        val restaurantCode = etRestaurantCode.text.toString().trim()
        val displayId = etDisplayId.text.toString().trim()
        val refreshIntervalStr = etRefreshInterval.text.toString().trim()
        
        // 유효성 검사
        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "서버 URL을 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (restaurantCode.isEmpty()) {
            Toast.makeText(this, "레스토랑 코드를 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (displayId.isEmpty()) {
            Toast.makeText(this, "디스플레이 ID를 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        val refreshInterval = try {
            refreshIntervalStr.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "새로고침 간격은 숫자로 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (refreshInterval < 10) {
            Toast.makeText(this, "새로고침 간격은 10초 이상이어야 합니다", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 설정 저장
        with(sharedPreferences.edit()) {
            putString(KEY_SERVER_URL, serverUrl)
            putString(KEY_RESTAURANT_CODE, restaurantCode)
            putString(KEY_DISPLAY_ID, displayId)
            putInt(KEY_REFRESH_INTERVAL, refreshInterval)
            apply()
        }
        
        Toast.makeText(this, "설정이 저장되었습니다", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }
}