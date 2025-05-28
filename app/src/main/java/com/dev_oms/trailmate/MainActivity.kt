package com.dev_oms.trailmate

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dev_oms.trailmate.ui.theme.TrailMateTheme
import com.dev_oms.trailmate.ui.PeakPalApp
import com.dev_oms.trailmate.ui.initial.InitialMapDownloadActivity
import java.io.File

class MainActivity : ComponentActivity() {
    
    private lateinit var sharedPreferences: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPreferences = getSharedPreferences("trailmate_prefs", MODE_PRIVATE)
        
        // 초기 지도 데이터 확인
        if (needsInitialMapDownload()) {
            Log.d("MainActivity", "초기 지도 다운로드 필요 - InitialMapDownloadActivity로 이동")
            startInitialDownload()
            return
        }
        
        Log.d("MainActivity", "지도 캐시 존재 - 정상 앱 시작")
        startMainApp()
    }
    
    private fun needsInitialMapDownload(): Boolean {
        // 1. SharedPreferences에서 초기 다운로드 완료 여부 확인
        val isInitialDownloadComplete = sharedPreferences.getBoolean("initial_download_complete", false)
        if (isInitialDownloadComplete) {
            Log.d("MainActivity", "✅ 이전에 초기 다운로드 완료됨")
            return false
        }
        
        // 2. 실제 캐시 파일 존재 확인 (카카오맵 캐시 디렉토리)
        val cacheDir = File(cacheDir, "kakaomap")
        val hasCachedTiles = cacheDir.exists() && cacheDir.listFiles()?.isNotEmpty() == true
        
        if (hasCachedTiles) {
            Log.d("MainActivity", "✅ 캐시 파일 발견 - 다운로드 완료로 표시")
            // 캐시가 있으면 완료로 표시
            sharedPreferences.edit().putBoolean("initial_download_complete", true).apply()
            return false
        }
        
        Log.d("MainActivity", "❌ 초기 지도 데이터 없음 - 다운로드 필요")
        return true
    }
    
    private fun startInitialDownload() {
        val intent = Intent(this, InitialMapDownloadActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun startMainApp() {
        enableEdgeToEdge()
        setContent {
            TrailMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PeakPalApp()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PeakPalPreview() {
    TrailMateTheme {
        PeakPalApp()
    }
}