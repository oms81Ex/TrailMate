package com.dev_oms.trailmate.data

import android.location.Location

data class TrackingState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val startTime: Long = 0L,
    val elapsedTime: Long = 0L,
    val currentLocation: Location? = null,
    val distance: Float = 0f,
    val averageSpeed: Float = 0f,
    val currentSpeed: Float = 0f,
    val elevationGain: Float = 0f,
    val currentElevation: Float = 0f,
    val calories: Int = 0,
    val path: List<Location> = emptyList()
)

data class HikingRecord(
    val id: String,
    val title: String,
    val date: Long,
    val duration: Long,
    val distance: Float,
    val elevationGain: Float,
    val maxElevation: Float,
    val calories: Int,
    val rating: Float,
    val completed: Boolean,
    val photos: List<String> = emptyList(),
    val notes: String = "",
    val path: List<Location> = emptyList()
)

data class MonthlyStat(
    val month: String,
    val hikingCount: Int,
    val totalDistance: Float,
    val totalElevation: Float,
    val totalTime: Long,
    val totalCalories: Int
)

data class WeatherInfo(
    val temperature: Int,
    val condition: String,
    val emoji: String,
    val humidity: Int,
    val windSpeed: Float
)

data class LocationInfo(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Float
) 