package com.dev_oms.trailmate.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dev_oms.trailmate.data.TrackingState
import com.dev_oms.trailmate.service.LocationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TrackingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val locationService = LocationService(application)
    
    private val _trackingState = MutableStateFlow(TrackingState())
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()
    
    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()
    
    private var timerJob: Job? = null
    private var startTime: Long = 0L
    private var pausedTime: Long = 0L
    
    init {
        // 위치 변경 감지
        viewModelScope.launch {
            locationService.currentLocation.collect { location ->
                updateTrackingWithLocation(location)
            }
        }
    }
    
    fun startTracking() {
        if (!locationService.hasLocationPermission()) {
            return
        }
        
        startTime = System.currentTimeMillis()
        _trackingState.value = _trackingState.value.copy(
            isTracking = true,
            isPaused = false,
            startTime = startTime
        )
        
        locationService.startLocationUpdates()
        startTimer()
    }
    
    fun pauseTracking() {
        pausedTime = System.currentTimeMillis()
        _trackingState.value = _trackingState.value.copy(isPaused = true)
        stopTimer()
    }
    
    fun resumeTracking() {
        val pauseDuration = System.currentTimeMillis() - pausedTime
        startTime += pauseDuration
        
        _trackingState.value = _trackingState.value.copy(isPaused = false)
        startTimer()
    }
    
    fun stopTracking() {
        locationService.stopLocationUpdates()
        stopTimer()
        
        _trackingState.value = TrackingState()
        _elapsedTime.value = 0L
    }
    
    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_trackingState.value.isTracking && !_trackingState.value.isPaused) {
                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - startTime
                _elapsedTime.value = elapsed
                
                _trackingState.value = _trackingState.value.copy(elapsedTime = elapsed)
                
                delay(1000) // 1초마다 업데이트
            }
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
    
    private fun updateTrackingWithLocation(location: Location?) {
        if (location == null || !_trackingState.value.isTracking) return
        
        val currentState = _trackingState.value
        val distance = locationService.getTotalDistance() / 1000f // km 단위
        val elevationGain = locationService.getElevationGain()
        val currentElevation = locationService.getCurrentElevation()
        
        // 평균 속도 계산 (km/h)
        val elapsedHours = currentState.elapsedTime / 3600000f
        val averageSpeed = if (elapsedHours > 0) distance / elapsedHours else 0f
        
        // 현재 속도 계산 (km/h)
        val currentSpeed = if (location.hasSpeed()) {
            location.speed * 3.6f // m/s to km/h
        } else 0f
        
        // 칼로리 계산 (대략적인 계산: 체중 70kg 기준)
        val calories = calculateCalories(distance, elevationGain, elapsedHours)
        
        _trackingState.value = currentState.copy(
            currentLocation = location,
            distance = distance,
            averageSpeed = averageSpeed,
            currentSpeed = currentSpeed,
            elevationGain = elevationGain,
            currentElevation = currentElevation,
            calories = calories,
            path = currentState.path + location
        )
    }
    
    private fun calculateCalories(distance: Float, elevationGain: Float, hours: Float): Int {
        // 간단한 칼로리 계산 공식 (등산)
        val baseCaloriesPerKm = 50 // 평지 기준
        val elevationBonus = elevationGain * 0.1f // 고도 보너스
        val totalCalories = (distance * baseCaloriesPerKm) + elevationBonus
        return totalCalories.roundToInt()
    }
    
    fun hasLocationPermission(): Boolean {
        return locationService.hasLocationPermission()
    }
    
    fun getCurrentLocation(): Location? {
        return locationService.getCurrentLocation()
    }
    
    fun formatElapsedTime(elapsedTimeMs: Long): String {
        val totalSeconds = elapsedTimeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
} 