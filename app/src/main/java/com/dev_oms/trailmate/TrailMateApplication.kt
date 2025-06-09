package com.dev_oms.trailmate

import android.app.Application
import android.util.Log
import com.kakao.vectormap.KakaoMapSdk

class TrailMateApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        val apiKey = "14526700db17a2bfe6fadd60b70d4b66"
        Log.d("TrailMateApp", "🔑 카카오맵 API 키로 초기화 시작: ${apiKey.take(8)}...")
        
        try {
            // 카카오맵 SDK 초기화
            KakaoMapSdk.init(this, apiKey)
            Log.d("TrailMateApp", "✅ 카카오맵 SDK 초기화 완료")
            
            // 초기화 상태 확인
            if (KakaoMapSdk.isInitialized()) {
                Log.d("TrailMateApp", "✅ 카카오맵 SDK 초기화 상태: 정상")
                Log.d("TrailMateApp", "📱 패키지명: ${packageName}")
                Log.d("TrailMateApp", "🔧 앱 패키지: ${packageName}")
            } else {
                Log.e("TrailMateApp", "❌ 카카오맵 SDK 초기화 실패")
            }
            
        } catch (e: Exception) {
            Log.e("TrailMateApp", "💥 카카오맵 SDK 초기화 중 예외 발생", e)
        }
    }
} 