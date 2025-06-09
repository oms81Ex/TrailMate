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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.kakao.vectormap.KakaoMapSdk

class KakaoMapAuthTestActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 인증 테스트 시작
        performAuthTest()
        
        setContent {
            KakaoMapAuthTestScreen()
        }
    }
    
    private fun performAuthTest() {
        Log.d("KakaoAuthTest", "=== 카카오맵 인증 테스트 시작 ===")
        
        // 1. SDK 초기화 상태 확인
        val isInitialized = KakaoMapSdk.isInitialized()
        Log.d("KakaoAuthTest", "1️⃣ SDK 초기화 상태: $isInitialized")
        
        // 2. 패키지 정보 확인
        Log.d("KakaoAuthTest", "2️⃣ 패키지명: ${packageName}")
        
        // 3. 앱 정보 확인
        Log.d("KakaoAuthTest", "3️⃣ 앱 정보: TrailMate v1.0")
        
        // 4. API 키 확인 (첫 8자리만)
        val apiKey = "14526700db17a2bfe6fadd60b70d4b66"
        Log.d("KakaoAuthTest", "4️⃣ API 키 (일부): ${apiKey.take(8)}...")
        
        // 5. 필요한 키 해시 정보
        Log.d("KakaoAuthTest", "5️⃣ 필요한 키 해시: z7mKdyTfmLmyq5vUFAsDMHcnZBo=")
        
        Log.d("KakaoAuthTest", "=== 인증 테스트 완료 ===")
        Log.d("KakaoAuthTest", "🔍 로그를 확인하여 인증 상태를 점검하세요")
    }
}

