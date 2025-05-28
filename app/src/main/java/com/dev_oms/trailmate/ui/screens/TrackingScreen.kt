package com.dev_oms.trailmate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dev_oms.trailmate.viewmodel.TrackingViewModel
import kotlin.math.roundToInt

data class TrackingStat(
    val value: String,
    val unit: String,
    val label: String
)

data class ControlAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    trackingViewModel: TrackingViewModel = viewModel()
) {
    val trackingState by trackingViewModel.trackingState.collectAsState()
    val elapsedTime by trackingViewModel.elapsedTime.collectAsState()
    
    var showStopDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Tracking Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = trackingViewModel.formatElapsedTime(elapsedTime),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        trackingState.isPaused -> Icons.Default.Pause
                        trackingState.isTracking -> Icons.Default.DirectionsRun
                        else -> Icons.Default.Stop
                    },
                    contentDescription = "트래킹 상태",
                    tint = when {
                        trackingState.isPaused -> MaterialTheme.colorScheme.secondary
                        trackingState.isTracking -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when {
                        trackingState.isPaused -> "일시정지 중"
                        trackingState.isTracking -> "트래킹 진행 중"
                        else -> "트래킹 준비"
                    },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Grid
        val stats = listOf(
            TrackingStat(
                String.format("%.1f", trackingState.distance), 
                "km", 
                "이동거리"
            ),
            TrackingStat(
                "${(elapsedTime / 60000).toInt()}", 
                "분", 
                "경과시간"
            ),
            TrackingStat(
                String.format("%.1f", trackingState.averageSpeed), 
                "km/h", 
                "평균속도"
            ),
            TrackingStat(
                "${trackingState.elevationGain.roundToInt()}", 
                "m", 
                "상승고도"
            ),
            TrackingStat(
                "${trackingState.currentElevation.roundToInt()}", 
                "m", 
                "현재고도"
            ),
            TrackingStat(
                "${trackingState.calories}", 
                "kcal", 
                "칼로리"
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items(stats) { stat ->
                StatCard(stat = stat)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tracking Map
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFE3F2FD),
                                Color(0xFFBBDEFB)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (trackingState.currentLocation != null) 
                            Icons.Default.MyLocation else Icons.Default.LocationSearching,
                        contentDescription = "실시간 위치",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (trackingState.currentLocation != null) 
                            "실시간 경로 추적 중" else "GPS 연결 중...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 경로 포인트 수 표시
                    if (trackingState.path.isNotEmpty()) {
                        Text(
                            text = "경로 포인트: ${trackingState.path.size}개",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tracking Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "정보",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "실시간 정보",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoItem(
                    label = "현재 속도", 
                    value = "${String.format("%.1f", trackingState.currentSpeed)} km/h"
                )
                InfoItem(
                    label = "GPS 정확도", 
                    value = "${trackingState.currentLocation?.accuracy?.roundToInt() ?: 0}m"
                )
                InfoItem(
                    label = "위치 업데이트", 
                    value = if (trackingState.currentLocation != null) "연결됨" else "연결 중..."
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Control Buttons
        val controlActions = if (trackingState.isTracking) {
            listOf(
                ControlAction(
                    if (trackingState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    if (trackingState.isPaused) "재시작" else "일시정지"
                ) { 
                    if (trackingState.isPaused) {
                        trackingViewModel.resumeTracking()
                    } else {
                        trackingViewModel.pauseTracking()
                    }
                },
                ControlAction(Icons.Default.Stop, "정지") { 
                    showStopDialog = true
                },
                ControlAction(Icons.Default.CameraAlt, "사진") { 
                    // 사진 촬영 기능
                },
                ControlAction(Icons.Default.Warning, "SOS") { 
                    // SOS 기능
                }
            )
        } else {
            listOf(
                ControlAction(Icons.Default.PlayArrow, "시작") { 
                    trackingViewModel.startTracking()
                },
                ControlAction(Icons.Default.Stop, "정지") { 
                    // 이미 정지 상태
                },
                ControlAction(Icons.Default.CameraAlt, "사진") { 
                    // 사진 촬영 기능
                },
                ControlAction(Icons.Default.Warning, "SOS") { 
                    // SOS 기능
                }
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(100.dp)
        ) {
            items(controlActions) { action ->
                ControlButton(action = action)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Permission Info
        if (!trackingViewModel.hasLocationPermission()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "⚠️ 위치 권한이 필요합니다. 설정에서 위치 권한을 허용해주세요.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
    
    // Stop Tracking Dialog
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("트래킹 종료") },
            text = { 
                Text("트래킹을 종료하시겠습니까?\n현재까지의 기록이 저장됩니다.")
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        trackingViewModel.stopTracking()
                        showStopDialog = false
                    }
                ) {
                    Text("종료")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun StatCard(stat: TrackingStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = stat.value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stat.unit,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stat.label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ControlButton(action: ControlAction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { action.onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = when (action.label) {
                    "SOS" -> MaterialTheme.colorScheme.error
                    "정지" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = action.label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 