package com.dev_oms.trailmate.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import android.util.Log

object ApiConfig {
    const val SERVICE_KEY_ENCODED = "4xuu5CiDRnUqx15NNFQekvjpPw3NogwcDEW%2BngHxuQsQnfMEVaPT0xJy5Ts%2BSLvo6UwCE74DrPO4RvBkNYd50g%3D%3D"
    const val SERVICE_KEY_DECODED = "4xuu5CiDRnUqx15NNFQekvjpPw3NogwcDEW+ngHxuQsQnfMEVaPT0xJy5Ts+SLvo6UwCE74DrPO4RvBkNYd50g=="
    
    // API Base URLs - 공공데이터포털에서 확인한 올바른 URL
    const val FOREST_SERVICE_BASE_URL = "http://openapi.forest.go.kr/openapi/service/trailInfoService"
    const val FOREST_CULTURE_BASE_URL = "https://apis.data.go.kr/1400000/service/cultureInfoService"  // 대안 API
    const val TOP100_MOUNTAIN_BASE_URL = "https://apis.data.go.kr/B553662/top100FamtListBasiInfoService"
    const val WEATHER_BASE_URL = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0"
    
    // Default parameters
    const val DEFAULT_PAGE_NO = 1
    const val DEFAULT_NUM_OF_ROWS = 10
    const val DATA_TYPE_JSON = "json"
    const val DATA_TYPE_XML = "xml"
}

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val resultCode: String = "00",
    val resultMsg: String = "NORMAL_SERVICE"
)

data class ApiError(
    val code: String,
    val message: String,
    val details: String? = null
)

enum class ApiErrorCode(val code: String, val message: String) {
    SUCCESS("00", "정상"),
    APPLICATION_ERROR("01", "어플리케이션 에러"),
    DB_ERROR("02", "데이터베이스 에러"),
    NO_DATA("03", "데이터없음 에러"),
    HTTP_ERROR("04", "HTTP 에러"),
    SERVICE_TIMEOUT("05", "서비스 연결실패 에러"),
    INVALID_REQUEST("10", "잘못된 요청 파라메터 에러"),
    NO_MANDATORY_PARAMS("11", "필수요청 파라메터가 없음"),
    NO_SERVICE_KEY("12", "서비스키가 없음"),
    ACCESS_DENIED("20", "서비스 접근거부"),
    REQUEST_LIMIT_EXCEEDED("22", "서비스 요청제한횟수 초과에러"),
    SERVICE_KEY_NOT_REGISTERED("30", "등록되지 않은 서비스키"),
    DEADLINE_EXPIRED("31", "기한만료된 서비스키")
}

class ApiClient {
    
    private val tag = "ApiClient"
    
    suspend fun get(
        url: String,
        parameters: Map<String, String> = emptyMap()
    ): ApiResponse<String> = withContext(Dispatchers.IO) {
        try {
            val fullUrl = buildUrl(url, parameters)
            Log.d(tag, "API Request: $fullUrl")
            Log.d(tag, "Parameters: $parameters")
            
            val connection = URL(fullUrl).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "PeakPal/1.0")
            }
            
            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage
            val responseBody = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            
            Log.d(tag, "API Response Code: $responseCode")
            Log.d(tag, "API Response Message: $responseMessage")
            Log.d(tag, "API Response Headers: ${connection.headerFields}")
            Log.d(tag, "API Response Body: ${responseBody.take(1000)}...")
            
            // HTTP 500 에러 특별 처리
            if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                Log.e(tag, "HTTP 500 Internal Server Error detected!")
                Log.e(tag, "Full error response: $responseBody")
                Log.e(tag, "Request URL: $fullUrl")
                Log.e(tag, "Request parameters: $parameters")
            }
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val apiError = parseApiError(responseBody)
                if (apiError != null) {
                    Log.w(tag, "API returned business logic error: ${apiError.code} - ${apiError.message}")
                    ApiResponse(success = false, error = apiError)
                } else {
                    ApiResponse(success = true, data = responseBody)
                }
            } else {
                val errorMessage = "HTTP Error: $responseCode $responseMessage"
                Log.e(tag, errorMessage)
                ApiResponse(
                    success = false,
                    error = ApiError(
                        code = responseCode.toString(),
                        message = errorMessage,
                        details = responseBody
                    )
                )
            }
            
        } catch (e: IOException) {
            Log.e(tag, "Network error", e)
            ApiResponse(
                success = false,
                error = ApiError(
                    code = "NETWORK_ERROR",
                    message = "네트워크 연결 오류",
                    details = e.message
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error", e)
            ApiResponse(
                success = false,
                error = ApiError(
                    code = "UNKNOWN_ERROR",
                    message = "알 수 없는 오류",
                    details = e.message
                )
            )
        }
    }
    
    private fun buildUrl(baseUrl: String, parameters: Map<String, String>): String {
        if (parameters.isEmpty()) return baseUrl
        
        val queryString = parameters.entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, "UTF-8")}"
        }
        
        return "$baseUrl?$queryString"
    }
    
    private fun parseApiError(responseBody: String): ApiError? {
        return try {
            val json = JSONObject(responseBody)
            val header = json.getJSONObject("response").getJSONObject("header")
            val resultCode = header.getString("resultCode")
            val resultMsg = header.getString("resultMsg")
            
            if (resultCode != "00") {
                ApiError(
                    code = resultCode,
                    message = resultMsg,
                    details = "API returned error code: $resultCode"
                )
            } else null
        } catch (e: Exception) {
            // JSON 파싱 실패 시 null 반환 (정상 응답으로 간주)
            null
        }
    }
} 