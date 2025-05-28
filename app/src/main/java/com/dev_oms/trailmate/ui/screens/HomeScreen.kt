package com.dev_oms.trailmate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import com.dev_oms.trailmate.ui.components.LocationPermissionHandler
import com.dev_oms.trailmate.ui.components.KakaoMapView
import com.dev_oms.trailmate.viewmodel.HomeViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTracking: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val locationInfo by homeViewModel.locationInfo.collectAsState()
    val weatherInfo by homeViewModel.weatherInfo.collectAsState()
    val batteryLevel by homeViewModel.batteryLevel.collectAsState()
    val currentLocation by homeViewModel.currentLocation.collectAsState()
    val trailRecommendations by homeViewModel.trailRecommendations.collectAsState()
    val isLoadingRecommendations by homeViewModel.isLoadingRecommendations.collectAsState()
    val recommendationError by homeViewModel.recommendationError.collectAsState()
    
    LocationPermissionHandler(
        onPermissionGranted = {
            homeViewModel.requestLocationUpdate()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "위치",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = locationInfo.name,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${weatherInfo.emoji} ${weatherInfo.condition} ${weatherInfo.temperature}°C",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🔋 ${batteryLevel}%",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Map Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // 카카오 지도
                var kakaoMapInstance by remember { mutableStateOf<com.kakao.vectormap.KakaoMap?>(null) }
                var isMapLoading by remember { mutableStateOf(true) }
                var mapLoadingError by remember { mutableStateOf<String?>(null) }
                var cacheStatus by remember { mutableStateOf("unknown") }
                var cachedTileCount by remember { mutableStateOf(0) }
                var loadingMessage by remember { mutableStateOf("지도 초기화 중...") }
                var mapStartTime by remember { mutableStateOf(0L) }
                
                // 캐시 상태 확인 함수
                suspend fun checkMapCacheStatus(): String {
                    return withContext(Dispatchers.IO) {
                        try {
                            val sharedPrefs = context.getSharedPreferences("trailmate_prefs", Context.MODE_PRIVATE)
                            val isInitialDownloadComplete = sharedPrefs.getBoolean("initial_download_complete", false)
                            
                            val cacheDir = File(context.cacheDir, "kakaomap")
                            val cacheFiles = cacheDir.listFiles()
                            cachedTileCount = cacheFiles?.size ?: 0
                            
                            when {
                                isInitialDownloadComplete && cachedTileCount > 0 -> "cached_available"
                                cachedTileCount > 0 -> "partial_cache"
                                else -> "no_cache"
                            }
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "캐시 상태 확인 실패: ${e.message}")
                            "error"
                        }
                    }
                }
                
                // 지도 로딩 시작 시 캐시 확인 및 타임아웃 설정
                LaunchedEffect(isMapLoading) {
                    if (isMapLoading && mapStartTime == 0L) {
                        mapStartTime = System.currentTimeMillis()
                        
                        // 캐시 상태 확인
                        cacheStatus = checkMapCacheStatus()
                        loadingMessage = when (cacheStatus) {
                            "cached_available" -> "캐시된 지도 로딩 중... (빠른 로딩 예상)"
                            "partial_cache" -> "지도 로딩 중... (일부 캐시 활용)"
                            "no_cache" -> "지도 다운로드 중... (첫 로딩, 시간 소요)"
                            else -> "지도 로딩 중..."
                        }
                        
                        Log.d("HomeScreen", "지도 로딩 시작 - 캐시 상태: $cacheStatus, 캐시 파일: ${cachedTileCount}개")
                        
                        // 캐시 상태에 따른 타임아웃 조정
                        val timeoutMs = when (cacheStatus) {
                            "cached_available" -> 15000L  // 15초 (캐시 있음)
                            "partial_cache" -> 45000L     // 45초 (부분 캐시)
                            "no_cache" -> 120000L         // 120초 (캐시 없음)
                            else -> 60000L                // 60초 (기본)
                        }
                        
                        kotlinx.coroutines.delay(timeoutMs)
                        if (isMapLoading) {
                            val loadTime = (System.currentTimeMillis() - mapStartTime) / 1000
                            Log.e("HomeScreen", "지도 로딩 타임아웃 - 로딩 시간: ${loadTime}초")
                            
                            val errorMessage = when (cacheStatus) {
                                "cached_available" -> "캐시가 있음에도 로딩에 실패했습니다.\n카카오 개발자 콘솔에서 키 해시 등록을 확인해주세요."
                                "partial_cache" -> "부분 캐시 상태에서 로딩에 실패했습니다.\n네트워크 연결을 확인해주세요."
                                "no_cache" -> "첫 실행이므로 네트워크를 통한 지도 다운로드가 필요합니다.\n인터넷 연결을 확인해주세요."
                                else -> "지도 로딩에 실패했습니다."
                            }
                            
                            mapLoadingError = errorMessage
                            isMapLoading = false
                        }
                    }
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    KakaoMapView(
                        modifier = Modifier.fillMaxSize(),
                        latitude = if (locationInfo.latitude != 0.0) locationInfo.latitude else 37.5665,
                        longitude = if (locationInfo.longitude != 0.0) locationInfo.longitude else 126.9780,
                        zoomLevel = 15,
                        onMapReady = { kakaoMap ->
                            kakaoMapInstance = kakaoMap
                            val loadEndTime = System.currentTimeMillis()
                            val loadTime = (loadEndTime - mapStartTime) / 1000
                            isMapLoading = false
                            
                            Log.d("HomeScreen", "✅ Kakao map ready and loading complete")
                            Log.d("HomeScreen", "Map position: ${locationInfo.latitude}, ${locationInfo.longitude}")
                            Log.d("HomeScreen", "⏱️ Map loading time: ${loadTime}초")
                            Log.d("HomeScreen", "📊 Cache status: $cacheStatus, cached files: ${cachedTileCount}개")
                            
                            val performanceMessage = when {
                                loadTime <= 5 && cacheStatus == "cached_available" -> "⚡ 캐시 활용으로 매우 빠른 로딩!"
                                loadTime <= 10 && cacheStatus == "partial_cache" -> "🔄 부분 캐시 활용으로 빠른 로딩"
                                loadTime >= 30 && cacheStatus == "no_cache" -> "🌐 첫 다운로드로 느린 로딩 (다음부터 빨라짐)"
                                else -> "✅ 정상 로딩 완료"
                            }
                            Log.d("HomeScreen", performanceMessage)
                        },
                        onZoomIn = { 
                            com.dev_oms.trailmate.ui.components.zoomIn(kakaoMapInstance) 
                        },
                        onZoomOut = { 
                            com.dev_oms.trailmate.ui.components.zoomOut(kakaoMapInstance) 
                        }
                    )
                    
                    // 지도 로딩 중 표시
                    if (isMapLoading) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = loadingMessage,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // 캐시 상태에 따른 정보 표시
                                when (cacheStatus) {
                                    "cached_available" -> {
                                        Text(
                                            text = "⚡ 캐시된 지도 활용 중 (${cachedTileCount}개 파일)",
                                            color = Color.Green.copy(alpha = 0.9f),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    "partial_cache" -> {
                                        Text(
                                            text = "🔄 부분 캐시 활용 중 (${cachedTileCount}개 파일)\n네트워크에서 추가 다운로드 중",
                                            color = Color.Yellow.copy(alpha = 0.9f),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    "no_cache" -> {
                                        Text(
                                            text = "🌐 첫 실행으로 네트워크 다운로드 중\n완료 후 다음부터 빨라집니다",
                                            color = Color.Cyan.copy(alpha = 0.9f),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "💡 지도가 하얀 화면으로 나타나면\n카카오 개발자 콘솔에서\n키 해시 등록이 필요합니다",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "현재 키 해시: z7mKdyTfmLmyq5vUFAsDMHcnZBo=",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    // 지도 로딩 오류 표시
                    mapLoadingError?.let { error ->
                        Card(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "오류",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "🗺️ 지도 로딩 실패",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // 캐시 정보 표시
                                Text(
                                    text = "💾 캐시 상태: ${when(cacheStatus) {
                                        "cached_available" -> "완료 (${cachedTileCount}개)"
                                        "partial_cache" -> "부분 (${cachedTileCount}개)"
                                        "no_cache" -> "없음"
                                        else -> "확인중"
                                    }}",
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "✅ 해결 방법:",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                val solutionText = when (cacheStatus) {
                                    "cached_available" -> "1. 카카오 개발자 콘솔에서 키 해시 등록 확인\n2. 앱 재시작\n3. 키 해시: z7mKdyTfmLmyq5vUFAsDMHcnZBo="
                                    "partial_cache" -> "1. 인터넷 연결 확인\n2. Wi-Fi 환경에서 재시도\n3. 캐시 일부만 있어 추가 다운로드 필요"
                                    "no_cache" -> "1. 인터넷 연결 확인\n2. 첫 실행이므로 시간 소요\n3. 초기 다운로드 화면 확인"
                                    else -> "1. 인터넷 연결 확인\n2. 카카오 개발자 콘솔에서 키 해시 등록:\n   z7mKdyTfmLmyq5vUFAsDMHcnZBo=\n3. 앱 재시작"
                                }
                                
                                Text(
                                    text = solutionText,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Button(
                                    onClick = { 
                                        mapLoadingError = null
                                        isMapLoading = true
                                        mapStartTime = 0L // 타이머 리셋
                                        cacheStatus = "unknown" // 캐시 상태 재확인
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "재시도",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("재시도", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                // Location Status Overlay
                if (currentLocation == null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GPS 신호 찾는 중...",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // GPS 연결됨 표시
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Green.copy(alpha = 0.8f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "GPS 연결됨",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "GPS 연결됨",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Map Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MapControlButton(icon = Icons.Default.MyLocation) {
                        homeViewModel.centerOnCurrentLocation()
                    }
                    MapControlButton(icon = Icons.Default.ZoomIn) {
                        com.dev_oms.trailmate.ui.components.zoomIn(kakaoMapInstance)
                    }
                    MapControlButton(icon = Icons.Default.ZoomOut) {
                        com.dev_oms.trailmate.ui.components.zoomOut(kakaoMapInstance)
                    }
                    MapControlButton(icon = Icons.Default.Refresh) {
                        homeViewModel.updateWeather()
                        homeViewModel.refreshRecommendations()
                    }
                }

                // Altitude Info
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = "현재 고도: ${locationInfo.altitude.toInt()}m",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                // Coordinates Info (for debugging)
                if (currentLocation != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = "위도: ${String.format("%.4f", locationInfo.latitude)}",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "경도: ${String.format("%.4f", locationInfo.longitude)}",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onNavigateToTracking() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = currentLocation != null
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                        contentDescription = "트래킹",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentLocation != null) "트래킹 시작" else "GPS 연결 중...")
                }

                Button(
                    onClick = { 
                        // SOS 기능 구현
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SOS")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trail Recommendation Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terrain,
                            contentDescription = "등산로",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "추천 등산로",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // 로딩 인디케이터
                        if (isLoadingRecommendations) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 에러 메시지 표시
                    recommendationError?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "⚠️ 추천 등산로 로딩 오류",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = error,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // 재시도 버튼
                                Button(
                                    onClick = { homeViewModel.refreshRecommendations() },
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "재시도",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("재시도", fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 추천 등산로 표시
                    val topRecommendation = trailRecommendations.firstOrNull()
                    if (topRecommendation != null) {
                        Text(
                            text = topRecommendation.mountain.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = topRecommendation.trail.name,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = topRecommendation.displayInfo.subtitle,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = topRecommendation.displayInfo.duration,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = topRecommendation.displayInfo.rating,
                                fontSize = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // 추천 점수 표시
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Text(
                                    text = "추천도 ${String.format("%.0f", topRecommendation.recommendationScore)}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // 위치 정보 표시
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📍 ${topRecommendation.mountain.location.address}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                    } else if (!isLoadingRecommendations) {
                        // 폴백 추천 정보
                        Text(
                            text = getRecommendedTrail(locationInfo.name),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "난이도: 중급",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "소요시간: 3시간",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⭐⭐⭐⭐",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "4.2 (1,247개 리뷰)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 추가 추천 등산로 목록 (2개 이상인 경우)
            if (trailRecommendations.size > 1) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🏔️ 주변 다른 등산로",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        trailRecommendations.drop(1).take(2).forEach { recommendation ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* Navigate to trail detail */ }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = recommendation.mountain.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${String.format("%.1f", recommendation.trail.distance)}km • ${recommendation.trail.estimatedTime / 60}시간",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Text(
                                    text = recommendation.trail.difficulty.stars,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Location Info Card (추가 정보)
            if (currentLocation != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📍 현재 위치 정보",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("정확도", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${currentLocation?.accuracy?.toInt() ?: 0}m", fontSize = 14.sp)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("속도", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val location = currentLocation
                            Text(
                                text = if (location?.hasSpeed() == true) {
                                    "${(location.speed * 3.6).toInt()} km/h"
                                } else {
                                    "정지"
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 카카오맵 인증 테스트 버튼 (개발용)
            val context = LocalContext.current
            if (true) { // 개발용으로 항상 표시
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            val intent = android.content.Intent(
                                context,
                                com.dev_oms.trailmate.ui.test.KakaoMapAuthTestActivity::class.java
                            )
                            context.startActivity(intent)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔑 카카오맵 인증 테스트",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 지도 로딩 테스트 버튼
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            val intent = android.content.Intent(
                                context,
                                com.dev_oms.trailmate.ui.test.KakaoMapLoadTestActivity::class.java
                            )
                            context.startActivity(intent)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🗺️ 지도 로딩 심화 테스트",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 카카오맵 종합 테스트 버튼 (신규)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            val intent = android.content.Intent(
                                context,
                                com.dev_oms.trailmate.ui.test.KakaoMapCompleteTestActivity::class.java
                            )
                            context.startActivity(intent)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🧪 카카오맵 종합 테스트",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getRecommendedTrail(locationName: String): String {
    return when {
        locationName.contains("북한산") -> "북한산 백운대 코스"
        locationName.contains("남산") -> "남산 순환코스"
        locationName.contains("관악산") -> "관악산 연주대 코스"
        else -> "인근 추천 등산로"
    }
} 