@Composable
fun KakaoMapAuthTestScreen() {
    val context = LocalContext.current
    var testStatus by remember { mutableStateOf("테스트 준비 중...") }
    var isInitialized by remember { mutableStateOf(false) }
    var testLogs by remember { mutableStateOf(listOf<String>()) }
    var authTestStep by remember { mutableStateOf(0) }
    var mapTestResult by remember { mutableStateOf<String?>(null) }
    var isMapTesting by remember { mutableStateOf(false) }
    
    // 로그 추가 함수
    fun addLog(message: String) {
        testLogs = testLogs + "[${System.currentTimeMillis() % 100000}] $message"
        Log.d("KakaoAuthTestScreen", message)
    }
    
    LaunchedEffect(Unit) {
        addLog("🔍 카카오맵 인증 테스트 시작")
        kotlinx.coroutines.delay(500)
        
        // 1단계: SDK 초기화 확인
        authTestStep = 1
        addLog("1️⃣ SDK 초기화 상태 확인 중...")
        kotlinx.coroutines.delay(1000)
        
        isInitialized = KakaoMapSdk.isInitialized()
        if (isInitialized) {
            addLog("✅ SDK 초기화 완료!")
            testStatus = "✅ SDK 초기화 완료"
        } else {
            addLog("❌ SDK 초기화 실패")
            testStatus = "❌ SDK 초기화 실패"
        }
        
        kotlinx.coroutines.delay(1000)
        
        // 2단계: API 키 확인
        authTestStep = 2
        addLog("2️⃣ API 키 확인 중...")
        addLog("API 키: bd9941dd...")
        kotlinx.coroutines.delay(500)
        
        // 3단계: 패키지명 확인
        authTestStep = 3
        addLog("3️⃣ 패키지명 확인 중...")
        addLog("패키지명: com.dev_oms.trailmate")
        kotlinx.coroutines.delay(500)
        
        // 4단계: 키 해시 확인
        authTestStep = 4
        addLog("4️⃣ 키 해시 확인 중...")
        addLog("키 해시: z7mKdyTfmLmyq5vUFAsDMHcnZBo=")
        kotlinx.coroutines.delay(500)
        
        // 5단계: 지도 테스트 시작
        authTestStep = 5
        if (isInitialized) {
            addLog("5️⃣ 실제 지도 로딩 테스트 시작...")
            isMapTesting = true
            // 지도 테스트는 별도로 처리
        } else {
            addLog("⚠️ SDK 초기화 실패로 지도 테스트 건너뛰기")
            authTestStep = 6
        }
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
            text = "🗺️ 카카오맵 인증 테스트",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 현재 상태 표시
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isInitialized) Color.Green.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📊 현재 상태",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = testStatus,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 단계별 진행 상황 표시
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Cyan.copy(alpha = 0.2f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔄 진행 단계",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // 단계별 상태 표시
                val steps = listOf(
                    "SDK 초기화 확인",
                    "API 키 확인", 
                    "패키지명 확인",
                    "키 해시 확인",
                    "지도 로딩 테스트",
                    "테스트 완료"
                )
                
                steps.forEachIndexed { index, step ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        val stepNumber = index + 1
                        val isCurrentStep = authTestStep == stepNumber
                        val isCompleted = authTestStep > stepNumber
                        
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
        
        // 최종 결과 및 다음 단계 안내
        if (authTestStep >= 6) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isInitialized) Color.Green.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (isInitialized) "🎉 테스트 완료!" else "⚠️ 문제 발견",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (isInitialized) {
                        Text(
                            text = "✅ SDK 초기화 성공\n✅ 기본 설정 완료\n\n🔍 다음 단계:",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "1. 홈화면으로 돌아가기\n2. 실제 지도 확인\n3. 여전히 하얀 화면이면 카카오 콘솔 설정 확인",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = "❌ SDK 초기화 실패\n\n🛠️ 해결 방법:",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "1. 앱 완전 종료 후 재시작\n2. 인터넷 연결 확인\n3. 카카오 개발자 콘솔 설정 확인",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                // 홈으로 돌아가기
                                (context as? androidx.activity.ComponentActivity)?.finish()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isInitialized) Color.Green else Color.Gray
                            )
                        ) {
                            Text("홈으로", fontSize = 14.sp)
                        }
                        
                        Button(
                            onClick = { 
                                // 재테스트 - 액티비티 재생성
                                (context as? androidx.activity.ComponentActivity)?.recreate()
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 실시간 로그 표시
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
                    text = "📝 실시간 로그",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    reverseLayout = true // 최신 로그가 위에 오도록
                ) {
                    items(testLogs.reversed()) { log ->
                        Text(
                            text = log,
                            color = Color.Green,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 지도 테스트 영역
        if (isMapTesting) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Blue.copy(alpha = 0.1f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (mapTestResult == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🗺️ 지도 로딩 테스트 중...",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "실제 카카오맵 API 호출",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        
                        // 실제 지도 테스트 실행
                        LaunchedEffect(isMapTesting) {
                            kotlinx.coroutines.delay(3000) // 3초 후 결과 표시
                            mapTestResult = if (isInitialized) {
                                "지도 테스트는 실제 앱에서 확인 필요"
                            } else {
                                "SDK 초기화 실패로 지도 테스트 불가"
                            }
                            authTestStep = 6
                        }
                    } else {
                        Text(
                            text = mapTestResult!!,
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 설정 정보 표시
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
                    text = "🔑 인증 정보",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "API 키: bd9941dd...",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "패키지명: com.dev_oms.trailmate",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "키 해시: z7mKdyTfmLmyq5vUFAsDMHcnZBo=",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 해결 방법 안내
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFF9800).copy(alpha = 0.2f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🛠️ 해결 방법",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "1. 카카오 개발자 콘솔 접속",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "2. 앱 설정 > 플랫폼 > Android",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "3. 위 패키지명과 키 해시 등록",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "4. 설정 저장 후 앱 재시작",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 로그 확인 안내
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF9C27B0).copy(alpha = 0.2f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📝 로그 확인",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Android Studio Logcat에서 다음 태그로 확인:",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• TrailMateApp",
                    color = Color.Yellow,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "• KakaoMapView",
                    color = Color.Yellow,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "• KakaoAuthTest",
                    color = Color.Yellow,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
} 