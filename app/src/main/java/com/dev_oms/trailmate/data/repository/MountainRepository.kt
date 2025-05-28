package com.dev_oms.trailmate.data.repository

import android.content.Context
import android.util.Log
import com.dev_oms.trailmate.data.api.ApiResponse
import com.dev_oms.trailmate.data.api.ForestService
import com.dev_oms.trailmate.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.*

class MountainRepository(private val context: Context) {
    
    private val forestService = ForestService()
    private val tag = "MountainRepository"
    
    // 캐시된 데이터
    private var cachedMountains: List<Mountain> = emptyList()
    private var cachedTop100Mountains: List<Mountain> = emptyList()
    private var cacheTimestamp: Long = 0
    private val cacheValidDuration = 30 * 60 * 1000L // 30분
    
    /**
     * 위치 기반 추천 등산로 가져오기
     */
    fun getRecommendedTrails(
        userLat: Double,
        userLon: Double,
        userLevel: Difficulty = Difficulty.MODERATE
    ): Flow<ApiResponse<List<TrailRecommendation>>> = flow {
        
        try {
            Log.d(tag, "Getting recommended trails for location: $userLat, $userLon")
            
            // 1. 근처 산 정보 조회
            val nearbyMountainsResponse = forestService.getNearbyMountains(
                userLat = userLat,
                userLon = userLon,
                radiusKm = 50.0,
                limit = 10
            )
            
            if (!nearbyMountainsResponse.success) {
                emit(ApiResponse(success = false, error = nearbyMountainsResponse.error))
                return@flow
            }
            
            val nearbyMountains = nearbyMountainsResponse.data ?: emptyList()
            Log.d(tag, "Found ${nearbyMountains.size} nearby mountains")
            
            // 2. 100대 명산 정보와 결합
            val top100Response = getTop100Mountains()
            val top100Mountains = if (top100Response.success) {
                top100Response.data ?: emptyList()
            } else emptyList()
            
            // 3. 추천 등산로 생성
            val recommendations = generateTrailRecommendations(
                nearbyMountains = nearbyMountains,
                top100Mountains = top100Mountains,
                userLocation = Coordinates(userLat, userLon),
                userLevel = userLevel
            )
            
            Log.d(tag, "Generated ${recommendations.size} trail recommendations")
            emit(ApiResponse(success = true, data = recommendations))
            
        } catch (e: Exception) {
            Log.e(tag, "Error getting recommended trails", e)
            emit(ApiResponse(
                success = false,
                error = com.dev_oms.trailmate.data.api.ApiError(
                    code = "RECOMMENDATION_ERROR",
                    message = "추천 등산로 조회 오류",
                    details = e.message
                )
            ))
        }
        
    }.flowOn(Dispatchers.IO)
    
    /**
     * 산 이름으로 검색
     */
    suspend fun searchMountainsByName(name: String): ApiResponse<List<Mountain>> {
        return forestService.getMountainInfo(mtNm = name, numOfRows = 20)
    }
    
