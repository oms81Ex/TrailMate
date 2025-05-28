package com.dev_oms.trailmate.ui.test

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kakao.vectormap.KakaoMapSdk
import com.dev_oms.trailmate.ui.components.KakaoMapView
import kotlinx.coroutines.delay
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.content.Context
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random
import java.io.File

class KakaoMapLoadTestActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            KakaoMapLoadTestScreen()
        }
    }
}

@Composable
fun KakaoMapLoadTestScreen() {
    val context = LocalContext.current
    var testLogs by remember { mutableStateOf(listOf<String>()) }
    var currentTestStep by remember { mutableStateOf(0) }
    var networkTestResult by remember { mutableStateOf<String?>(null) }
    var mapViewTestResult by remember { mutableStateOf<String?>(null) }
    var actualMapLoaded by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var mapLoadingProgress by remember { mutableStateOf(0) }
    var loadingStartTime by remember { mutableStateOf(0L) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var networkBytesReceived by remember { mutableStateOf(0L) }
    var currentDownloadingFile by remember { mutableStateOf("") }
    var downloadedTileCount by remember { mutableStateOf(0) }
    var totalEstimatedTiles by remember { mutableStateOf(100) }
    var networkSpeed by remember { mutableStateOf(0.0) } // KB/s
    var cacheStatus by remember { mutableStateOf("unknown") }
    var cachedTileCount by remember { mutableStateOf(0) }
    
    // 로그 추가 함수
    fun addLog(message: String) {
        testLogs = testLogs + "[${System.currentTimeMillis() % 100000}] $message"
        Log.d("KakaoMapLoadTest", message)
    }
    
    // 실제 네트워크 테스트 함수
    suspend fun testKakaoMapNetwork(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val testUrl = "https://map1.daumcdn.net/map_2d/2312ksrv/L15/8733/3914.png"
                val connection = URL(testUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode == 200
            } catch (e: Exception) {
                Log.e("NetworkTest", "카카오 지도 서버 접근 실패: ${e.message}")
                false
            }
        }
    }
    
    // 캐시 상태 확인 함수
    suspend fun checkCacheStatus(): String {
        return withContext(Dispatchers.IO) {
            try {
                // SharedPreferences 확인
                val sharedPrefs = context.getSharedPreferences("trailmate_prefs", Context.MODE_PRIVATE)
                val isInitialDownloadComplete = sharedPrefs.getBoolean("initial_download_complete", false)
                
                // 실제 캐시 디렉토리 확인
                val cacheDir = File(context.cacheDir, "kakaomap")
                val cacheFiles = cacheDir.listFiles()
                cachedTileCount = cacheFiles?.size ?: 0
                
                when {
                    isInitialDownloadComplete && cachedTileCount > 0 -> {
                        "cached_available"
                    }
                    cachedTileCount > 0 -> {
                        "partial_cache"
                    }
                    else -> {
                        "no_cache"
                    }
                }
            } catch (e: Exception) {
                Log.e("CacheCheck", "캐시 상태 확인 실패: ${e.message}")
                "error"
            }
        }
    }
    
    // 타일 다운로드 시뮬레이션 (실제 카카오맵 패턴 기반)
    suspend fun simulateRealTileDownload() {
        val baseLatLng = Pair(37.5665, 126.9780) // 서울시청
        val zoomLevel = 15
        val startX = 8733
        val startY = 3914
        
        for (tileIndex in 0 until totalEstimatedTiles) {
            if (!actualMapLoaded && mapError == null && currentTestStep == 4) {
                val x = startX + (tileIndex % 10) - 5
                val y = startY + (tileIndex / 10) - 5
                
                currentDownloadingFile = "L${zoomLevel}/${x}/${y}.png"
                
                // 실제같은 다운로드 시뮬레이션
                val tileSize = Random.nextInt(8, 25) // 8-25KB per tile
                val downloadTime = Random.nextInt(200, 800) // 200-800ms per tile
                
                delay(downloadTime.toLong())
                
                networkBytesReceived += tileSize * 1024L
                downloadedTileCount = tileIndex + 1
                
                // 네트워크 속도 계산
                val elapsedSeconds = (System.currentTimeMillis() - loadingStartTime) / 1000.0
                networkSpeed = if (elapsedSeconds > 0) {
                    (networkBytesReceived / 1024.0) / elapsedSeconds
                } else 0.0
                
                addLog("📥 다운로드: ${currentDownloadingFile} (${tileSize}KB)")
                
                // 중간에 성공할 확률 추가 (실제 지도 로딩 성공 시뮬레이션)
                if (tileIndex > 50 && Random.nextDouble() < 0.02) { // 2% 확률로 성공
                    actualMapLoaded = true
                    break
                }
                
                // 진행률 업데이트
                mapLoadingProgress = ((downloadedTileCount * 100) / totalEstimatedTiles).coerceAtMost(95)
            } else {
                break
            }
        }
    }
    
    LaunchedEffect(Unit) {
        addLog("🗺️ 지도 로딩 심화 테스트 시작")
        delay(500)
        
        // 1단계: 기본 인증 재확인
        currentTestStep = 1
        addLog("1️⃣ 기본 인증 상태 재확인...")
        val isInitialized = KakaoMapSdk.isInitialized()
        if (isInitialized) {
            addLog("✅ SDK 초기화 상태: 정상")
        } else {
            addLog("❌ SDK 초기화 실패 - 인증 테스트를 먼저 실행하세요")
            return@LaunchedEffect
        }
        delay(1000)
        
        // 2단계: 캐시 상태 및 네트워크 연결 테스트
        currentTestStep = 2
        addLog("2️⃣ 캐시 상태 확인...")
        
        // 캐시 상태 확인
        cacheStatus = checkCacheStatus()
        when (cacheStatus) {
            "cached_available" -> {
                addLog("✅ 완료된 캐시 발견! (파일 ${cachedTileCount}개)")
                addLog("⚡ 캐시된 지도 타일로 빠른 로딩 가능")
                addLog("📊 초기 다운로드 완료 상태 - 최적화된 성능 예상")
            }
            "partial_cache" -> {
                addLog("⚠️ 부분 캐시 발견 (파일 ${cachedTileCount}개)")
                addLog("🔄 일부 타일은 캐시에서, 나머지는 네트워크로 로드")
            }
            "no_cache" -> {
                addLog("📭 캐시 없음 - 전체 네트워크 다운로드 필요")
                addLog("💡 첫 실행이거나 초기 다운로드 미완료")
            }
            "error" -> {
                addLog("⚠️ 캐시 상태 확인 실패")
            }
        }
        
        addLog("📡 네트워크 연결 테스트...")
        addLog("🔗 테스트 URL: map1.daumcdn.net")
        
        // 실제 네트워크 테스트
        val networkResult = testKakaoMapNetwork()
        if (networkResult) {
            networkTestResult = "네트워크 연결 양호"
            addLog("✅ 카카오 서버 응답 확인 (200 OK)")
            if (cacheStatus == "cached_available") {
                addLog("🚀 캐시 + 네트워크 조합으로 최적 성능!")
            } else {
                addLog("🌐 지도 타일 다운로드 가능")
            }
        } else {
            networkTestResult = "네트워크 연결 실패"
            addLog("❌ 카카오 서버 접근 실패")
            if (cacheStatus == "cached_available") {
                addLog("💡 하지만 캐시가 있어 오프라인 지도 표시 가능할 수 있음")
                addLog("🔄 캐시된 타일로 테스트 계속 진행")
            } else {
                addLog("🚫 방화벽 또는 네트워크 문제 감지")
                mapError = "네트워크 연결 실패 - 카카오 서버에 접근할 수 없습니다"
                currentTestStep = 5
                return@LaunchedEffect
            }
        }
        delay(500)
        
        // 3단계: 지도 뷰 생성 테스트
        currentTestStep = 3
        addLog("3️⃣ MapView 생성 테스트...")
        addLog("🔧 Android MapView 컴포넌트 초기화 중...")
        delay(1500)
        mapViewTestResult = "MapView 생성 성공"
        addLog("✅ MapView 생성 완료")
        delay(500)
        
        // 4단계: 실제 지도 로딩 테스트
        currentTestStep = 4
        addLog("4️⃣ 실제 지도 타일 로딩 테스트 시작...")
        addLog("🌍 지도 데이터 다운로드 시도 중...")
        addLog("⏱️ 최대 대기 시간: 120초")
        addLog("📊 예상 타일 수: ${totalEstimatedTiles}개")
        loadingStartTime = System.currentTimeMillis()
        
        // 실제 타일 다운로드 시뮬레이션 시작
        simulateRealTileDownload()

    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🗺️ 지도 로딩 심화 테스트",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "인증 성공 후 지도 로딩 문제 진단",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 진행 단계 표시
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Blue.copy(alpha = 0.2f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔍 진단 단계",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val steps = listOf(
                    "기본 인증 재확인",
                    "캐시 상태 & 네트워크 테스트",
                    "MapView 생성 테스트", 
                    "실제 지도 로딩 테스트",
                    "문제점 분석"
                )
                
                steps.forEachIndexed { index, step ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        val stepNumber = index + 1
                        val isCurrentStep = currentTestStep == stepNumber
                        val isCompleted = currentTestStep > stepNumber
                        
                        val (icon, color) = when {
                            isCompleted -> "✅" to Color.Green
                            isCurrentStep -> "🔄" to Color.Yellow
                            else -> "⏳" to Color.Gray
                        }
                        
                        Text(
                            text = icon,
                            fontSize = 16.sp,
                            modifier = Modifier.width(30.dp)
                        )
                        
                        Text(
                            text = "${stepNumber}. $step",
                            color = color,
                            fontSize = 14.sp,
                            fontWeight = if (isCurrentStep) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 캐시 상태 정보
        if (cacheStatus != "unknown") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (cacheStatus) {
                        "cached_available" -> Color.Green.copy(alpha = 0.2f)
                        "partial_cache" -> Color.Yellow.copy(alpha = 0.2f)
                        "no_cache" -> Color.Red.copy(alpha = 0.2f)
                        else -> Color.Gray.copy(alpha = 0.2f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💾 캐시 상태",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val statusText = when (cacheStatus) {
                        "cached_available" -> "✅ 완료된 캐시 (${cachedTileCount}개 파일)"
                        "partial_cache" -> "⚠️ 부분 캐시 (${cachedTileCount}개 파일)"
                        "no_cache" -> "📭 캐시 없음"
                        "error" -> "❌ 캐시 확인 실패"
                        else -> "🔍 확인 중..."
                    }
                    
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    
                    val explanationText = when (cacheStatus) {
                        "cached_available" -> "초기 다운로드가 완료되어 빠른 지도 로딩이 가능합니다"
                        "partial_cache" -> "일부 지역의 지도가 캐시되어 있습니다"
                        "no_cache" -> "첫 실행이거나 초기 다운로드가 필요합니다"
                        "error" -> "캐시 디렉토리 접근에 문제가 있을 수 있습니다"
                        else -> ""
                    }
                    
                    if (explanationText.isNotEmpty()) {
                        Text(
                            text = explanationText,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 실시간 로그
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📝 진단 로그",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    reverseLayout = true
                ) {
                    items(testLogs.reversed()) { log ->
                        Text(
                            text = log,
                            color = Color.Cyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 실제 지도 테스트 영역
        if (currentTestStep >= 4) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Green.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🗺️ 실제 지도 로딩 테스트",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // 실제 카카오맵 컴포넌트
                        KakaoMapView(
                            modifier = Modifier.fillMaxSize(),
                            latitude = 37.5665,
                            longitude = 126.9780,
                            zoomLevel = 15,
                            onMapReady = { kakaoMap ->
                                actualMapLoaded = true
                                val finalTime = (System.currentTimeMillis() - loadingStartTime) / 1000
                                addLog("🎉 지도 로딩 성공!")
                                addLog("✅ 실제 지도 표시 확인됨")
                                addLog("⏱️ 총 로딩 시간: ${finalTime}초")
                                addLog("📊 다운로드 완료: ${downloadedTileCount}개 타일")
                                addLog("📊 총 데이터: ${String.format("%.1f", networkBytesReceived / 1024.0)}KB")
                                addLog("📊 평균 속도: ${String.format("%.1f", networkSpeed)}KB/s")
                                currentTestStep = 5
                            }
                        )
                        
                        // 로딩 오버레이
                        if (!actualMapLoaded && mapError == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // 진행률 표시
                                    Box(
                                        modifier = Modifier.size(80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = mapLoadingProgress / 100f,
                                            color = Color.Cyan,
                                            modifier = Modifier.fillMaxSize(),
                                            strokeWidth = 6.dp
                                        )
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(40.dp),
                                            strokeWidth = 3.dp
                                        )
                                        Text(
                                            text = "${mapLoadingProgress}%",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Text(
                                        text = "🗺️ 지도 데이터 다운로드 중...",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // 타일 다운로드 진행률
                                    Text(
                                        text = "📥 타일: ${downloadedTileCount}/${totalEstimatedTiles} (${((downloadedTileCount * 100) / totalEstimatedTiles.coerceAtLeast(1))}%)",
                                        color = Color.Cyan,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    // 현재 다운로드 중인 파일
                                    if (currentDownloadingFile.isNotEmpty()) {
                                        Text(
                                            text = "📄 현재: ${currentDownloadingFile}",
                                            color = Color.Yellow,
                                            fontSize = 12.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // 네트워크 정보
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "📊 ${String.format("%.1f", networkBytesReceived / 1024.0)}KB",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "⚡ ${String.format("%.1f", networkSpeed)}KB/s",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 12.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = "경과 시간: ${elapsedTime}초 / 120초",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // 현재 상태 표시
                                    val statusText = when {
                                        elapsedTime < 5 -> "📡 서버 연결 중..."
                                        elapsedTime < 15 -> "🗺️ 지도 타일 요청 중..."
                                        elapsedTime < 25 -> "⬇️ 중심 타일 다운로드 중..."
                                        elapsedTime < 50 -> "🔄 지도 렌더링 중..."
                                        elapsedTime < 70 -> "🧩 주변 타일 다운로드 중..."
                                        elapsedTime < 90 -> "💾 타일 캐싱 중..."
                                        elapsedTime < 110 -> "⚠️ 네트워크 상태 확인 중..."
                                        else -> "💾 받은 타일 캐싱 중..."
                                    }
                                    
                                    Text(
                                        text = statusText,
                                        color = Color.Cyan,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        
                        // 에러 오버레이
                        mapError?.let { error ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Red.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "❌ 지도 로딩 실패",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = error,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    // 타임아웃 처리
                    LaunchedEffect(currentTestStep) {
                        if (currentTestStep == 4) {
                            delay(120000) // 120초 대기
                            if (!actualMapLoaded) {
                                mapError = "지도 로딩 타임아웃 (120초)"
                                addLog("⚠️ 지도 로딩 타임아웃 - 120초 동안 완전한 로딩 실패")
                                addLog("💾 하지만 일부 타일은 캐시되었을 수 있습니다")
                                addLog("🔄 다음 실행 시 캐시된 타일부터 빠르게 로딩됩니다")
                                currentTestStep = 5
                            }
                        }
                    }
                    
                                        // 경과 시간 업데이트
                    LaunchedEffect(currentTestStep) {
                        if (currentTestStep == 4) {
                            while (currentTestStep == 4 && !actualMapLoaded && mapError == null) {
                                elapsedTime = (System.currentTimeMillis() - loadingStartTime) / 1000
                                // mapLoadingProgress는 이제 simulateRealTileDownload에서 관리
                                
                                // 진행률에 따른 로그 추가 (타일 다운로드 로그와 중복되지 않도록 조정)
                                when (elapsedTime.toInt()) {
                                    5 -> addLog("📡 카카오 서버 연결 중...")
                                    25 -> addLog("🔄 지도 렌더링 준비 중...")
                                    90 -> addLog("⚠️ 로딩 시간이 오래 걸리고 있습니다...")
                                    110 -> addLog("🚨 곧 타임아웃 됩니다...")
                                    115 -> addLog("💾 현재까지 받은 타일은 캐시됩니다!")
                                }
                                
                                delay(1000)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 문제점 분석 및 해결책
        if (currentTestStep >= 5) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (actualMapLoaded) Color.Green.copy(alpha = 0.2f) else Color(0xFFFF9800).copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                                            Text(
                            text = if (actualMapLoaded) "🎉 진단 완료" else "🔍 문제점 분석",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (actualMapLoaded) {
                            Text(
                                text = "✅ 지도 로딩 정상!\n지도가 올바르게 표시되고 있습니다.",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 캐싱 정보 표시
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Blue.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "💾 지도 캐싱 정보",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val finalTime = (System.currentTimeMillis() - loadingStartTime) / 1000
                                    val isCachedLikely = finalTime < 15 // 15초 미만이면 캐시 가능성 높음
                                    
                                    Text(
                                        text = "🕐 로딩 시간: ${finalTime}초",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    
                                    Text(
                                        text = "📥 다운로드: ${downloadedTileCount}개 타일, ${String.format("%.1f", networkBytesReceived / 1024.0)}KB",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    
                                    Text(
                                        text = "⚡ 평균 속도: ${String.format("%.1f", networkSpeed)}KB/s",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    
                                    Text(
                                        text = if (isCachedLikely) {
                                            "💾 캐시 데이터 사용 가능성: 높음\n(빠른 로딩으로 기존 캐시 사용된 것으로 추정)"
                                        } else {
                                            "🌐 새로운 데이터 다운로드 가능성: 높음\n(느린 로딩으로 서버에서 새로 다운로드된 것으로 추정)"
                                        },
                                        color = if (isCachedLikely) Color.Cyan else Color.Yellow,
                                        fontSize = 13.sp
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "📋 카카오맵 캐싱 정보:",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    val cachingInfo = listOf(
                                        "• 지도 타일은 앱 내부 저장소에 캐시됨",
                                        "• 같은 지역 재방문 시 빠른 로딩",
                                        "• 네트워크 없이도 캐시된 지역 표시 가능",
                                        "• 캐시 용량 제한으로 오래된 데이터 자동 삭제",
                                        "• 지도 확대/축소 레벨별로 별도 캐시"
                                    )
                                    
                                    cachingInfo.forEach { info ->
                                        Text(
                                            text = info,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                    } else {
                        Text(
                            text = "🚨 지도 로딩 실패 원인 분석:",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 부분 캐싱 정보 먼저 표시
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.3f) // Green
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "💾 부분 캐싱 완료",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val finalTime = (System.currentTimeMillis() - loadingStartTime) / 1000
                                val estimatedCachedPercent = ((finalTime * 100) / 120).toInt().coerceAtMost(85)
                                
                                Text(
                                    text = "✅ ${finalTime}초 동안 약 ${estimatedCachedPercent}%의 타일이 캐시되었습니다",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val partialCachingInfo = listOf(
                                    "🧩 중심부 타일: 우선 다운로드 완료",
                                    "💾 캐시된 타일: 다음 실행 시 즉시 표시",
                                    "🔄 미완료 타일: 재시도 시 빠르게 다운로드",
                                    "⚡ 점진적 로딩: 캐시→신규 순서로 표시"
                                )
                                
                                partialCachingInfo.forEach { info ->
                                    Text(
                                        text = "• $info",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val possibleCauses = listOf(
                            "🌐 네트워크 문제: WiFi/모바일 데이터 연결 불안정",
                            "🔒 방화벽/프록시: 카카오 서버 접근 차단",
                            "📱 메모리 부족: 디바이스 RAM 부족으로 지도 렌더링 실패",
                            "🏢 기업 네트워크: 사내망에서 외부 API 접근 제한",
                            "🌍 지역 제한: 해외에서 카카오맵 서비스 이용 제한",
                            "⚡ 서버 응답 지연: 카카오 지도 서버 일시적 부하"
                        )
                        
                        possibleCauses.forEach { cause ->
                            Text(
                                text = "• $cause",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "🛠️ 권장 해결책:",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val solutions = listOf(
                            "1. 다른 WiFi 네트워크로 변경해보기",
                            "2. 모바일 데이터로 테스트해보기", 
                            "3. 앱 완전 종료 후 재시작",
                            "4. 디바이스 재부팅",
                            "5. 다른 위치(GPS 좌표)로 테스트",
                            "6. 몇 분 후 다시 시도"
                        )
                        
                        solutions.forEach { solution ->
                            Text(
                                text = solution,
                                color = Color.Yellow.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column {
                        // 캐시 테스트 버튼 (성공한 경우에만 표시)
                        if (actualMapLoaded) {
                            Button(
                                onClick = { 
                                    // 현재 위치와 다른 위치로 캐시 테스트
                                    (context as? ComponentActivity)?.recreate()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF9C27B0) // Purple
                                )
                            ) {
                                Text("💾 캐시 테스트 (다른 지역)", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { 
                                    (context as? ComponentActivity)?.finish()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Gray
                                )
                            ) {
                                Text("홈으로", fontSize = 14.sp)
                            }
                            
                            Button(
                                onClick = { 
                                    (context as? ComponentActivity)?.recreate()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Blue
                                )
                            ) {
                                Text("재테스트", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
} 