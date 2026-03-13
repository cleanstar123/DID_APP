package com.welstory.didclient

import com.google.gson.annotations.SerializedName

// API 요청 모델
data class ScheduleRequest(
    @SerializedName("scheduleId")
    val scheduleId: String
)

// API 응답 모델
data class ScheduleResponse(
    @SerializedName("header")
    val header: ResponseHeader,
    @SerializedName("body")
    val body: ResponseBody
)

data class ResponseHeader(
    @SerializedName("returnCode")
    val returnCode: String,
    @SerializedName("returnMessage")
    val returnMessage: String
)

data class ResponseBody(
    @SerializedName("actionType")
    val actionType: String,
    @SerializedName("scheduleId")
    val scheduleId: String,
    @SerializedName("templateId")
    val templateId: String
)

// 업데이트 요청 모델
data class UpdateScheduleRequest(
    @SerializedName("BeforeScheduleId")
    val beforeScheduleId: String,
    @SerializedName("scheduleId")
    val scheduleId: String
)