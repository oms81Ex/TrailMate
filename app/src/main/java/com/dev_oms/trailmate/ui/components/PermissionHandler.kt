package com.dev_oms.trailmate.ui.components

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionHandler(
    onPermissionGranted: () -> Unit,
    content: @Composable () -> Unit
) {
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    when {
        locationPermissions.allPermissionsGranted -> {
            LaunchedEffect(Unit) {
                onPermissionGranted()
            }
            content()
        }
        locationPermissions.shouldShowRationale -> {
            PermissionRationaleDialog(
                onRequestPermission = { locationPermissions.launchMultiplePermissionRequest() }
            )
        }
        else -> {
            PermissionRequestDialog(
                onRequestPermission = { locationPermissions.launchMultiplePermissionRequest() }
            )
        }
    }
}

@Composable
fun PermissionRationaleDialog(
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = "위치 권한",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "위치 권한이 필요합니다",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "등산 트래킹 기능을 사용하기 위해서는 위치 권한이 필요합니다. 현재 위치를 기반으로 거리, 속도, 고도 등을 측정합니다.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("권한 허용하기")
            }
        }
    }
}

@Composable
fun PermissionRequestDialog(
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = "위치 권한",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "PeakPal과 함께 등산하세요! 🏔️",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "정확한 등산 기록을 위해 위치 권한이 필요합니다.\n\n• 실시간 위치 추적\n• 이동 거리 측정\n• 고도 변화 기록\n• 속도 계산",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("시작하기")
            }
        }
    }
} 