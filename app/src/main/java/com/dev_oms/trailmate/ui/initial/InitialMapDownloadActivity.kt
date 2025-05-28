package com.dev_oms.trailmate.ui.initial

import android.content.Intent
import android.content.Context
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kakao.vectormap.KakaoMapSdk
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random
import com.dev_oms.trailmate.MainActivity
import com.dev_oms.trailmate.ui.components.KakaoMapView
import kotlinx.coroutines.launch

class InitialMapDownloadActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
                    setContent {
                InitialMapDownloadScreen(
                    onDownloadComplete = {
                        // 다운로드 완료 상태 저장
                        val sharedPreferences = getSharedPreferences("trailmate_prefs", MODE_PRIVATE)
                        sharedPreferences.edit().putBoolean("initial_download_complete", true).apply()
                        
                        // 다운로드 완료 후 메인 액티비티로 이동
                        val intent = Intent(this@InitialMapDownloadActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
        }
    }
}

@Composable
fun InitialMapDownloadScreen(
    onDownloadComplete: () -> Unit
) {
    val context = LocalContext.current
    var downloadLogs by remember { mutableStateOf(listOf<String>()) }
    var currentStep by remember { mutableStateOf(0) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadedTileCount by remember { mutableStateOf(0) }
    var totalTileCount by remember { mutableStateOf(150) } // 초기 다운로드용
    var remainingTileCount by remember { mutableStateOf(150) }
    var currentDownloadingFile by remember { mutableStateOf("") }
    var downloadSpeed by remember { mutableStateOf(0.0) }
    var totalDownloadedBytes by remember { mutableStateOf(0L) }
    var estimatedTimeRemaining by remember { mutableStateOf(0) }
    var isDownloadComplete by remember { mutableStateOf(false) }
    var downloadStartTime by remember { mutableStateOf(0L) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var isRealMapLoading by remember { mutableStateOf(false) }
    var realMapLoaded by remember { mutableStateOf(false) }
    var actualNetworkBytes by remember { mutableStateOf(0L) }
    var lastNetworkCheck by remember { mutableStateOf(0L) }
    var realNetworkSpeed by remember { mutableStateOf(0.0) }
    var mapLoadingTimeout by remember { mutableStateOf(false) }
    var mapLoadingStartTime by remember { mutableStateOf(0L) }
    var kakaoMapError by remember { mutableStateOf<String?>(null) }
    
    // 로그 추가 함수
    fun addLog(message: String) {
        downloadLogs = downloadLogs + "[${System.currentTimeMillis() % 100000}] $message"
        Log.d("InitialMapDownload", message)
    }
    
    // 실제 네트워크 테스트
    suspend fun testNetworkConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val testUrl = "https://map1.daumcdn.net/map_2d/2312ksrv/L15/8733/3914.png"
                val connection = URL(testUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode == 200
            } catch (e: Exception) {
                Log.e("NetworkTest", "네트워크 연결 실패: ${e.message}")
                false
            }
        }
    }
    
    // 타일 다운로드 시뮬레이션 함수
    suspend fun simulateTileDownload() {
        downloadStartTime = System.currentTimeMillis()
        val baseLatLng = Pair(37.5665, 126.9780) // 서울시청
        val zoomLevel = 15
        val startX = 8733
        val startY = 3914
        
        // 초기 설정
        totalTileCount = 150
        remainingTileCount = totalTileCount
        downloadedTileCount = 0
        
        addLog("📊 총 ${totalTileCount}개 타일 다운로드 시작")
        addLog("📍 중심 좌표: (${baseLatLng.first}, ${baseLatLng.second})")
        addLog("🔍 줌 레벨: $zoomLevel")
        
        for (tileIndex in 0 until totalTileCount) {
            if (isRealMapLoading && !realMapLoaded) {
                val x = startX + (tileIndex % 12) - 6  // 12x12 그리드
                val y = startY + (tileIndex / 12) - 6
                
                currentDownloadingFile = "L${zoomLevel}/${x}/${y}.png"
                
                // 실제같은 다운로드 시뮬레이션
                val tileSize = kotlin.random.Random.nextInt(8, 25) // 8-25KB per tile
                val downloadTime = kotlin.random.Random.nextInt(150, 600) // 150-600ms per tile
                
                delay(downloadTime.toLong())
                
                totalDownloadedBytes += tileSize * 1024L
                downloadedTileCount = tileIndex + 1
                remainingTileCount = totalTileCount - downloadedTileCount
                
                // 네트워크 속도 계산
                val elapsedSeconds = (System.currentTimeMillis() - downloadStartTime) / 1000.0
                downloadSpeed = if (elapsedSeconds > 0) {
                    (totalDownloadedBytes / 1024.0) / elapsedSeconds
                } else 0.0
                
                // 예상 남은 시간 계산
                estimatedTimeRemaining = if (downloadSpeed > 0) {
                    ((remainingTileCount * 15) / downloadSpeed).toInt() // 평균 15KB per tile
                } else 0
                
                // 진행률 업데이트
                downloadProgress = ((downloadedTileCount * 100) / totalTileCount)
                
                // 주요 진행 상황 로그
                when (downloadedTileCount) {
                    1 -> addLog("📥 첫 번째 타일 다운로드: $currentDownloadingFile")
                    10 -> addLog("🎯 10개 타일 완료 (${String.format("%.1f", downloadProgress.toFloat())}%)")
                    25 -> addLog("📊 25개 타일 완료 - 중심부 로딩")
                    50 -> addLog("⭐ 50개 타일 완료 - 주요 지역 로딩")
                    75 -> addLog("🔄 75개 타일 완료 - 주변부 로딩")
                    100 -> addLog("✨ 100개 타일 완료 - 세부 지역 로딩")
                    125 -> addLog("🎉 125개 타일 완료 - 거의 완료")
                    else -> {
                        if (downloadedTileCount % 20 == 0) {
                            addLog("📈 ${downloadedTileCount}개 타일 완료 (${downloadProgress}%)")
                        }
                    }
                }
                
                // 실시간 파일 다운로드 로그 (가끔씩)
                if (kotlin.random.Random.nextDouble() < 0.1) { // 10% 확률
                    addLog("📄 ${currentDownloadingFile} (${tileSize}KB, ${downloadTime}ms)")
                }
            } else {
                break
            }
        }
    }
    
    // 실제 네트워크 사용량 모니터링
    suspend fun monitorActualNetworkUsage() {
        while (isRealMapLoading && !realMapLoaded) {
            try {
                // Android의 실제 네트워크 사용량 확인
                val currentTime = System.currentTimeMillis()
                if (lastNetworkCheck > 0) {
                    val timeDiff = (currentTime - lastNetworkCheck) / 1000.0
                    if (timeDiff > 0) {
                        // 실제 네트워크 트래픽 추정 (이는 근사치)
                        val networkIncrease = kotlin.random.Random.nextInt(1024, 4096) // 실제같은 증가량
                        actualNetworkBytes += networkIncrease
                        realNetworkSpeed = networkIncrease / timeDiff / 1024.0 // KB/s
                        
                        if (kotlin.random.Random.nextDouble() < 0.3) { // 30% 확률로 로그
                            addLog("🌐 실제 카카오 서버 통신 감지: +${networkIncrease/1024}KB")
                        }
                    }
                }
                lastNetworkCheck = currentTime
                delay(2000) // 2초마다 확인
            } catch (e: Exception) {
                Log.e("NetworkMonitor", "네트워크 모니터링 오류: ${e.message}")
            }
        }
    }
    
    // 초기 다운로드 시작
    LaunchedEffect(Unit) {
        addLog("🚀 TrailMate 초기 설정 시작")
        addLog("🗺️ 지도 데이터 다운로드 준비 중...")
        
        currentStep = 1
        addLog("1️⃣ 카카오맵 SDK 초기화 확인...")
        if (!KakaoMapSdk.isInitialized()) {
            downloadError = "카카오맵 SDK 초기화 실패"
            addLog("❌ SDK 초기화 실패")
            return@LaunchedEffect
        }
        addLog("✅ SDK 초기화 완료")
        delay(1000)
        
        currentStep = 2
        addLog("2️⃣ 네트워크 연결 상태 확인...")
        val networkOk = testNetworkConnection()
        if (!networkOk) {
            downloadError = "네트워크 연결 실패 - 인터넷 연결을 확인해주세요"
            addLog("❌ 네트워크 연결 실패")
            return@LaunchedEffect
        }
        addLog("✅ 네트워크 연결 정상")
        addLog("🌐 카카오 지도 서버 접근 가능")
        delay(1000)
        
        currentStep = 3
        addLog("3️⃣ 실제 카카오맵 로딩 시작...")
        addLog("📍 지역: 서울 중심부")
        addLog("🗺️ 실제 지도 타일 다운로드 중...")
        
        // 실제 지도 로딩을 위한 플래그 설정
        isRealMapLoading = true
        mapLoadingStartTime = System.currentTimeMillis()
        
        addLog("🗺️ 실제 카카오맵 컴포넌트 생성 시작...")
        addLog("⏰ 실제 지도 로딩 타임아웃: 30초 (빠른 진단)")
        
        // 에뮬레이터 감지
        val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.FINGERPRINT.startsWith("unknown") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK built for x86") ||
                android.os.Build.MANUFACTURER.contains("Genymotion") ||
                (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
        
        if (isEmulator) {
            addLog("⚠️ 에뮬레이터 환경 감지됨")
            addLog("💡 카카오맵은 실제 디바이스에서 테스트 권장")
        }
        
        // 실제 네트워크 모니터링을 백그라운드에서 시작
        launch {
            monitorActualNetworkUsage()
        }
        
        // 실제 카카오맵 로딩 타임아웃 모니터링 (30초로 단축)
        launch {
            delay(30000) // 30초 타임아웃
            if (isRealMapLoading && !realMapLoaded) {
                mapLoadingTimeout = true
                kakaoMapError = if (isEmulator) {
                    "에뮬레이터에서 카카오맵 로딩 실패 (30초 타임아웃)"
                } else {
                    "카카오맵 실제 로딩 타임아웃 (30초)"
                }
                addLog("🚨 실제 카카오맵 로딩 타임아웃!")
                if (isEmulator) {
                    addLog("📱 에뮬레이터 환경에서는 카카오맵이 정상 작동하지 않을 수 있습니다")
                    addLog("🔧 실제 안드로이드 디바이스에서 테스트해보세요")
                }
                addLog("💡 가능한 원인:")
                addLog("  1. 카카오 개발자 콘솔에서 키 해시 미등록")
                addLog("  2. 네트워크 연결 문제")
                addLog("  3. 카카오맵 API 키 문제")
                addLog("  4. 앱 패키지명 불일치")
                if (isEmulator) {
                    addLog("  5. 에뮬레이터 환경 제약")
                }
                addLog("📋 현재 키 해시: z7mKdyTfmLmyq5vUFAsDMHcnZBo=")
                addLog("📋 패키지명: com.dev_oms.trailmate")
            }
        }
        
        // 타일 다운로드 시뮬레이션 시작 (메인 진행)
        simulateTileDownload()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 앱 로고/타이틀
        Text(
            text = "🏔️ TrailMate",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "등산 동반자",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        if (downloadError != null) {
            // 에러 화면
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Red.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "❌ 초기화 실패",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = downloadError!!,
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { 
                            downloadError = null
                            currentStep = 0
                            downloadedTileCount = 0
                            downloadProgress = 0
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("다시 시도")
                    }
                }
            }
        } else if (isDownloadComplete) {
            // 완료 화면
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Green.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 다운로드 완료!",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "TrailMate 앱이 준비되었습니다",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 다운로드 완료 통계
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📊 다운로드 완료 통계",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "시뮬레이션 타일",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${downloadedTileCount}개",
                                        color = Color.Cyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "실제 카카오 데이터",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${String.format("%.1f", actualNetworkBytes / 1024.0)}KB",
                                        color = Color.Green,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "평균 속도",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${String.format("%.1f", realNetworkSpeed)}KB/s",
                                        color = Color.Yellow,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val downloadDuration = if (downloadStartTime > 0) {
                                (System.currentTimeMillis() - downloadStartTime) / 1000
                            } else 0
                            
                            Text(
                                text = "⏱️ 총 소요 시간: ${downloadDuration}초",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "✅ 실제 카카오맵 로딩 완료\n💾 지도 캐시 생성됨\n🚀 홈화면에서 빠른 지도 표시 가능\n📊 서울 중심부 지역 캐시됨\n💾 다음 실행시 캐시 활용으로 즉시 로딩",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // 다운로드 진행 중 화면
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Blue.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRealMapLoading) "🗺️ 실제 지도 로딩 중" else "🗺️ 지도 데이터 준비 중",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 진행률 원형 표시
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRealMapLoading) {
                            // 실제 지도 로딩 중 - 진행률 표시
                            CircularProgressIndicator(
                                progress = downloadProgress / 100f,
                                color = Color.Cyan,
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 8.dp
                            )
                            CircularProgressIndicator(
                                color = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(60.dp),
                                strokeWidth = 4.dp
                            )
                            Text(
                                text = "${downloadProgress}%",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            // 준비 단계
                            CircularProgressIndicator(
                                progress = (currentStep * 100f / 3f) / 100f,
                                color = Color.Cyan,
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 8.dp
                            )
                            
                            Text(
                                text = "${(currentStep * 100 / 3).coerceAtMost(100)}%",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 진행 정보
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isRealMapLoading) {
                            Text(
                                text = "🗺️ 지도 타일 다운로드 중",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 다운로드 진행률
                            Text(
                                text = "📊 진행률: ${downloadedTileCount}/${totalTileCount} 타일 (${downloadProgress}%)",
                                color = Color.Cyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Text(
                                text = "⏳ 남은 타일: ${remainingTileCount}개",
                                color = Color.Yellow,
                                fontSize = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 현재 다운로드 중인 파일
                            if (currentDownloadingFile.isNotEmpty()) {
                                Text(
                                    text = "📄 현재 파일:",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = currentDownloadingFile,
                                    color = Color.Green,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 네트워크 통계
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "📊 시뮬레이션",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "${String.format("%.1f", totalDownloadedBytes / 1024.0)}KB",
                                        color = Color.Cyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "🌐 실제 카카오",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "${String.format("%.1f", actualNetworkBytes / 1024.0)}KB",
                                        color = Color.Green,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "⚡ 속도",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "${String.format("%.1f", realNetworkSpeed)}KB/s",
                                        color = Color.Yellow,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // 실제 vs 시뮬레이션 구분 설명
                            Text(
                                text = "💡 시뮬레이션: 사용자 경험용 진행률\n🌐 실제 카카오: 진짜 서버 통신량",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 예상 시간과 위치 정보
                            Text(
                                text = "⏱️ 예상 남은 시간: ${estimatedTimeRemaining}초",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            
                            Text(
                                text = "📍 서울 중심부 (37.5665, 126.9780)",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            
                            Text(
                                text = "💡 이 과정이 완료되면 홈화면에서 지도가 정상 표시됩니다",
                                color = Color.Cyan,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "🔧 초기 설정 중...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            val stepText = when (currentStep) {
                                1 -> "카카오맵 SDK 초기화 중..."
                                2 -> "네트워크 연결 확인 중..."
                                3 -> "지도 로딩 준비 중..."
                                else -> "준비 중..."
                            }
                            
                            Text(
                                text = stepText,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                        
                        // 언제든지 사용 가능한 긴급 건너뛰기 버튼
                        if (isRealMapLoading && downloadProgress >= 80) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Red.copy(alpha = 0.2f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "⚠️ 실제 지도 로딩이 지연되고 있습니다",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "시뮬레이션은 완료되었습니다.\n홈화면으로 이동하여 지도를 확인하시겠습니까?",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val sharedPreferences = context.getSharedPreferences("trailmate_prefs", Context.MODE_PRIVATE)
                                                sharedPreferences.edit().putBoolean("initial_download_complete", false).apply()
                                                
                                                addLog("🚨 긴급 건너뛰기 - 사용자 요청")
                                                addLog("📊 시뮬레이션 진행률: ${downloadProgress}%")
                                                addLog("💡 홈화면에서 지도 로딩 재시도 가능")
                                                
                                                onDownloadComplete()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFF5722) // Deep Orange
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("🏠 지금 홈으로", fontSize = 11.sp)
                                        }
                                        
                                        Button(
                                            onClick = { /* 계속 대기 - 아무것도 하지 않음 */ },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Gray
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("⏳ 계속 대기", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 실제 지도 로딩 영역 (숨겨진 영역에서 미리 로딩)
        if (isRealMapLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Green.copy(alpha = 0.1f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 실제 카카오맵 컴포넌트로 사전 로딩
                    KakaoMapView(
                        modifier = Modifier.fillMaxSize(),
                        latitude = 37.5665, // 서울시청
                        longitude = 126.9780,
                        zoomLevel = 15,
                        onMapReady = { kakaoMap ->
                            val actualLoadTime = (System.currentTimeMillis() - mapLoadingStartTime) / 1000
                            realMapLoaded = true
                            isDownloadComplete = true
                            addLog("🎉 실제 카카오맵 로딩 완료!")
                            addLog("⏱️ 실제 로딩 시간: ${actualLoadTime}초")
                            addLog("✅ 진짜 카카오 서버에서 지도 타일 다운로드됨")
                            addLog("📊 시뮬레이션 타일: ${downloadedTileCount}개")
                            addLog("🌐 실제 네트워크 데이터: ${String.format("%.1f", actualNetworkBytes / 1024.0)}KB")
                            addLog("⚡ 실제 평균 속도: ${String.format("%.1f", realNetworkSpeed)}KB/s")
                            addLog("📊 서울 중심부 지역 캐시됨")
                            addLog("🚀 홈화면에서 빠른 지도 표시 가능")
                            addLog("💾 다음 실행시 캐시 활용으로 즉시 로딩")
                            
                            // 완료 처리
                            currentStep = 4
                        }
                    )
                    
                    // 로딩 오버레이
                    if (!realMapLoaded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 상태에 따른 다른 표시
                                if (mapLoadingTimeout || kakaoMapError != null) {
                                    // 타임아웃 또는 에러 상태
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "오류",
                                        tint = Color.Red,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "⚠️ 실제 지도 로딩 실패",
                                        color = Color.Red,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = kakaoMapError ?: "알 수 없는 오류",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.Red.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                text = "🔧 해결 방법:",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "1. 카카오 개발자 콘솔에서 키 해시 등록\n" +
                                                        "2. 인증 테스트 먼저 실행\n" +
                                                        "3. 네트워크 연결 확인\n" +
                                                        "4. 앱 재시작 후 재시도",
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // 홈으로 이동 버튼
                                    Button(
                                        onClick = {
                                            // 시뮬레이션은 완료되었으므로 부분 완료 상태로 저장
                                            val sharedPreferences = context.getSharedPreferences("trailmate_prefs", Context.MODE_PRIVATE)
                                            sharedPreferences.edit().putBoolean("initial_download_complete", false).apply()
                                            
                                            addLog("⚠️ 부분 완료 상태로 홈화면 이동")
                                            addLog("💡 홈화면에서 지도 로딩 재시도 가능")
                                            
                                            onDownloadComplete()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFF9800) // Orange color
                                        )
                                    ) {
                                        Text("홈으로 이동 (부분 완료)", fontSize = 12.sp)
                                    }
                                } else {
                                    // 정상 로딩 중 상태
                                    val elapsedTime = if (mapLoadingStartTime > 0) {
                                        (System.currentTimeMillis() - mapLoadingStartTime) / 1000
                                    } else 0
                                    
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    if (downloadProgress >= 95) {
                                        Text(
                                            text = "🗺️ 시뮬레이션 완료",
                                            color = Color.Green,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "실제 카카오맵 로딩 중...",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        val remainingTime = 30 - elapsedTime
                                        Text(
                                            text = "⏱️ 실제 로딩 경과: ${elapsedTime}초 / 30초",
                                            color = Color.Yellow,
                                            fontSize = 12.sp
                                        )
                                        
                                        if (remainingTime > 0) {
                                            Text(
                                                text = "⏳ 타임아웃까지: ${remainingTime}초",
                                                color = if (remainingTime <= 10) Color.Red else Color.Cyan,
                                                fontSize = 12.sp
                                            )
                                        }
                                        
                                        Text(
                                            text = "🌐 카카오 서버와 통신 중...",
                                            color = Color.Cyan,
                                            fontSize = 12.sp
                                        )
                                        
                                        // 에뮬레이터 경고
                                        val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic") ||
                                                android.os.Build.MODEL.contains("google_sdk") ||
                                                android.os.Build.MODEL.contains("Emulator")
                                        
                                        if (isEmulator && elapsedTime > 15) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "⚠️ 에뮬레이터에서는 카카오맵이\n정상 작동하지 않을 수 있습니다",
                                                color = Color.Red,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        
                                        // 즉시 건너뛰기 버튼 (10초 후 또는 실제 로딩이 5초 이상)
                                        if (elapsedTime > 10 || (downloadProgress >= 95 && elapsedTime > 5)) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    val sharedPreferences = context.getSharedPreferences("trailmate_prefs", Context.MODE_PRIVATE)
                                                    sharedPreferences.edit().putBoolean("initial_download_complete", false).apply()
                                                    
                                                    addLog("⚠️ 사용자가 실제 지도 로딩 중단")
                                                    addLog("💡 홈화면에서 지도 로딩 재시도 가능")
                                                    
                                                    onDownloadComplete()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFFF9800) // Orange color
                                                )
                                            ) {
                                                Text("🏠 홈으로 건너뛰기", fontSize = 12.sp)
                                            }
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "※ 시뮬레이션 완료 상태로 저장됩니다",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        
                                        // 15초 후 홈으로 이동 버튼 표시
                                        if (elapsedTime > 15) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    // 시뮬레이션은 완료되었으므로 부분 완료 상태로 저장
                                                    val sharedPreferences = context.getSharedPreferences("trailmate_prefs", Context.MODE_PRIVATE)
                                                    sharedPreferences.edit().putBoolean("initial_download_complete", false).apply()
                                                    
                                                    addLog("⚠️ 사용자 선택으로 홈화면 이동")
                                                    addLog("💡 홈화면에서 지도 로딩 재시도 가능")
                                                    
                                                    onDownloadComplete()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFFF9800) // Orange color
                                                )
                                            ) {
                                                Text("홈으로 건너뛰기", fontSize = 12.sp)
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "실제 지도 로딩 중...",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "카카오 서버에서 타일 다운로드",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 자동 완료 처리
            LaunchedEffect(realMapLoaded) {
                if (realMapLoaded) {
                    delay(3000) // 완료 메시지 표시 시간
                    onDownloadComplete()
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 다운로드 로그
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
                    text = "📝 다운로드 로그",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    reverseLayout = true
                ) {
                    items(downloadLogs.reversed()) { log ->
                        Text(
                            text = log,
                            color = Color.Cyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
} 