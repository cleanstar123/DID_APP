package com.welstory.didclient

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var apiClient: ApiClient
    private val handler = Handler(Looper.getMainLooper())
    
    // 설정값들
    private var serverUrl = ""
    private var restaurantCode = ""
    private var displayId = ""
    private var refreshInterval = 60000L
    
    // 스케줄 관리
    private var currentScheduleId = ""
    private var currentTemplateUrl = ""
    private var isScreenOff = false
    
    companion object {
        private const val TAG = "MainActivity"
        private const val SETTINGS_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPreferences = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        loadSettings()
        initApiClient()
        
        // 전체 화면 설정
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        // WebView 설정
        webView = WebView(this)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        
        // 콘솔 로그 비활성화 (선택사항)
        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                // 콘솔 메시지를 무시하여 로그 출력 방지
                return true
            }
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "페이지 로드 완료: $url")
                // 페이지 로드 완료 후 자동 새로고침 스케줄
                scheduleRefresh()
            }
            
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e(TAG, "WebView 오류: $errorCode - $description")
                showToast("페이지 로드 오류: $description")
            }
        }
        
        // 터치 이벤트로 설정 접근 (화면 우상단 모서리 3번 탭)
        var tapCount = 0
        var lastTapTime = 0L
        
        webView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                val currentTime = System.currentTimeMillis()
                
                // 우상단 모서리 영역 체크 (100x100 픽셀)
                if (event.x > webView.width - 100 && event.y < 100) {
                    if (currentTime - lastTapTime < 1000) { // 1초 내 연속 탭
                        tapCount++
                    } else {
                        tapCount = 1
                    }
                    lastTapTime = currentTime
                    
                    if (tapCount >= 3) {
                        Log.d(TAG, "우상단 3번 탭 - 설정 화면 열기")
                        openSettings()
                        tapCount = 0
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
        
        setContentView(webView)
        
        // API로 스케줄 조회 시작
        loadScheduleFromApi()
    }
    
    private fun loadSettings() {
        serverUrl = sharedPreferences.getString(SettingsActivity.KEY_SERVER_URL, SettingsActivity.DEFAULT_SERVER_URL) ?: SettingsActivity.DEFAULT_SERVER_URL
        restaurantCode = sharedPreferences.getString(SettingsActivity.KEY_RESTAURANT_CODE, SettingsActivity.DEFAULT_RESTAURANT_CODE) ?: SettingsActivity.DEFAULT_RESTAURANT_CODE
        displayId = sharedPreferences.getString(SettingsActivity.KEY_DISPLAY_ID, SettingsActivity.DEFAULT_DISPLAY_ID) ?: SettingsActivity.DEFAULT_DISPLAY_ID
        refreshInterval = sharedPreferences.getInt(SettingsActivity.KEY_REFRESH_INTERVAL, SettingsActivity.DEFAULT_REFRESH_INTERVAL).toLong() * 1000
        
        Log.d(TAG, "설정 로드: serverUrl=$serverUrl, restaurantCode=$restaurantCode, displayId=$displayId, refreshInterval=${refreshInterval/1000}초")
    }
    
    private fun initApiClient() {
        apiClient = ApiClient(serverUrl, restaurantCode, displayId)
    }
    
    private fun loadScheduleFromApi() {
        Log.d(TAG, "API로 스케줄 조회 시작")
        
        try {
            // 첫 번째 호출이거나 강제 업데이트인 경우 빈 스케줄 ID 사용
            val requestScheduleId = if (currentScheduleId.isEmpty()) "" else currentScheduleId
            
            apiClient.getSchedule(requestScheduleId, object : ApiClient.ScheduleCallback {
                override fun onSuccess(response: ScheduleResponse) {
                    try {
                        handler.post {
                            Log.d(TAG, "API 성공: ${response.header.returnCode}")
                            handleScheduleResponse(response)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "응답 처리 오류", e)
                        handler.post {
                            showErrorPage("응답 처리 오류: ${e.message}")
                            scheduleRefresh()
                        }
                    }
                }
                
                override fun onError(error: String) {
                    try {
                        handler.post {
                            Log.e(TAG, "스케줄 조회 실패: $error")
                            showToast("연결 오류: $error")
                            showErrorPage(error)
                            scheduleRefresh()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "오류 처리 중 예외 발생", e)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "API 호출 준비 오류", e)
            showErrorPage("설정 오류: ${e.message}")
            scheduleRefresh()
        }
    }
    
    private fun handleScheduleResponse(response: ScheduleResponse) {
        Log.d(TAG, "스케줄 응답 처리: ${response.header.returnCode} - ${response.body.actionType}")
        
        if (response.header.returnCode != "0000") {
            Log.e(TAG, "API 오류: ${response.header.returnMessage}")
            showToast("API 오류: ${response.header.returnMessage}")
            scheduleRefresh()
            return
        }
        
        when (response.body.actionType) {
            "00" -> {
                // 변경 없음 - 현재 상태 유지하되 주기적으로 템플릿 새로고침
                Log.d(TAG, "스케줄 변경 없음 - 현재 상태 유지")
                
                // 만약 현재 템플릿이 로드되지 않았다면 로드
                if (currentTemplateUrl.isEmpty() && response.body.templateId.isNotEmpty()) {
                    Log.d(TAG, "현재 템플릿 없음 - 초기 로드: ${response.body.templateId}")
                    currentScheduleId = response.body.scheduleId
                    currentTemplateUrl = response.body.templateId
                    loadTemplate(response.body.templateId)
                } else if (currentTemplateUrl.isNotEmpty()) {
                    // 5분마다 템플릿 새로고침 (템플릿 수정사항 반영)
                    val lastRefreshTime = sharedPreferences.getLong("last_template_refresh", 0)
                    val currentTime = System.currentTimeMillis()
                    val fiveMinutes = 5 * 60 * 1000L // 5분
                    
                    if (currentTime - lastRefreshTime > fiveMinutes) {
                        Log.d(TAG, "템플릿 새로고침 (5분 경과)")
                        refreshTemplate()
                        sharedPreferences.edit().putLong("last_template_refresh", currentTime).apply()
                    }
                }
                
                scheduleRefresh()
            }
            "11" -> {
                // 신규 또는 업데이트 - 새 템플릿 로드
                val newScheduleId = response.body.scheduleId
                val newTemplateUrl = response.body.templateId
                
                Log.d(TAG, "새 스케줄: $newScheduleId, 템플릿: $newTemplateUrl")
                
                if (newTemplateUrl.isNotEmpty()) {
                    // 이전 스케줄 ID 저장
                    val beforeScheduleId = currentScheduleId
                    
                    // 새 스케줄 정보 업데이트
                    currentScheduleId = newScheduleId
                    currentTemplateUrl = newTemplateUrl
                    isScreenOff = false
                    
                    // 템플릿 로드
                    loadTemplate(newTemplateUrl)
                    
                    // 서버에 업데이트 알림
                    if (beforeScheduleId.isNotEmpty()) {
                        apiClient.updateSchedule(beforeScheduleId, newScheduleId)
                    }
                } else {
                    Log.w(TAG, "템플릿 URL이 비어있음")
                    scheduleRefresh()
                }
            }
            "99" -> {
                // 화면 OFF - 스케줄 없음 페이지 표시
                Log.d(TAG, "화면 OFF 요청 - 스케줄 없음 페이지 표시")
                currentScheduleId = "0"
                currentTemplateUrl = ""
                isScreenOff = true
                showNoSchedulePage()
                scheduleRefresh()
            }
            else -> {
                Log.w(TAG, "알 수 없는 actionType: ${response.body.actionType}")
                scheduleRefresh()
            }
        }
    }
    
    private fun loadTemplate(templateUrl: String) {
        Log.d(TAG, "템플릿 로드: $templateUrl")
        webView.loadUrl(templateUrl)
    }
    
    private fun refreshTemplate() {
        if (currentTemplateUrl.isNotEmpty()) {
            Log.d(TAG, "템플릿 새로고침: $currentTemplateUrl")
            // 캐시 무시하고 새로고침
            webView.clearCache(true)
            webView.reload()
        }
    }
    
    private fun showNoSchedulePage() {
        Log.d(TAG, "스케줄 없음 페이지 표시")
        val noScheduleHtml = """
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { 
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        font-family: Arial, sans-serif;
                        display: flex;
                        flex-direction: column;
                        justify-content: center;
                        align-items: center;
                        height: 100vh;
                        margin: 0;
                        text-align: center;
                    }
                    h1 { font-size: 48px; margin-bottom: 20px; }
                    p { font-size: 24px; margin: 10px 0; }
                    .status { 
                        background: rgba(0,0,0,0.3);
                        padding: 30px;
                        border-radius: 15px;
                        margin-top: 30px;
                        max-width: 600px;
                    }
                    .time {
                        font-size: 32px;
                        color: #ffeb3b;
                        margin: 20px 0;
                    }
                    .blink {
                        animation: blink 2s infinite;
                    }
                    @keyframes blink {
                        0%, 50% { opacity: 1; }
                        51%, 100% { opacity: 0.3; }
                    }
                    .settings-hint {
                        background: rgba(255,193,7,0.2);
                        border: 2px solid #ffc107;
                        padding: 15px;
                        border-radius: 10px;
                        margin-top: 20px;
                        font-size: 18px;
                    }
                </style>
                <script>
                    function updateTime() {
                        const now = new Date();
                        const timeElement = document.getElementById('time');
                        if (timeElement) {
                            timeElement.textContent = now.toLocaleString('ko-KR');
                        }
                    }
                    // 5초마다 업데이트로 변경 (로그 감소)
                    setInterval(updateTime, 5000);
                    window.onload = updateTime;
                </script>
            </head>
            <body>
                <h1>📺 WelStory DID Client</h1>
                <div class="time" id="time"></div>
                <div class="status">
                    <h2 class="blink">⏰ 현재 스케줄 없음</h2>
                    <p>서버: $serverUrl</p>
                    <p>레스토랑: $restaurantCode</p>
                    <p>디스플레이: $displayId</p>
                    <p>다음 확인: 60초 후</p>
                    <hr style="margin: 20px 0; border: 1px solid rgba(255,255,255,0.3);">
                    <div class="settings-hint">
                        <p>⚙️ <strong>설정 변경 방법</strong></p>
                        <p>화면 우상단 모서리를 3번 연속 터치하세요</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, noScheduleHtml, "text/html", "UTF-8", null)
    }
    
    private fun showErrorPage(error: String) {
        Log.d(TAG, "오류 페이지 표시: $error")
        val errorHtml = """
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { 
                        background: linear-gradient(45deg, #ff6b6b, #ee5a24);
                        color: white;
                        font-family: Arial, sans-serif;
                        display: flex;
                        flex-direction: column;
                        justify-content: center;
                        align-items: center;
                        height: 100vh;
                        margin: 0;
                        text-align: center;
                    }
                    h1 { font-size: 48px; margin-bottom: 20px; }
                    p { font-size: 20px; margin: 10px 0; }
                    .error { 
                        background: rgba(0,0,0,0.3);
                        padding: 30px;
                        border-radius: 15px;
                        margin-top: 30px;
                        max-width: 700px;
                    }
                    .settings-hint {
                        background: rgba(255,193,7,0.2);
                        border: 2px solid #ffc107;
                        padding: 15px;
                        border-radius: 10px;
                        margin-top: 20px;
                        font-size: 16sp;
                    }
                    .blink {
                        animation: blink 2s infinite;
                    }
                    @keyframes blink {
                        0%, 50% { opacity: 1; }
                        51%, 100% { opacity: 0.3; }
                    }
                </style>
            </head>
            <body>
                <h1>⚠️ 연결 오류</h1>
                <div class="error">
                    <p><strong>오류 내용:</strong></p>
                    <p>$error</p>
                    <hr style="margin: 20px 0; border: 1px solid rgba(255,255,255,0.3);">
                    <p>현재 설정:</p>
                    <p>서버: $serverUrl</p>
                    <p>레스토랑: $restaurantCode</p>
                    <p>디스플레이: $displayId</p>
                    <p class="blink">60초 후 재시도...</p>
                    <div class="settings-hint">
                        <p>⚙️ <strong>설정 변경 방법</strong></p>
                        <p>화면 우상단 모서리를 3번 연속 터치하세요</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
    }
    
    private fun showBlackScreen() {
        Log.d(TAG, "검은 화면 표시")
        val blackHtml = """
            <html>
            <head>
                <style>
                    body { 
                        background-color: black; 
                        margin: 0; 
                        padding: 0; 
                        width: 100vw; 
                        height: 100vh; 
                    }
                </style>
            </head>
            <body></body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, blackHtml, "text/html", "UTF-8", null)
    }
    
    private fun scheduleRefresh() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            loadScheduleFromApi()
        }, refreshInterval)
        
        Log.d(TAG, "다음 새로고침: ${refreshInterval/1000}초 후")
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 메뉴 키나 설정 키를 눌렀을 때 설정 화면으로 이동
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            openSettings()
            return true
        }
        
        // 뒤로가기 키를 길게 눌렀을 때 설정 화면으로 이동 (TV 리모컨 대응)
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.isLongPress == true) {
            openSettings()
            return true
        }
        
        return super.onKeyDown(keyCode, event)
    }
    
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivityForResult(intent, SETTINGS_REQUEST_CODE)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            // 설정이 변경되었으므로 다시 로드
            Log.d(TAG, "설정 변경됨 - 재시작")
            loadSettings()
            initApiClient()
            currentScheduleId = "" // 스케줄 ID 초기화하여 강제 업데이트
            loadScheduleFromApi()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // 전체 화면 유지
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }
}