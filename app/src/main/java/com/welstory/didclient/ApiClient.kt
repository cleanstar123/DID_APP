package com.welstory.didclient

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(
    private val serverUrl: String,
    private val restaurantCode: String,
    private val displayId: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    
    companion object {
        private const val TAG = "ApiClient"
    }
    
    interface ScheduleCallback {
        fun onSuccess(response: ScheduleResponse)
        fun onError(error: String)
    }
    
    /**
     * 스케줄 조회 API 호출
     */
    fun getSchedule(currentScheduleId: String, callback: ScheduleCallback) {
        try {
            val request = ScheduleRequest(scheduleId = currentScheduleId)
            val json = gson.toJson(request)
            val requestBody = json.toRequestBody(mediaType)
            
            val httpRequest = Request.Builder()
                .url("$serverUrl/api/v1/schedule/SelectSchedule")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("accessToken", restaurantCode)
                .addHeader("accessDispId", displayId)
                .build()
            
            Log.d(TAG, "API 요청: ${httpRequest.url}")
            Log.d(TAG, "요청 헤더: accessToken=$restaurantCode, accessDispId=$displayId")
            Log.d(TAG, "요청 본문: $json")
            
            client.newCall(httpRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "API 호출 실패", e)
                    val errorMessage = when {
                        e.message?.contains("failed to connect", ignoreCase = true) == true -> 
                            "서버에 연결할 수 없습니다. 서버 주소를 확인하세요."
                        e.message?.contains("timeout", ignoreCase = true) == true -> 
                            "서버 응답 시간이 초과되었습니다."
                        e.message?.contains("unknown host", ignoreCase = true) == true -> 
                            "서버 주소가 올바르지 않습니다."
                        else -> "네트워크 오류: ${e.message}"
                    }
                    callback.onError(errorMessage)
                }
                
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string()
                        Log.d(TAG, "API 응답: $responseBody")
                        
                        if (response.isSuccessful && responseBody != null) {
                            val scheduleResponse = gson.fromJson(responseBody, ScheduleResponse::class.java)
                            callback.onSuccess(scheduleResponse)
                        } else {
                            val errorMessage = when (response.code) {
                                404 -> "API 엔드포인트를 찾을 수 없습니다. 서버 주소를 확인하세요."
                                500 -> "서버 내부 오류가 발생했습니다."
                                else -> "서버 오류: ${response.code} ${response.message}"
                            }
                            callback.onError(errorMessage)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "응답 파싱 오류", e)
                        callback.onError("응답 파싱 오류: ${e.message}")
                    } finally {
                        response.close()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "API 요청 생성 오류", e)
            callback.onError("요청 생성 오류: ${e.message}")
        }
    }
    
    /**
     * 스케줄 업데이트 알림 API 호출
     */
    fun updateSchedule(beforeScheduleId: String, currentScheduleId: String) {
        val request = UpdateScheduleRequest(
            beforeScheduleId = beforeScheduleId,
            scheduleId = currentScheduleId
        )
        val json = gson.toJson(request)
        val requestBody = json.toRequestBody(mediaType)
        
        val httpRequest = Request.Builder()
            .url("$serverUrl/api/v1/schedule/UpdateSchedule")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("accessToken", restaurantCode)
            .addHeader("accessDispId", displayId)
            .build()
        
        Log.d(TAG, "업데이트 API 요청: ${httpRequest.url}")
        
        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "업데이트 API 호출 실패", e)
            }
            
            override fun onResponse(call: Call, response: Response) {
                Log.d(TAG, "업데이트 API 응답: ${response.code}")
                response.close()
            }
        })
    }
}