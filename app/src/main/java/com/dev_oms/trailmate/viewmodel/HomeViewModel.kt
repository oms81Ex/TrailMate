package com.dev_oms.trailmate.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.BatteryManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dev_oms.trailmate.data.LocationInfo
import com.dev_oms.trailmate.data.WeatherInfo
import com.dev_oms.trailmate.data.model.TrailRecommendation
import com.dev_oms.trailmate.data.repository.MountainRepository
import com.dev_oms.trailmate.service.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val locationService = LocationService(application)
    private val mountainRepository = MountainRepository(application)
    private val tag = "HomeViewModel"
    
    // Location info
    private val _locationInfo = MutableStateFlow(
        LocationInfo(
            name = "위치 확인 중...",
            latitude = 0.0,
            longitude = 0.0,
            altitude = 0f
        )
    )
    val locationInfo: StateFlow<LocationInfo> = _locationInfo.asStateFlow()
    
    // Weather info
    private val _weatherInfo = MutableStateFlow(
        WeatherInfo(
            temperature = 0,
            condition = "확인 중",
            emoji = "🌤️",
            humidity = 0,
            windSpeed = 0f
        )
    )
    val weatherInfo: StateFlow<WeatherInfo> = _weatherInfo.asStateFlow()
    
    // Battery level
    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()
    
    // Current location
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    
    // Trail recommendations
    private val _trailRecommendations = MutableStateFlow<List<TrailRecommendation>>(emptyList())
    val trailRecommendations: StateFlow<List<TrailRecommendation>> = _trailRecommendations.asStateFlow()
    
    // Loading states
    private val _isLoadingRecommendations = MutableStateFlow(false)
    val isLoadingRecommendations: StateFlow<Boolean> = _isLoadingRecommendations.asStateFlow()
    
    private val _recommendationError = MutableStateFlow<String?>(null)
    val recommendationError: StateFlow<String?> = _recommendationError.asStateFlow()
    
    init {
        updateBatteryLevel()
        initializeLocationUpdates()
    }
    
    private fun initializeLocationUpdates() {
        viewModelScope.launch {
            locationService.currentLocation.collect { location ->
                location?.let { 
                    _currentLocation.value = it
                    updateLocationInfo(it)
                    updateWeatherForLocation(it)
                    loadTrailRecommendations(it.latitude, it.longitude)
                }
            }
        }
    }
    
    fun requestLocationUpdate() {
        Log.d(tag, "Requesting location update")
        viewModelScope.launch {
            val hasPermission = locationService.hasLocationPermission()
            if (hasPermission) {
                locationService.startLocationUpdates()
            } else {
                Log.w(tag, "Location permission not granted")
            }
        }
    }
    
    private fun updateLocationInfo(location: Location) {
        val locationName = determineLocationName(location.latitude, location.longitude)
        _locationInfo.value = LocationInfo(
            name = locationName,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude.toFloat() else 0f
        )
        Log.d(tag, "Location updated: $locationName (${location.latitude}, ${location.longitude})")
    }
    
    private fun updateWeatherForLocation(location: Location) {
        // 현재는 임시 날씨 정보, 실제로는 기상청 API 연동 가능
        val weatherInfo = generateWeatherInfo(location.latitude, location.longitude)
        _weatherInfo.value = weatherInfo
        Log.d(tag, "Weather updated: ${weatherInfo.condition} ${weatherInfo.temperature}°C")
    }
    
    private fun loadTrailRecommendations(latitude: Double, longitude: Double) {
        _isLoadingRecommendations.value = true
        _recommendationError.value = null
        
        viewModelScope.launch {
            try {
                Log.d(tag, "=== Loading Trail Recommendations ===")
                Log.d(tag, "User location: $latitude, $longitude")
                
                mountainRepository.getRecommendedTrails(
                    userLat = latitude,
                    userLon = longitude
                ).collect { response ->
                    _isLoadingRecommendations.value = false
                    
                    Log.d(tag, "Repository response received - Success: ${response.success}")
                    
                    if (response.success) {
                        val recommendations = response.data ?: emptyList()
                        _trailRecommendations.value = recommendations
                        Log.d(tag, "Loaded ${recommendations.size} trail recommendations")
                        
                        // 첫 번째 추천 등산로가 있으면 로케이션 이름 업데이트
                        if (recommendations.isNotEmpty()) {
                            val firstRecommendation = recommendations.first()
                            val updatedLocationInfo = _locationInfo.value.copy(
                                name = "${firstRecommendation.mountain.location.city} 근처"
                            )
                            _locationInfo.value = updatedLocationInfo
                            Log.d(tag, "Updated location name to: ${updatedLocationInfo.name}")
                        }
                    } else {
                        val errorMessage = response.error?.message ?: "추천 등산로 로딩 실패"
                        val errorCode = response.error?.code ?: "UNKNOWN"
                        val errorDetails = response.error?.details ?: ""
                        
                        Log.e(tag, "=== Trail Recommendation Error ===")
                        Log.e(tag, "Error Code: $errorCode")
                        Log.e(tag, "Error Message: $errorMessage")
                        Log.e(tag, "Error Details: $errorDetails")
                        
                        // 사용자 친화적인 에러 메시지
                        val userFriendlyMessage = when {
                            errorCode.contains("SERVICE_ACCESS_DENIED") -> 
                                "서버 점검 중입니다. 기본 추천 등산로를 표시합니다."
                            errorCode.contains("NETWORK") -> 
                                "네트워크 연결을 확인해주세요. 기본 추천 등산로를 표시합니다."
                            errorCode.contains("PARSE_ERROR") -> 
                                "데이터 처리 중 문제가 발생했습니다. 기본 추천 등산로를 표시합니다."
                            else -> 
                                "추천 등산로 로딩 중 문제가 발생했습니다. 기본 추천 등산로를 표시합니다."
                        }
                        
                        _recommendationError.value = userFriendlyMessage
                        Log.e(tag, "Failed to load recommendations: $errorMessage")
                        
                        // 오류 시 더미 데이터로 폴백
                        loadFallbackRecommendations(latitude, longitude)
                    }
                }
            } catch (e: Exception) {
                _isLoadingRecommendations.value = false
                val errorMessage = "추천 등산로 로딩 중 오류 발생: ${e.message}"
                
                Log.e(tag, "=== Exception in loadTrailRecommendations ===")
                Log.e(tag, "Exception Type: ${e.javaClass.simpleName}")
                Log.e(tag, "Exception Message: ${e.message}")
                Log.e(tag, "Stack trace:", e)
                
                _recommendationError.value = errorMessage
                
                // 예외 발생 시 더미 데이터로 폴백
                loadFallbackRecommendations(latitude, longitude)
            }
        }
    }
    
    private fun loadFallbackRecommendations(latitude: Double, longitude: Double) {
        Log.d(tag, "Loading fallback recommendations for location: $latitude, $longitude")
        
        // 위치 기반 fallback 추천 등산로
        val fallbackRecommendations = when {
            // 서울 지역
            latitude in 37.4..37.7 && longitude in 126.7..127.2 -> {
                listOf(
                    createFallbackRecommendation(
                        mountainName = "북한산",
                        trailName = "백운대 코스",
                        province = "서울특별시",
                        city = "은평구",
                        distance = 4.2f,
                        time = 180,
                        difficulty = com.dev_oms.trailmate.data.model.Difficulty.MODERATE,
                        elevation = 836,
                        coordinates = com.dev_oms.trailmate.data.model.Coordinates(37.6658, 126.9780)
                    ),
                    createFallbackRecommendation(
                        mountainName = "관악산",
                        trailName = "연주대 코스",
                        province = "서울특별시", 
                        city = "관악구",
                        distance = 3.8f,
                        time = 150,
                        difficulty = com.dev_oms.trailmate.data.model.Difficulty.MODERATE,
                        elevation = 632,
                        coordinates = com.dev_oms.trailmate.data.model.Coordinates(37.4526, 126.9614)
                    )
                )
            }
            // 경기도 지역  
            latitude in 37.0..37.6 && longitude in 126.6..127.8 -> {
                listOf(
                    createFallbackRecommendation(
                        mountainName = "광교산",
                        trailName = "광교저수지 코스",
                        province = "경기도",
                        city = "수원시",
                        distance = 5.1f,
                        time = 210,
                        difficulty = com.dev_oms.trailmate.data.model.Difficulty.MODERATE,
                        elevation = 582,
                        coordinates = com.dev_oms.trailmate.data.model.Coordinates(37.2869, 127.0131)
                    ),
                    createFallbackRecommendation(
                        mountainName = "수리산",
                        trailName = "태을봉 코스",
                        province = "경기도",
                        city = "군포시",
                        distance = 3.2f,
                        time = 120,
                        difficulty = com.dev_oms.trailmate.data.model.Difficulty.EASY,
                        elevation = 489,
                        coordinates = com.dev_oms.trailmate.data.model.Coordinates(37.3833, 126.9167)
                    )
                )
            }
            // 기타 지역 - 전국 유명 명산
            else -> {
                listOf(
                    createFallbackRecommendation(
                        mountainName = "지리산",
                        trailName = "천왕봉 코스",
                        province = "전라남도",
                        city = "구례군",
                        distance = 8.5f,
                        time = 360,
                        difficulty = com.dev_oms.trailmate.data.model.Difficulty.HARD,
                        elevation = 1915,
                        coordinates = com.dev_oms.trailmate.data.model.Coordinates(35.3384, 127.7314)
                    ),
                    createFallbackRecommendation(
                        mountainName = "설악산",
                        trailName = "대청봉 코스",
                        province = "강원도",
                        city = "속초시",
                        distance = 7.2f,
                        time = 300,
                        difficulty = com.dev_oms.trailmate.data.model.Difficulty.HARD,
                        elevation = 1708,
                        coordinates = com.dev_oms.trailmate.data.model.Coordinates(38.1193, 128.4656)
                    )
                )
            }
        }
        
        _trailRecommendations.value = fallbackRecommendations
        Log.d(tag, "Loaded ${fallbackRecommendations.size} fallback recommendations")
    }
    
    private fun createFallbackRecommendation(
        mountainName: String,
        trailName: String,
        province: String,
        city: String,
        distance: Float,
        time: Int,
        difficulty: com.dev_oms.trailmate.data.model.Difficulty,
        elevation: Int,
        coordinates: com.dev_oms.trailmate.data.model.Coordinates
    ): com.dev_oms.trailmate.data.model.TrailRecommendation {
        
        val mountain = com.dev_oms.trailmate.data.model.Mountain(
            id = "fallback_${mountainName.lowercase()}",
            name = mountainName,
            location = com.dev_oms.trailmate.data.model.MountainLocation(
                province = province,
                city = city,
                address = "$province $city",
                coordinates = coordinates
            ),
            elevation = elevation,
            category = com.dev_oms.trailmate.data.model.MountainCategory(
                is100Mountain = mountainName in listOf("지리산", "설악산", "북한산", "한라산"),
                difficulty = difficulty
            ),
            description = "${mountainName}은 ${province} ${city}에 위치한 아름다운 명산입니다.",
            updatedAt = java.time.LocalDateTime.now().toString(),
            source = "FALLBACK_RECOMMENDATION"
        )
        
        val trail = com.dev_oms.trailmate.data.model.Trail(
            id = "fallback_trail_${mountainName.lowercase()}",
            mountainId = mountain.id,
            name = trailName,
            distance = distance,
            estimatedTime = time,
            difficulty = difficulty,
            elevationGain = (elevation * 0.8).toInt(),
            path = com.dev_oms.trailmate.data.model.TrailPath(
                startPoint = coordinates,
                endPoint = coordinates
            ),
            facilities = com.dev_oms.trailmate.data.model.TrailFacilities(
                parking = true,
                restroom = true,
                shelter = false,
                restaurant = kotlin.random.Random.nextBoolean()
            ),
            statistics = com.dev_oms.trailmate.data.model.TrailStatistics(
                rating = 4.0f + kotlin.random.Random.nextFloat() * 0.8f,
                reviewCount = (500 + kotlin.random.Random.nextInt(1000)),
                completionRate = 0.85f + kotlin.random.Random.nextFloat() * 0.1f
            )
        )
        
        return com.dev_oms.trailmate.data.model.TrailRecommendation(
            trail = trail,
            mountain = mountain,
            recommendationScore = 75f + kotlin.random.Random.nextFloat() * 20f,
            reasons = com.dev_oms.trailmate.data.model.RecommendationReasons(
                distance = 80f,
                difficulty = 85f,
                weather = 75f,
                popularity = 90f,
                accessibility = 70f
            ),
            realtime = com.dev_oms.trailmate.data.model.RealtimeInfo(
                weatherSuitability = com.dev_oms.trailmate.data.model.WeatherSuitability.GOOD,
                crowdLevel = com.dev_oms.trailmate.data.model.CrowdLevel.MODERATE,
                currentWeather = "맑음"
            ),
            displayInfo = com.dev_oms.trailmate.data.model.DisplayInfo(
                subtitle = "난이도: ${difficulty.displayName} ${difficulty.stars}",
                duration = "거리: ${String.format("%.1f", distance)}km | 소요시간: ${time / 60}시간",
                rating = "⭐⭐⭐⭐ ${String.format("%.1f", trail.statistics.rating)} (${trail.statistics.reviewCount}개 리뷰)"
            )
        )
    }
    
    fun centerOnCurrentLocation() {
        Log.d(tag, "Centering on current location")
        _currentLocation.value?.let { location ->
            // 위치 정보 즉시 업데이트
            updateLocationInfo(location)
            Log.d(tag, "Current location: ${location.latitude}, ${location.longitude}")
        } ?: run {
            Log.w(tag, "No current location available")
            // 위치 정보가 없으면 새로 요청
            requestLocationUpdate()
        }
    }
    
    fun updateWeather() {
        Log.d(tag, "Manual weather update requested")
        _currentLocation.value?.let { location ->
            updateWeatherForLocation(location)
        }
    }
    
    fun refreshRecommendations() {
        Log.d(tag, "Refreshing trail recommendations")
        _currentLocation.value?.let { location ->
            loadTrailRecommendations(location.latitude, location.longitude)
        }
    }
    
    fun getTopRecommendation(): TrailRecommendation? {
        return _trailRecommendations.value.firstOrNull()
    }
    
    private fun updateBatteryLevel() {
        val context = getApplication<Application>()
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryIntent?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level != -1 && scale != -1) {
                val batteryPct = (level * 100) / scale
                _batteryLevel.value = batteryPct
            }
        }
    }
    
    private fun determineLocationName(latitude: Double, longitude: Double): String {
        return when {
            latitude in 37.4..37.7 && longitude in 126.8..127.2 -> {
                when {
                    latitude > 37.6 -> "북한산 근처"
                    latitude > 37.5 -> "남산 근처" 
                    else -> "서울 근처"
                }
            }
            latitude in 37.2..37.5 && longitude in 126.9..127.3 -> {
                when {
                    longitude > 127.1 -> "관악산 근처"
                    else -> "경기도 근처"
                }
            }
            latitude in 36.3..36.5 && longitude in 127.3..127.5 -> "대전 근처"
            latitude in 35.8..36.0 && longitude in 128.5..128.7 -> "대구 근처"
            latitude in 35.1..35.2 && longitude in 129.0..129.1 -> "부산 근처"
            else -> "등산로 탐색 중..."
        }
    }
    
    private fun generateWeatherInfo(latitude: Double, longitude: Double): WeatherInfo {
        // 간단한 날씨 정보 생성 (실제로는 기상청 API 연동)
        val conditions = listOf(
            WeatherInfo(
                temperature = (15 + kotlin.random.Random.nextInt(11)), // 15-25
                condition = "맑음",
                emoji = "☀️",
                humidity = (50 + kotlin.random.Random.nextInt(21)), // 50-70
                windSpeed = (1f + kotlin.random.Random.nextFloat() * 2f) // 1f-3f
            ),
            WeatherInfo(
                temperature = (10 + kotlin.random.Random.nextInt(11)), // 10-20
                condition = "흐림",
                emoji = "☁️",
                humidity = (60 + kotlin.random.Random.nextInt(21)), // 60-80
                windSpeed = (2f + kotlin.random.Random.nextFloat() * 2f) // 2f-4f
            ),
            WeatherInfo(
                temperature = (12 + kotlin.random.Random.nextInt(11)), // 12-22
                condition = "조금 흐림",
                emoji = "🌤️",
                humidity = (55 + kotlin.random.Random.nextInt(21)), // 55-75
                windSpeed = (1.5f + kotlin.random.Random.nextFloat() * 2f) // 1.5f-3.5f
            )
        )
        return conditions[kotlin.random.Random.nextInt(conditions.size)]
    }
    
    override fun onCleared() {
        super.onCleared()
        locationService.stopLocationUpdates()
    }
} 