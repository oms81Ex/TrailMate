package com.dev_oms.trailmate.data.api

import android.util.Log
import com.dev_oms.trailmate.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ForestService {
    
    private val apiClient = ApiClient()
    private val tag = "ForestService"
    
    /**
     * 산림청 산정보 API 호출
     */
    suspend fun getMountainInfo(
        siNm: String? = null,       // 시/도명
        gunNm: String? = null,      // 시/군/구명
        mtNm: String? = null,       // 산명
        pageNo: Int = 1,
        numOfRows: Int = 10
    ): ApiResponse<List<Mountain>> = withContext(Dispatchers.IO) {
        
        try {
            // 1. 산림청 Forest Story API 시도 (XML 응답 처리)
            val parameters = buildMap {
                put("ServiceKey", ApiConfig.SERVICE_KEY_DECODED)
                put("pageNo", pageNo.toString())
                put("numOfRows", numOfRows.toString())
                mtNm?.let { put("mntnNm", it) }
            }
            
            val endpoint = "${ApiConfig.FOREST_SERVICE_BASE_URL}/getforeststoryservice"
            
            Log.d(tag, "=== Mountain Info API Call ===")
            Log.d(tag, "Endpoint: $endpoint") 
            Log.d(tag, "Parameters: $parameters")
            Log.d(tag, "Service Key (first 20 chars): ${parameters["ServiceKey"]?.take(20)}...")
            
            val response = apiClient.get(endpoint, parameters)
            
            Log.d(tag, "API Response Success: ${response.success}")
            
            if (response.success && response.data != null) {
                // XML 응답 파싱 시도
                val mountains = parseForestXmlResponse(response.data)
                
                if (mountains.isNotEmpty()) {
                    Log.d(tag, "Successfully parsed ${mountains.size} mountains from Forest API")
                    return@withContext ApiResponse(success = true, data = mountains)
                } else {
                    Log.w(tag, "Forest API returned no valid data")
                }
            } else {
                Log.e(tag, "Forest API failed: ${response.error?.message}")
            }
            
            // 2. API 실패 시 지역 기반 fallback 데이터 생성
            Log.d(tag, "Generating fallback mountain data based on location")
            val fallbackMountains = generateRegionalMountains(siNm, gunNm)
            
            if (fallbackMountains.isNotEmpty()) {
                Log.d(tag, "Generated ${fallbackMountains.size} fallback mountains")
                return@withContext ApiResponse(success = true, data = fallbackMountains)
            }
            
            // 3. 모든 시도 실패
            ApiResponse(success = false, error = com.dev_oms.trailmate.data.api.ApiError(
                code = "API_ACCESS_DENIED",
                message = "산림청 API 접근 권한 없음",
                details = "서비스 키 권한 확인 필요 - fallback 데이터 사용"
            ))
            
        } catch (e: Exception) {
            Log.e(tag, "Error fetching mountain info", e)
            
            // 예외 발생 시에도 fallback 데이터 제공
            val fallbackMountains = generateRegionalMountains(siNm, gunNm)
            if (fallbackMountains.isNotEmpty()) {
                Log.d(tag, "Exception fallback: Generated ${fallbackMountains.size} mountains")
                return@withContext ApiResponse(success = true, data = fallbackMountains)
            }
            
            ApiResponse(
                success = false,
                error = com.dev_oms.trailmate.data.api.ApiError(
                    code = "PARSE_ERROR",
                    message = "데이터 파싱 오류",
                    details = e.message
                )
            )
        }
    }
    
    /**
     * 위치 기반 근처 산 정보 조회
     */
    suspend fun getNearbyMountains(
        userLat: Double,
        userLon: Double,
        radiusKm: Double = 50.0,
        limit: Int = 10
    ): ApiResponse<List<Mountain>> = withContext(Dispatchers.IO) {
        
        // 위도/경도를 기반으로 근사적 행정구역 추정
        val estimatedRegion = estimateRegionFromCoordinates(userLat, userLon)
        
        val response = getMountainInfo(
            siNm = estimatedRegion.province,
            gunNm = estimatedRegion.city,
            numOfRows = limit * 2 // 더 많이 가져와서 거리 필터링
        )
        
        if (response.success && response.data != null) {
            // 거리 기반 필터링 및 정렬
            val nearbyMountains = response.data
                .filter { mountain ->
                    val distance = calculateDistance(
                        userLat, userLon,
                        mountain.location.coordinates.latitude,
                        mountain.location.coordinates.longitude
                    )
                    distance <= radiusKm
                }
                .sortedBy { mountain ->
                    calculateDistance(
                        userLat, userLon,
                        mountain.location.coordinates.latitude,
                        mountain.location.coordinates.longitude
                    )
                }
                .take(limit)
            
            ApiResponse(success = true, data = nearbyMountains)
        } else {
            response
        }
    }
    
    /**
     * 100대 명산 목록 조회
     */
    suspend fun getTop100Mountains(
        pageNo: Int = 1,
        numOfRows: Int = 100
    ): ApiResponse<List<Mountain>> = withContext(Dispatchers.IO) {
        
        try {
            // JSON 시도
            val jsonParameters = mapOf(
                "serviceKey" to ApiConfig.SERVICE_KEY_DECODED,
                "pageNo" to pageNo.toString(),
                "numOfRows" to numOfRows.toString(),
                "dataType" to "json"
            )
            
            val response = apiClient.get(
                "${ApiConfig.TOP100_MOUNTAIN_BASE_URL}/getTop100FamtListBasiInfoList",
                jsonParameters
            )
            
            if (response.success && response.data != null) {
                val responseData = response.data
                
                // XML 응답인지 확인
                if (responseData.startsWith("<OpenAPI_ServiceResponse>")) {
                    Log.w(tag, "Received XML response instead of JSON for Top100 API")
                    
                    // ACCESS_DENIED 체크
                    if (responseData.contains("SERVICE_ACCESS_DENIED_ERROR")) {
                        Log.e(tag, "Top100 API access denied - using fallback data")
                        return@withContext generateFallbackTop100Mountains()
                    }
                    
                    // XML 파싱 시도
                    val mountains = parseTop100XmlResponse(responseData)
                    return@withContext ApiResponse(success = true, data = mountains)
                } else {
                    // JSON 파싱
                    val mountains = parseTop100ApiResponse(responseData)
                    return@withContext ApiResponse(success = true, data = mountains)
                }
            } else {
                Log.w(tag, "Top100 API request failed, using fallback data")
                return@withContext generateFallbackTop100Mountains()
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error fetching top 100 mountains", e)
            Log.d(tag, "Using fallback Top100 mountains due to error")
            return@withContext generateFallbackTop100Mountains()
        }
    }
    
    /**
     * 산림청 API 응답 파싱
     */
    private fun parseForestApiResponse(jsonString: String): List<Mountain> {
        return try {
            val jsonObject = JSONObject(jsonString)
            val response = jsonObject.getJSONObject("response")
            val header = response.getJSONObject("header")
            
            if (header.getString("resultCode") != "00") {
                Log.w(tag, "API returned error: ${header.getString("resultMsg")}")
                return emptyList()
            }
            
            val body = response.optJSONObject("body")
            if (body == null) {
                Log.w(tag, "No body in API response")
                return emptyList()
            }
            
            val items = body.optJSONArray("items")
            if (items == null) {
                Log.w(tag, "No items in API response")
                return emptyList()
            }
            
            val mountains = mutableListOf<Mountain>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val mountain = parseForestItem(item)
                mountain?.let { mountains.add(it) }
            }
            
            mountains
        } catch (e: Exception) {
            Log.e(tag, "Error parsing forest API response", e)
            emptyList()
        }
    }
    
    /**
     * 100대 명산 API 응답 파싱
     */
    private fun parseTop100ApiResponse(jsonString: String): List<Mountain> {
        return try {
            val jsonObject = JSONObject(jsonString)
            val response = jsonObject.getJSONObject("response")
            val header = response.getJSONObject("header")
            
            if (header.getString("resultCode") != "00") {
                Log.w(tag, "Top100 API returned error: ${header.getString("resultMsg")}")
                return emptyList()
            }
            
            val body = response.optJSONObject("body")
            val items = body?.optJSONArray("items")
            
            if (items == null) {
                Log.w(tag, "No items in Top100 API response")
                return emptyList()
            }
            
            val mountains = mutableListOf<Mountain>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val mountain = parseTop100Item(item)
                mountain?.let { mountains.add(it) }
            }
            
            mountains
        } catch (e: Exception) {
            Log.e(tag, "Error parsing top100 API response", e)
            emptyList()
        }
    }
    
    private fun parseForestItem(item: JSONObject): Mountain? {
        return try {
            // 산림청 API 응답 필드명에 맞게 수정
            val mntnnm = item.optString("mntnnm").takeIf { it.isNotEmpty() } ?: return null  // 산명
            val mntninfohght = item.optString("mntninfohght").toIntOrNull() ?: 0  // 산정보 높이
            val mntninfopoflc = item.optString("mntninfopoflc", "")  // 산정보소재지
            val mntninfodscrt = item.optString("mntninfodscrt", "")  // 산정보개관
            
            // 좌표 정보는 산림청 API에서 직접 제공하지 않으므로 기본값 사용
            val lat = 37.5665 + (kotlin.random.Random.nextDouble() - 0.5) * 0.1  // 서울 주변 임시 좌표
            val lon = 126.9780 + (kotlin.random.Random.nextDouble() - 0.5) * 0.1
            
            // 주소에서 시도, 시군구 추출
            val addressParts = mntninfopoflc.split(" ")
            val province = addressParts.getOrNull(0) ?: ""
            val city = addressParts.getOrNull(1) ?: ""
            
            Mountain(
                id = generateMountainId(mntnnm, lon, lat),
                name = mntnnm,
                location = MountainLocation(
                    province = province,
                    city = city,
                    address = mntninfopoflc,
                    coordinates = Coordinates(lat, lon)
                ),
                elevation = mntninfohght,
                category = MountainCategory(
                    is100Mountain = false,  // 일반 산림청 API는 100대명산 여부를 별도로 표시하지 않음
                    difficulty = determineDifficulty(mntninfohght)
                ),
                description = mntninfodscrt,
                updatedAt = getCurrentTimestamp(),
                source = "FOREST_SERVICE"
            )
        } catch (e: Exception) {
            Log.e(tag, "Error parsing forest item", e)
            null
        }
    }
    
    private fun parseTop100Item(item: JSONObject): Mountain? {
        return try {
            val mtNm = item.optString("mtNm").takeIf { it.isNotEmpty() } ?: return null
            val ctpvNm = item.optString("ctpvNm", "")
            val sggNm = item.optString("sggNm", "")
            val lat = item.optString("lat").toDoubleOrNull() ?: 0.0
            val lot = item.optString("lot").toDoubleOrNull() ?: 0.0
            val elevation = item.optString("elevation").toIntOrNull() ?: 0
            val overview = item.optString("overview", "")
            
            Mountain(
                id = generateMountainId(mtNm, lot, lat),
                name = mtNm,
                location = MountainLocation(
                    province = ctpvNm,
                    city = sggNm,
                    address = "$ctpvNm $sggNm",
                    coordinates = Coordinates(lat, lot)
                ),
                elevation = elevation,
                category = MountainCategory(
                    is100Mountain = true,
                    difficulty = determineDifficulty(elevation)
                ),
                description = overview,
                updatedAt = getCurrentTimestamp(),
                source = "TOP100_MOUNTAIN"
            )
        } catch (e: Exception) {
            Log.e(tag, "Error parsing top100 item", e)
            null
        }
    }
    
    // 유틸리티 함수들
    private fun generateMountainId(name: String, lon: Double, lat: Double): String {
        return "${name}_${lat.toString().take(8)}_${lon.toString().take(8)}".replace(".", "_")
    }
    
    private fun determineDifficulty(elevation: Int): Difficulty {
        return when {
            elevation < 500 -> Difficulty.EASY
            elevation < 1000 -> Difficulty.MODERATE
            else -> Difficulty.HARD
        }
    }
    
    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
    }
    
    private fun estimateRegionFromCoordinates(lat: Double, lon: Double): RegionInfo {
        // 간단한 좌표 기반 지역 추정 (실제로는 Reverse Geocoding API 사용 권장)
        return when {
            lat in 37.4..37.7 && lon in 126.8..127.2 -> RegionInfo("서울특별시", "")
            lat in 37.2..37.5 && lon in 126.9..127.3 -> RegionInfo("경기도", "")
            lat in 36.3..36.5 && lon in 127.3..127.5 -> RegionInfo("대전광역시", "")
            lat in 35.8..36.0 && lon in 128.5..128.7 -> RegionInfo("대구광역시", "")
            lat in 35.1..35.2 && lon in 129.0..129.1 -> RegionInfo("부산광역시", "")
            else -> RegionInfo("", "")
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
    
    /**
     * 산림청 API XML 응답 파싱
     */
    private fun parseForestXmlResponse(xmlString: String): List<Mountain> {
        return try {
            Log.d(tag, "Parsing XML response: ${xmlString.take(500)}...")
            
            // XML에서 에러 체크
            if (xmlString.contains("SERVICE_ACCESS_DENIED_ERROR") || 
                xmlString.contains("SERVICE ERROR") ||
                xmlString.contains("returnReasonCode>20")) {
                Log.w(tag, "Forest API access denied - service key authorization needed")
                return emptyList()
            }
            
            // TODO: 실제 XML 파싱 구현 (현재는 서비스 접근 거부 상태)
            // 실제 산림청 API가 작동할 때를 위한 XML 파싱 로직은 나중에 구현
            
            emptyList()
        } catch (e: Exception) {
            Log.e(tag, "Error parsing XML response", e)
            emptyList()
        }
    }
    
    /**
     * 100대 명산 XML 응답 파싱
     */
    private fun parseTop100XmlResponse(xmlString: String): List<Mountain> {
        return try {
            Log.d(tag, "Parsing Top100 XML response")
            
            // XML에서 에러 체크
            if (xmlString.contains("SERVICE_ACCESS_DENIED_ERROR") || 
                xmlString.contains("SERVICE ERROR") ||
                xmlString.contains("returnReasonCode>20")) {
                Log.w(tag, "Top100 API access denied")
                return emptyList()
            }
            
            // TODO: 실제 XML 파싱 구현
            emptyList()
        } catch (e: Exception) {
            Log.e(tag, "Error parsing Top100 XML response", e)
            emptyList()
        }
    }
    
    /**
     * Fallback 100대 명산 데이터 생성
     */
    private fun generateFallbackTop100Mountains(): ApiResponse<List<Mountain>> {
        Log.d(tag, "Generating fallback Top100 mountains")
        
        val fallbackMountains = listOf(
            Mountain(
                id = "fallback_jirisan",
                name = "지리산",
                location = MountainLocation(
                    province = "전라남도",
                    city = "구례군",
                    address = "전라남도 구례군 마산면",
                    coordinates = Coordinates(35.3384, 127.7314)
                ),
                elevation = 1915,
                category = MountainCategory(
                    is100Mountain = true,
                    difficulty = Difficulty.HARD
                ),
                description = "한국의 명산 중 하나로 천왕봉이 최고봉입니다.",
                updatedAt = getCurrentTimestamp(),
                source = "FALLBACK_TOP100"
            ),
            Mountain(
                id = "fallback_seoraksan",
                name = "설악산",
                location = MountainLocation(
                    province = "강원도",
                    city = "속초시",
                    address = "강원도 속초시 설악동",
                    coordinates = Coordinates(38.1193, 128.4656)
                ),
                elevation = 1708,
                category = MountainCategory(
                    is100Mountain = true,
                    difficulty = Difficulty.HARD
                ),
                description = "대청봉을 주봉으로 하는 명산입니다.",
                updatedAt = getCurrentTimestamp(),
                source = "FALLBACK_TOP100"
            ),
            Mountain(
                id = "fallback_bukhansan",
                name = "북한산",
                location = MountainLocation(
                    province = "서울특별시",
                    city = "강북구",
                    address = "서울특별시 강북구 우이동",
                    coordinates = Coordinates(37.6587, 126.9772)
                ),
                elevation = 836,
                category = MountainCategory(
                    is100Mountain = true,
                    difficulty = Difficulty.MODERATE
                ),
                description = "서울 근교의 대표적인 명산입니다.",
                updatedAt = getCurrentTimestamp(),
                source = "FALLBACK_TOP100"
            ),
            Mountain(
                id = "fallback_hallasan",
                name = "한라산",
                location = MountainLocation(
                    province = "제주특별자치도",
                    city = "제주시",
                    address = "제주특별자치도 제주시",
                    coordinates = Coordinates(33.3617, 126.5292)
                ),
                elevation = 1947,
                category = MountainCategory(
                    is100Mountain = true,
                    difficulty = Difficulty.HARD
                ),
                description = "제주도의 영산이자 한국 최고봉입니다.",
                updatedAt = getCurrentTimestamp(),
                source = "FALLBACK_TOP100"
            ),
            Mountain(
                id = "fallback_naejangsan",
                name = "내장산",
                location = MountainLocation(
                    province = "전라북도",
                    city = "정읍시",
                    address = "전라북도 정읍시 내장동",
                    coordinates = Coordinates(35.4975, 126.8975)
                ),
                elevation = 763,
                category = MountainCategory(
                    is100Mountain = true,
                    difficulty = Difficulty.MODERATE
                ),
                description = "가을 단풍으로 유명한 명산입니다.",
                updatedAt = getCurrentTimestamp(),
                source = "FALLBACK_TOP100"
            )
        )
        
        return ApiResponse(success = true, data = fallbackMountains)
    }
    
    /**
     * 지역 기반 fallback 산 데이터 생성
     */
    private fun generateRegionalMountains(siNm: String?, gunNm: String?): List<Mountain> {
        return try {
            // 경기도 지역의 유명한 산들 (사용자 위치 기반)
            val gyeonggiMountains = listOf(
                Mountain(
                    id = "mountain_bukhansan",
                    name = "북한산",
                    location = MountainLocation(
                        province = "서울특별시",
                        city = "은평구",
                        address = "서울특별시 은평구 진관동",
                        coordinates = Coordinates(37.6658, 126.9780)
                    ),
                    elevation = 836,
                    category = MountainCategory(
                        is100Mountain = true,
                        difficulty = Difficulty.MODERATE
                    ),
                    description = "서울 근교 최고의 산행지로 인수봉, 백운대, 만경대의 3개 주봉으로 이루어져 있다.",
                    updatedAt = getCurrentTimestamp(),
                    source = "FALLBACK_DATA"
                ),
                Mountain(
                    id = "mountain_namsan",
                    name = "남산",
                    location = MountainLocation(
                        province = "서울특별시",
                        city = "중구",
                        address = "서울특별시 중구 예장동",
                        coordinates = Coordinates(37.5512, 126.9941)
                    ),
                    elevation = 262,
                    category = MountainCategory(
                        is100Mountain = false,
                        difficulty = Difficulty.EASY
                    ),
                    description = "서울의 중심에 위치한 시민들의 휴식처로 N서울타워가 있는 곳이다.",
                    updatedAt = getCurrentTimestamp(),
                    source = "FALLBACK_DATA"
                ),
                Mountain(
                    id = "mountain_gwanaksan",
                    name = "관악산",
                    location = MountainLocation(
                        province = "서울특별시",
                        city = "관악구",
                        address = "서울특별시 관악구 신림동",
                        coordinates = Coordinates(37.4526, 126.9614)
                    ),
                    elevation = 632,
                    category = MountainCategory(
                        is100Mountain = false,
                        difficulty = Difficulty.MODERATE
                    ),
                    description = "서울 남부의 대표적인 산으로 연주대에서의 전망이 뛰어나다.",
                    updatedAt = getCurrentTimestamp(),
                    source = "FALLBACK_DATA"
                ),
                Mountain(
                    id = "mountain_suraksan",
                    name = "수락산",
                    location = MountainLocation(
                        province = "경기도",
                        city = "남양주시",
                        address = "경기도 남양주시 별내면",
                        coordinates = Coordinates(37.6967, 127.0767)
                    ),
                    elevation = 638,
                    category = MountainCategory(
                        is100Mountain = false,
                        difficulty = Difficulty.MODERATE
                    ),
                    description = "경기도와 서울의 경계에 위치한 산으로 기암괴석과 폭포가 유명하다.",
                    updatedAt = getCurrentTimestamp(),
                    source = "FALLBACK_DATA"
                ),
                Mountain(
                    id = "mountain_dobongsan",
                    name = "도봉산",
                    location = MountainLocation(
                        province = "서울특별시",
                        city = "도봉구",
                        address = "서울특별시 도봉구 도봉동",
                        coordinates = Coordinates(37.6895, 127.0464)
                    ),
                    elevation = 740,
                    category = MountainCategory(
                        is100Mountain = false,
                        difficulty = Difficulty.MODERATE
                    ),
                    description = "북한산국립공원의 일부로 기암절벽과 단풍이 아름다운 산이다.",
                    updatedAt = getCurrentTimestamp(),
                    source = "FALLBACK_DATA"
                )
            )
            
            Log.d(tag, "Generated ${gyeonggiMountains.size} regional fallback mountains")
            gyeonggiMountains
            
        } catch (e: Exception) {
            Log.e(tag, "Error generating fallback mountains", e)
            emptyList()
        }
    }
    
    private data class RegionInfo(val province: String, val city: String)
} 