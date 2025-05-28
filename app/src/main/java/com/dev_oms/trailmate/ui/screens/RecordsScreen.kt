package com.dev_oms.trailmate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dev_oms.trailmate.viewmodel.RecordsViewModel
import com.dev_oms.trailmate.viewmodel.MonthlyStat
import com.dev_oms.trailmate.viewmodel.HikingRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    recordsViewModel: RecordsViewModel = viewModel()
) {
    val monthlyStats by recordsViewModel.monthlyStats.collectAsState()
    val growthPercentage by recordsViewModel.growthPercentage.collectAsState()
    val hikingRecords by recordsViewModel.hikingRecords.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header with refresh button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊 내 등산 기록",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            IconButton(
                onClick = { recordsViewModel.refreshData() }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "새로고침",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Monthly Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "🏔️ 이번 달 통계",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    monthlyStats.forEach { stat ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stat.value,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stat.label,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "📈 전월 대비 +${growthPercentage}% ↗️",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "📅 최근 등산 기록",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hiking Records
        if (hikingRecords.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🥾",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "아직 등산 기록이 없습니다",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "첫 등산을 시작해보세요!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            hikingRecords.forEach { record ->
                HikingRecordCard(
                    record = record,
                    onClick = { /* Navigate to detail */ }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // More Stats Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* Show more stats */ },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "📊 더 많은 통계 보기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Achievement Section
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🏅 획득한 배지",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AchievementBadge("🥾", "첫걸음")
                    AchievementBadge("🏔️", "산악인")
                    AchievementBadge("📈", "성장")
                    AchievementBadge("⏰", "꾸준함")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun HikingRecordCard(
    record: HikingRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4CAF50),
                                Color(0xFF81C784)
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = record.emoji,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = record.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${record.date} · ${record.duration} · ${record.distance}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (record.completed) {
                        Text(
                            text = "완주 ✅",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = record.rating,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementBadge(
    emoji: String,
    name: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(50.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Color(0xFFE8F5E8),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 20.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
} 