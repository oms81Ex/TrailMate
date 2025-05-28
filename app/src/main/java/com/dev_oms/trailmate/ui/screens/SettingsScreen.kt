package com.dev_oms.trailmate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SettingItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val value: String? = null,
    val hasSwitch: Boolean = false,
    val switchValue: Boolean = false,
    val hasArrow: Boolean = true,
    val onClick: () -> Unit = {}
)

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // User Profile
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "프로필",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "김등산",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "kim.hiking@email.com",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "등산 경력: 중급 (2년)",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings Sections
        SettingSection(
            title = "🎯 트래킹 설정",
            items = listOf(
                SettingItem(
                    title = "GPS 정확도",
                    value = "고정밀",
                    icon = Icons.Default.GpsFixed
                ),
                SettingItem(
                    title = "위치 업데이트 주기",
                    value = "5초",
                    icon = Icons.Default.Update
                ),
                SettingItem(
                    title = "자동 일시정지",
                    icon = Icons.Default.PauseCircle,
                    hasSwitch = true,
                    switchValue = true,
                    hasArrow = false
                ),
                SettingItem(
                    title = "음성 안내",
                    icon = Icons.Default.VolumeUp,
                    hasSwitch = true,
                    switchValue = true,
                    hasArrow = false
                )
            )
        )

        SettingSection(
            title = "🔔 알림 설정",
            items = listOf(
                SettingItem(
                    title = "위험 지역 경고",
                    icon = Icons.Default.Warning,
                    hasSwitch = true,
                    switchValue = true,
                    hasArrow = false
                ),
                SettingItem(
                    title = "날씨 변화 알림",
                    icon = Icons.Default.Cloud,
                    hasSwitch = true,
                    switchValue = true,
                    hasArrow = false
                ),
                SettingItem(
                    title = "목표 달성 알림",
                    icon = Icons.Default.Flag,
                    hasSwitch = true,
                    switchValue = true,
                    hasArrow = false
                ),
                SettingItem(
                    title = "커뮤니티 알림",
                    icon = Icons.Default.Group,
                    hasSwitch = true,
                    switchValue = false,
                    hasArrow = false
                )
            )
        )

        SettingSection(
            title = "🛡️ 안전 설정",
            items = listOf(
                SettingItem(
                    title = "비상연락처 (2명)",
                    subtitle = "설정 완료",
                    icon = Icons.Default.ContactPhone
                ),
                SettingItem(
                    title = "SOS 자동 발송",
                    icon = Icons.Default.Emergency,
                    hasSwitch = true,
                    switchValue = true,
                    hasArrow = false
                ),
                SettingItem(
                    title = "위치 공유 설정",
                    value = "친구만",
                    icon = Icons.Default.Share
                )
            )
        )

        SettingSection(
            title = "📱 앱 설정",
            items = listOf(
                SettingItem(
                    title = "다크 모드",
                    icon = Icons.Default.DarkMode,
                    hasSwitch = true,
                    switchValue = false,
                    hasArrow = false
                ),
                SettingItem(
                    title = "언어",
                    value = "한국어",
                    icon = Icons.Default.Language
                ),
                SettingItem(
                    title = "단위",
                    value = "미터법",
                    icon = Icons.Default.Straighten
                ),
                SettingItem(
                    title = "오프라인 지도",
                    subtitle = "관리",
                    icon = Icons.Default.Map
                )
            )
        )

        SettingSection(
            title = "🔒 개인정보",
            items = listOf(
                SettingItem(
                    title = "개인정보 처리방침",
                    subtitle = "보기",
                    icon = Icons.Default.PrivacyTip
                ),
                SettingItem(
                    title = "위치정보 이용약관",
                    subtitle = "보기",
                    icon = Icons.Default.LocationOn
                ),
                SettingItem(
                    title = "데이터 다운로드",
                    subtitle = "요청",
                    icon = Icons.Default.Download
                ),
                SettingItem(
                    title = "계정 삭제",
                    icon = Icons.Default.Delete
                )
            )
        )

        SettingSection(
            title = "ℹ️ 앱 정보",
            items = listOf(
                SettingItem(
                    title = "버전",
                    value = "1.0.0",
                    icon = Icons.Default.Info,
                    hasArrow = false
                ),
                SettingItem(
                    title = "고객센터",
                    value = "1588-1234",
                    icon = Icons.Default.Phone
                ),
                SettingItem(
                    title = "오픈소스 라이선스",
                    subtitle = "보기",
                    icon = Icons.Default.Code
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingSection(
    title: String,
    items: List<SettingItem>
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingItemRow(item = item)
                    if (index < items.size - 1) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingItemRow(item: SettingItem) {
    var switchState by remember { mutableStateOf(item.switchValue) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (!item.hasSwitch) {
                    item.onClick()
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            item.hasSwitch -> {
                Switch(
                    checked = switchState,
                    onCheckedChange = { switchState = it }
                )
            }
            item.value != null -> {
                Text(
                    text = item.value,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (item.hasArrow) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "이동",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
} 