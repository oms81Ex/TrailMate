package com.dev_oms.trailmate.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationService(private val context: Context) : LocationListener {
    
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private var previousLocation: Location? = null
    private var totalDistance: Float = 0f
    private var elevationGain: Float = 0f
    private var startElevation: Float? = null
    
    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        _isTracking.value = true
        
        // GPS 제공자에서 위치 업데이트 요청
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            5000L, // 5초마다 업데이트
            5f,    // 5미터 이동 시 업데이트
            this
        )
        
        // 네트워크 제공자에서도 위치 업데이트 요청 (백업용)
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            5000L,
            5f,
            this
        )
    }
    
    fun stopLocationUpdates() {
        _isTracking.value = false
        locationManager.removeUpdates(this)
        resetTrackingData()
    }
    
    fun getCurrentLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }
    
    override fun onLocationChanged(location: Location) {
        _currentLocation.value = location
        
        previousLocation?.let { prev ->
            // 거리 계산
            val distance = prev.distanceTo(location)
            if (distance > 5f) { // 5미터 이상 움직였을 때만 추가
                totalDistance += distance
                
                // 고도 상승 계산
                if (startElevation == null) {
                    startElevation = prev.altitude.toFloat()
                }
                
                val elevationDiff = location.altitude.toFloat() - prev.altitude.toFloat()
                if (elevationDiff > 0) {
                    elevationGain += elevationDiff
                }
            }
        }
        
        previousLocation = location
    }
    
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
    
    override fun onProviderEnabled(provider: String) {}
    
    override fun onProviderDisabled(provider: String) {}
    
    fun getTotalDistance(): Float = totalDistance
    
    fun getElevationGain(): Float = elevationGain
    
    fun getCurrentElevation(): Float = _currentLocation.value?.altitude?.toFloat() ?: 0f
    
    private fun resetTrackingData() {
        previousLocation = null
        totalDistance = 0f
        elevationGain = 0f
        startElevation = null
    }
    
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
} 