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
import java.net.HttpURLConnection
import java.net.URL
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
    var isDownloadComplete by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var isRealMapLoading by remember { mutableStateOf(false) }
    var realMapLoaded by remember { mutableStateOf(false) }
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
    
    // 초기 다운로드 시작
    LaunchedEffect(Unit) {
        addLog("🚀 TrailMate 초기 설정 시작")
        addLog("🗺️ 지도 데이터 준비 중...")
        
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
        addLog("📍 지역: 서울 중심부 (37.5665, 126.9780)")
        
        // 실제 지도 로딩을 위한 플래그 설정
        isRealMapLoading = true
        mapLoadingStartTime = System.currentTimeMillis()
        
        addLog("🗺️ 카카오맵 SDK 초기화 중...")
        addLog("⏰ 지도 로딩 타임아웃: 30초")
        
        // SDK 상태 확인
        addLog("🔍 SDK 초기화 상태: ${if (KakaoMapSdk.isInitialized()) "정상" else "실패"}")
        addLog("📦 앱 컨텍스트: ${context.packageName}")
        
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
        
        // 실제 카카오맵 로딩 타임아웃 모니터링
        launch {
            delay(30000) // 30초 타임아웃
            if (isRealMapLoading && !realMapLoaded) {
                mapLoadingTimeout = true
                kakaoMapError = if (isEmulator) {
                    "에뮬레이터에서 카카오맵 로딩 실패 (30초 타임아웃)"
                } else {
                    "카카오맵 로딩 타임아웃 (30초)"
                }
                addLog("🚨 카카오맵 로딩 타임아웃!")
                if (isEmulator) {
                    addLog("📱 에뮬레이터 환경에서는 카카오맵이 정상 작동하지 않을 수 있습니다")
                }
                addLog("💡 가능한 원인:")
                addLog("  1. RenderView 생성 실패")
                addLog("  2. OpenGL 컨텍스트 문제")
                addLog("  3. View 크기가 0")
                addLog("  4. 카카오 개발자 콘솔에서 키 해시 미등록")
                addLog("  5. 네트워크 연결 문제")
                addLog("  6. 카카오맵 API 키 문제")
                addLog("  7. 앱 패키지명 불일치")
                if (isEmulator) {
                    addLog("  8. 에뮬레이터 환경 제약")
                }
                addLog("📋 현재 키 해시: z7mKdyTfmLmyq5vUFAsDMHcnZBo=")
                addLog("📋 패키지명: com.dev_oms.trailmate")
            }
        }
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
                        text = "🎉 지도 로딩 완료!",
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val loadingDuration = if (mapLoadingStartTime > 0) {
                        (System.currentTimeMillis() - mapLoadingStartTime) / 1000
                    } else 0
                    
                    Text(
                        text = "⏱️ 로딩 시간: ${loadingDuration}초",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "✅ 카카오맵 로딩 완료\n💾 지도 캐시 생성됨\n🚀 홈화면에서 빠른 지도 표시 가능\n📍 서울 중심부 (37.5665, 126.9780)",
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
                        text = if (isRealMapLoading) "🗺️ 카카오맵 로딩 중" else "🗺️ 지도 데이터 준비 중",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 진행 표시
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRealMapLoading) {
                            // 실제 지도 로딩 중
                            CircularProgressIndicator(
                                color = Color.Cyan,
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 8.dp
                            )
                            
                            Text(
                                text = "로딩 중",
                                color = Color.White,
                                fontSize = 16.sp,
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
                                text = "🗺️ 카카오맵 로딩 중",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val elapsedTime = if (mapLoadingStartTime > 0) {
                                (System.currentTimeMillis() - mapLoadingStartTime) / 1000
                            } else 0
                            
                            Text(
                                text = "⏱️ 경과 시간: ${elapsedTime}초 / 30초",
                                color = Color.Cyan,
                                fontSize = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "📍 서울 중심부 (37.5665, 126.9780)",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            
                            Text(
                                text = "🌐 카카오 서버와 통신 중...",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
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
                        
                        // 10초 후 건너뛰기 버튼
                        if (isRealMapLoading && mapLoadingStartTime > 0) {
                            val elapsedTime = (System.currentTimeMillis() - mapLoadingStartTime) / 1000
                            
                            if (elapsedTime >= 10) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = {
                                        val sharedPreferences = context.getSharedPreferences("trailmate_prefs", Context.MODE_PRIVATE)
                                        sharedPreferences.edit().putBoolean("initial_download_complete", true).apply()
                                        
                                        addLog("🏠 사용자가 홈화면으로 이동")
                                        onDownloadComplete()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF9800) // Orange
                                    )
                                ) {
                                    Text("🏠 홈화면으로 건너뛰기")
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
                    // 지도 영역 크기 확인
                    addLog("📐 지도 로딩 영역 크기: 200dp")
                    
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
                            addLog("🎉 카카오맵 로딩 완료!")
                            addLog("⏱️ 로딩 시간: ${actualLoadTime}초")
                            addLog("✅ 카카오 서버에서 지도 데이터 수신 완료")
                            addLog("📍 서울 중심부 (37.5665, 126.9780)")
                            addLog("🚀 홈화면에서 빠른 지도 표시 가능")
                            addLog("💾 지도 캐시 생성됨")
                            
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
                                        text = "⚠️ 지도 로딩 실패",
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
                                            val sharedPreferences = context.getSharedPreferences("trailmate_prefs", Context.MODE_PRIVATE)
                                            sharedPreferences.edit().putBoolean("initial_download_complete", true).apply()
                                            
                                            addLog("⚠️ 지도 로딩 실패로 홈화면 이동")
                                            onDownloadComplete()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFF9800)
                                        )
                                    ) {
                                        Text("홈으로 이동", fontSize = 12.sp)
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
                                    
                                    Text(
                                        text = "카카오맵 로딩 중...",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val remainingTime = 30 - elapsedTime
                                    Text(
                                        text = "⏱️ 경과: ${elapsedTime}초 / 30초",
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