    /**
     * 100대 명산 목록 조회 (캐시 사용)
     */
    private suspend fun getTop100Mountains(): ApiResponse<List<Mountain>> {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            
            // 캐시 확인
            if (cachedTop100Mountains.isNotEmpty() && 
                currentTime - cacheTimestamp < cacheValidDuration) {
                Log.d(tag, "Using cached top 100 mountains")
                return@withContext ApiResponse(success = true, data = cachedTop100Mountains)
            }
            
            // API 호출
            val response = forestService.getTop100Mountains()
            if (response.success) {
                cachedTop100Mountains = response.data ?: emptyList()
                cacheTimestamp = currentTime
                Log.d(tag, "Cached ${cachedTop100Mountains.size} top 100 mountains")
            }
            
            response
        }
    }
    
    /**
     * 추천 등산로 생성 알고리즘
     */
    private fun generateTrailRecommendations(
        nearbyMountains: List<Mountain>,
        top100Mountains: List<Mountain>,
        userLocation: Coordinates,
        userLevel: Difficulty
    ): List<TrailRecommendation> {
        
        val allMountains = (nearbyMountains + top100Mountains).distinctBy { it.id }
        val recommendations = mutableListOf<TrailRecommendation>()
        
        for (mountain in allMountains) {
            val trail = generateTrailFromMountain(mountain)
            val recommendation = calculateRecommendation(
                mountain = mountain,
                trail = trail,
                userLocation = userLocation,
                userLevel = userLevel
            )
            
            if (recommendation.recommendationScore > 30.0f) { // 최소 점수 필터
                recommendations.add(recommendation)
            }
        }
        
        return recommendations
            .sortedByDescending { it.recommendationScore }
            .take(10) // 상위 10개만 반환
    }
    
    /**
     * 산 정보로부터 등산로 생성
     */
    private fun generateTrailFromMountain(mountain: Mountain): Trail {
        return Trail(
            id = "trail_${mountain.id}",
            mountainId = mountain.id,
            name = "${mountain.name} 주 등산로",
            distance = generateDistance(mountain.elevation),
            estimatedTime = generateEstimatedTime(mountain.elevation),
            difficulty = mountain.category.difficulty,
            elevationGain = (mountain.elevation * 0.8).toInt(),
            path = TrailPath(
                startPoint = mountain.location.coordinates,
                endPoint = mountain.location.coordinates
            ),
            facilities = generateFacilities(mountain.category.is100Mountain),
            statistics = generateStatistics(mountain.category.is100Mountain)
        )
    }
    
    /**
     * 추천 점수 계산
     */
    private fun calculateRecommendation(
        mountain: Mountain,
        trail: Trail,
        userLocation: Coordinates,
        userLevel: Difficulty
    ): TrailRecommendation {
        
        val distance = calculateDistance(
            userLocation.latitude, userLocation.longitude,
            mountain.location.coordinates.latitude, mountain.location.coordinates.longitude
        )
        
        // 점수 계산 (각 0-100점)
        val distanceScore = calculateDistanceScore(distance)
        val difficultyScore = calculateDifficultyScore(trail.difficulty, userLevel)
        val weatherScore = calculateWeatherScore()
        val popularityScore = calculatePopularityScore(mountain.category.is100Mountain)
        val accessibilityScore = calculateAccessibilityScore(mountain.location.province)
        
        // 가중 평균
        val totalScore = (
            distanceScore * 0.3f +
            difficultyScore * 0.25f +
            weatherScore * 0.15f +
            popularityScore * 0.2f +
            accessibilityScore * 0.1f
        )
        
        return TrailRecommendation(
            trail = trail,
            mountain = mountain,
            recommendationScore = totalScore,
            reasons = RecommendationReasons(
                distance = distanceScore,
                difficulty = difficultyScore,
                weather = weatherScore,
                popularity = popularityScore,
                accessibility = accessibilityScore
            ),
            realtime = RealtimeInfo(
                weatherSuitability = WeatherSuitability.GOOD,
                crowdLevel = CrowdLevel.MODERATE,
                currentWeather = "맑음"
            ),
            displayInfo = DisplayInfo(
                subtitle = "난이도: ${trail.difficulty.displayName} ${trail.difficulty.stars}",
                duration = "거리: ${String.format("%.1f", trail.distance)}km | 소요시간: ${trail.estimatedTime / 60}시간",
                rating = "⭐⭐⭐⭐ ${String.format("%.1f", trail.statistics.rating)} (${trail.statistics.reviewCount}개 리뷰)"
            )
        )
    }
    
    // 점수 계산 함수들
    private fun calculateDistanceScore(distanceKm: Double): Float {
        return when {
            distanceKm <= 10 -> 100f
            distanceKm <= 30 -> 80f
            distanceKm <= 50 -> 60f
            distanceKm <= 100 -> 40f
            else -> 20f
        }
    }
    
    private fun calculateDifficultyScore(trailDifficulty: Difficulty, userLevel: Difficulty): Float {
        return when {
            trailDifficulty == userLevel -> 100f
            Math.abs(trailDifficulty.ordinal - userLevel.ordinal) == 1 -> 80f
            else -> 50f
        }
    }
    
    private fun calculateWeatherScore(): Float {
        // 현재는 고정값, 실제로는 날씨 API 연동
        return 75f
    }
    
    private fun calculatePopularityScore(is100Mountain: Boolean): Float {
        return if (is100Mountain) 90f else 60f
    }
    
    private fun calculateAccessibilityScore(province: String): Float {
        return when (province) {
            "서울특별시" -> 100f
            "경기도" -> 90f
            "인천광역시" -> 85f
            else -> 70f
        }
    }
    
    // 데이터 생성 함수들
    private fun generateDistance(elevation: Int): Float {
        return when {
            elevation < 500 -> (2f..4f).let { (it.start + Math.random() * (it.endInclusive - it.start)).toFloat() }
            elevation < 1000 -> (3f..6f).let { (it.start + Math.random() * (it.endInclusive - it.start)).toFloat() }
            else -> (5f..10f).let { (it.start + Math.random() * (it.endInclusive - it.start)).toFloat() }
        }
    }
    
    private fun generateEstimatedTime(elevation: Int): Int {
        return when {
            elevation < 500 -> (90..150).let { (it.first + Math.random() * (it.last - it.first)).toInt() }
            elevation < 1000 -> (120..240).let { (it.first + Math.random() * (it.last - it.first)).toInt() }
            else -> (180..360).let { (it.first + Math.random() * (it.last - it.first)).toInt() }
        }
    }
    
    private fun generateFacilities(is100Mountain: Boolean): TrailFacilities {
        return TrailFacilities(
            parking = is100Mountain || kotlin.random.Random.nextBoolean(),
            restroom = is100Mountain || kotlin.random.Random.nextFloat() > 0.3f,
            shelter = is100Mountain && kotlin.random.Random.nextBoolean(),
            restaurant = kotlin.random.Random.nextFloat() > 0.6f
        )
    }
    
    private fun generateStatistics(is100Mountain: Boolean): TrailStatistics {
        return TrailStatistics(
            completionRate = if (is100Mountain) {
                (0.8f + kotlin.random.Random.nextFloat() * 0.15f)
            } else {
                (0.6f + kotlin.random.Random.nextFloat() * 0.25f)
            },
            averageTime = (120 + kotlin.random.Random.nextInt(180)),
            popularSeasons = listOf("봄", "가을"),
            rating = if (is100Mountain) {
                (4.0f + kotlin.random.Random.nextFloat() * 0.8f)
            } else {
                (3.5f + kotlin.random.Random.nextFloat() * 1.0f)
            },
            reviewCount = if (is100Mountain) {
                (500 + kotlin.random.Random.nextInt(1500))
            } else {
                (50 + kotlin.random.Random.nextInt(750))
            }
        )
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
} 