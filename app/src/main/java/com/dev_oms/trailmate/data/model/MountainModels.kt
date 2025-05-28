package com.dev_oms.trailmate.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Mountain(
    val id: String,
    val name: String,
    val nameEng: String? = null,
    val location: MountainLocation,
    val elevation: Int,
    val area: Float? = null,
    val category: MountainCategory,
    val description: String,
    val features: List<String> = emptyList(),
    val updatedAt: String,
    val source: String = "PUBLIC_API"
)

@Serializable
data class MountainLocation(
    val province: String,
    val city: String,
    val address: String,
    val coordinates: Coordinates
)

@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class MountainCategory(
    val is100Mountain: Boolean = false,
    val nationalPark: String? = null,
    val difficulty: Difficulty = Difficulty.MODERATE
)

enum class Difficulty(val displayName: String, val stars: String) {
    EASY("초급", "⭐"),
    MODERATE("중급", "⭐⭐⭐"),
    HARD("고급", "⭐⭐⭐⭐⭐")
}

@Serializable
data class Trail(
    val id: String,
    val mountainId: String,
    val name: String,
    val distance: Float,
    val estimatedTime: Int, // 분 단위
    val difficulty: Difficulty,
    val elevationGain: Int,
    val path: TrailPath,
    val facilities: TrailFacilities,
    val statistics: TrailStatistics
)

@Serializable
data class TrailPath(
    val startPoint: Coordinates,
    val endPoint: Coordinates,
    val waypoints: List<Coordinates> = emptyList(),
    val gpxData: String? = null
)

@Serializable
data class TrailFacilities(
    val parking: Boolean = false,
    val restroom: Boolean = false,
    val shelter: Boolean = false,
    val restaurant: Boolean = false
)

@Serializable
data class TrailStatistics(
    val completionRate: Float = 0f,
    val averageTime: Int = 0,
    val popularSeasons: List<String> = emptyList(),
    val rating: Float = 0f,
    val reviewCount: Int = 0
)

@Serializable
data class TrailRecommendation(
    val trail: Trail,
    val mountain: Mountain,
    val recommendationScore: Float,
    val reasons: RecommendationReasons,
    val realtime: RealtimeInfo,
    val displayInfo: DisplayInfo
)

@Serializable
data class RecommendationReasons(
    val distance: Float,
    val difficulty: Float,
    val weather: Float,
    val popularity: Float,
    val accessibility: Float
)

@Serializable
data class RealtimeInfo(
    val weatherSuitability: WeatherSuitability,
    val crowdLevel: CrowdLevel,
    val currentWeather: String
)

enum class WeatherSuitability(val displayName: String) {
    GOOD("좋음"),
    FAIR("보통"),
    POOR("나쁨")
}

enum class CrowdLevel(val displayName: String) {
    LOW("한적함"),
    MODERATE("보통"),
    HIGH("붐빔")
}

@Serializable
data class DisplayInfo(
    val subtitle: String,
    val duration: String,
    val rating: String
)

// API 응답 모델
@Serializable
data class ForestServiceResponse(
    val response: ForestResponse
)

@Serializable
data class ForestResponse(
    val header: ResponseHeader,
    val body: ForestBody? = null
)

@Serializable
data class ResponseHeader(
    val resultCode: String,
    val resultMsg: String
)

@Serializable
data class ForestBody(
    val items: List<ForestItem> = emptyList(),
    val numOfRows: Int = 0,
    val pageNo: Int = 0,
    val totalCount: Int = 0
)

@Serializable
data class ForestItem(
    val mtNm: String? = null,           // 산명
    val mtHg: String? = null,           // 산높이
    val mtAdd: String? = null,          // 소재지
    val mtDtl: String? = null,          // 상세설명
    val coordX: String? = null,         // X좌표
    val coordY: String? = null,         // Y좌표
    val mt100: String? = null           // 100대명산여부
)

// 100대명산 API 응답 모델
@Serializable
data class Top100MountainResponse(
    val response: Top100Response
)

@Serializable
data class Top100Response(
    val header: ResponseHeader,
    val body: Top100Body? = null
)

@Serializable
data class Top100Body(
    val items: List<Top100Item> = emptyList(),
    val numOfRows: Int = 0,
    val pageNo: Int = 0,
    val totalCount: Int = 0
)

@Serializable
data class Top100Item(
    val mtId: String? = null,           // 산 ID
    val mtNm: String? = null,           // 산명
    val ctpvNm: String? = null,         // 시도명
    val sggNm: String? = null,          // 시군구명
    val lat: String? = null,            // 위도
    val lot: String? = null,            // 경도
    val elevation: String? = null,      // 해발고도
    val overview: String? = null        // 개요
) 