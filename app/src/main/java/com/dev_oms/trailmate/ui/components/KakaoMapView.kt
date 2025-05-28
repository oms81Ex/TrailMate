package com.dev_oms.trailmate.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory

@Composable
fun KakaoMapView(
    modifier: Modifier = Modifier,
    latitude: Double = 37.5665,
    longitude: Double = 126.9780,
    zoomLevel: Int = 15,
    onMapReady: (KakaoMap) -> Unit = {},
    onLocationMarkerClick: () -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    
    // 생명주기 관찰자
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView?.resume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView?.pause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    // MapView는 자동으로 정리됨
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    AndroidView(
        modifier = modifier,
        factory = { context ->
            createMapView(context).also { 
                mapView = it
                initializeMap(it, latitude, longitude, zoomLevel, onMapReady) { map ->
                    kakaoMap = map
                }
            }
        },
        update = { view ->
            // 위치가 변경되었을 때 지도 업데이트
            kakaoMap?.let { map ->
                try {
                    val newPosition = LatLng.from(latitude, longitude)
                    val cameraUpdate = CameraUpdateFactory.newCenterPosition(newPosition)
                    map.moveCamera(cameraUpdate)
                    Log.d("KakaoMapView", "Map updated to: $latitude, $longitude")
                    
                    // 현재 위치 마커 추가/업데이트 (간단한 구현)
                    // 실제로는 LabelLayer를 사용해서 마커를 추가해야 하지만
                    // 현재는 지도 중심 이동으로 대체
                } catch (e: Exception) {
                    Log.e("KakaoMapView", "Error updating map location", e)
                }
            }
        }
    )
    
    // 외부에서 줌 기능 제어할 수 있도록 LaunchedEffect 추가
    LaunchedEffect(kakaoMap) {
        // 줌 인/아웃 기능을 외부에서 호출할 수 있도록 준비
        // 실제 구현은 HomeScreen에서 버튼 클릭 시 처리
    }
}

// 줌 인 기능
fun zoomIn(kakaoMap: KakaoMap?) {
    kakaoMap?.let { map ->
        try {
            // 현재 카메라 위치 기준으로 줌인
            val cameraUpdate = CameraUpdateFactory.zoomIn()
            map.moveCamera(cameraUpdate)
            Log.d("KakaoMapView", "Zoom in")
        } catch (e: Exception) {
            Log.e("KakaoMapView", "Error zooming in", e)
        }
    }
}

// 줌 아웃 기능
fun zoomOut(kakaoMap: KakaoMap?) {
    kakaoMap?.let { map ->
        try {
            // 현재 카메라 위치 기준으로 줌아웃
            val cameraUpdate = CameraUpdateFactory.zoomOut()
            map.moveCamera(cameraUpdate)
            Log.d("KakaoMapView", "Zoom out")
        } catch (e: Exception) {
            Log.e("KakaoMapView", "Error zooming out", e)
        }
    }
}

private fun createMapView(context: Context): MapView {
    Log.d("KakaoMapView", "Creating MapView...")
    return MapView(context).apply {
        id = android.R.id.content
        Log.d("KakaoMapView", "MapView created with ID: $id")
    }
}

private fun initializeMap(
    mapView: MapView,
    latitude: Double,
    longitude: Double,
    zoomLevel: Int,
    onMapReady: (KakaoMap) -> Unit,
    onKakaoMapReady: (KakaoMap) -> Unit
) {
    Log.d("KakaoMapView", "Starting MapView initialization...")
    mapView.start(
        object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                Log.d("KakaoMapView", "Map destroyed")
            }
            
            override fun onMapError(exception: Exception) {
                Log.e("KakaoMapView", "=== MAP ERROR DETECTED ===")
                Log.e("KakaoMapView", "Error Type: ${exception.javaClass.simpleName}")
                Log.e("KakaoMapView", "Error Message: ${exception.message}")
                Log.e("KakaoMapView", "Stack trace:", exception)
                
                when (exception) {
                    is MapAuthException -> {
                        val errorCode = exception.errorCode
                        val errorMessage = when (errorCode) {
                            -1 -> "알 수 없는 에러"
                            -2 -> "연결 에러 - 네트워크 확인 필요"
                            -3 -> "SocketTimeout - 네트워크 타임아웃"
                            -4 -> "ConnectTimeout - 연결 타임아웃"
                            400 -> "파라미터 오류 - API 키 확인 필요"
                            401 -> "인증 실패 - API 키가 잘못됨"
                            403 -> "권한 없음 - API 키 권한 부족"
                            429 -> "쿼터 초과 - API 사용량 한도 초과"
                            499 -> "통신 실패 - 서버 연결 실패"
                            else -> "지도 인증 에러: $errorCode"
                        }
                        Log.e("KakaoMapView", "MapAuthException Code: $errorCode")
                        Log.e("KakaoMapView", "MapAuthException Message: $errorMessage")
                        
                        // 하얀 화면 문제 해결을 위한 추가 정보
                        if (errorCode == 401 || errorCode == 403) {
                            Log.e("KakaoMapView", "!!! API KEY 문제로 인한 하얀 화면 가능성 높음 !!!")
                            Log.e("KakaoMapView", "현재 사용 중인 API 키: bd9941dd7fbc5a5a94c7ff1da148ef4d")
                            Log.e("KakaoMapView", "필요한 설정:")
                            Log.e("KakaoMapView", "  📱 패키지명: com.dev_oms.trailmate")
                            Log.e("KakaoMapView", "  🔑 키 해시: z7mKdyTfmLmyq5vUFAsDMHcnZBo=")
                            Log.e("KakaoMapView", "  🌐 카카오 개발자 콘솔에서 Android 플랫폼 등록 필요")
                        }
                    }
                    else -> {
                        Log.e("KakaoMapView", "Unknown map error: ${exception.message}")
                    }
                }
            }
        },
        object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                Log.d("KakaoMapView", "=== MAP READY SUCCESS ===")
                Log.d("KakaoMapView", "KakaoMap instance: $kakaoMap")
                
                try {
                    // 초기 위치 설정
                    val initialPosition = LatLng.from(latitude, longitude)
                    Log.d("KakaoMapView", "Initial position created: $latitude, $longitude")
                    
                    val cameraUpdate = CameraUpdateFactory.newCenterPosition(
                        initialPosition,
                        zoomLevel
                    )
                    Log.d("KakaoMapView", "Camera update created with zoom level: $zoomLevel")
                    
                    kakaoMap.moveCamera(cameraUpdate)
                    Log.d("KakaoMapView", "Camera moved successfully")
                    
                    // 지도 타일이 제대로 로드되는지 확인
                    Log.d("KakaoMapView", "Map should now be visible - checking for white screen...")
                    
                    // 카카오 지도 준비 완료
                    Log.d("KakaoMapView", "✅ Map initialized successfully at position: $latitude, $longitude")
                    
                    onMapReady(kakaoMap)
                    onKakaoMapReady(kakaoMap)
                    
                } catch (e: Exception) {
                    Log.e("KakaoMapView", "Error during map initialization", e)
                }
            }
            
            override fun getPosition(): LatLng {
                return LatLng.from(latitude, longitude)
            }
            
            override fun getZoomLevel(): Int {
                return zoomLevel
            }
            
            override fun getMapViewInfo(): MapViewInfo {
                return MapViewInfo.from("TrailMateMapView")
            }
            
            override fun getViewName(): String {
                return "TrailMateMapView"
            }
            
            override fun isVisible(): Boolean {
                return true
            }
            
            override fun getTag(): String {
                return "TrailMateMap"
            }
        }
    )
}

 