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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CommunityScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { /* Write review */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "후기 작성",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("후기 작성")
            }

            Button(
                onClick = { /* Find companion */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "동행 찾기",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("동행 찾기")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "🔥 인기 게시물",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Post 1 - 후기 게시물
        PostCard(
            userInitial = "등",
            username = "등산러버",
            location = "북한산 · 2시간 전",
            content = "오늘 북한산 백운대 다녀왔어요!\n날씨도 좋고 경치가 정말 환상적... 📷",
            stats = "5.2km · 3시간 20분 · 고도 836m",
            likes = 12,
            comments = 3,
            shares = 2,
            hasImage = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Post 2 - 동행 모집
        PostCard(
            userInitial = "산",
            username = "산악인",
            location = "관악산 · 5시간 전",
            content = "📅 내일 관악산 연주대 가실 분?\n오전 9시 출발 예정입니다!",
            stats = null,
            likes = 8,
            comments = 5,
            shares = 1,
            hasImage = false,
            hasCompanionInfo = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Post 3 - 긴급상황
        PostCard(
            userInitial = "초",
            username = "초보등산",
            location = "설악산 · 1일 전",
            content = "🆘 설악산에서 길을 잃었어요...\n현재 위치 공유합니다. 도움 부탁!",
            stats = null,
            likes = 15,
            comments = 23,
            shares = 0,
            hasImage = false,
            hasCompanionInfo = false,
            isEmergency = true
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PostCard(
    userInitial: String,
    username: String,
    location: String,
    content: String,
    stats: String?,
    likes: Int,
    comments: Int,
    shares: Int,
    hasImage: Boolean,
    hasCompanionInfo: Boolean = false,
    isEmergency: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Post Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = username,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = location,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post Content
            Text(
                text = content,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Post Image
            if (hasImage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE8F5E8),
                                    Color(0xFFC8E6C9)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[등산로 사진]",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Companion Info
            if (hasCompanionInfo) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E8)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "📅 동행 모집",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("일시: 5월 23일 오전 9시", fontSize = 14.sp)
                        Text("장소: 관악산 연주대 코스", fontSize = 14.sp)
                        Text("난이도: 중급", fontSize = 14.sp)
                        Text("모집인원: 2/4명", fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Emergency Info
            if (isEmergency) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "📍 실시간 위치 공유 중",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Stats
            if (stats != null) {
                Text(
                    text = stats,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Post Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ActionButton(
                        icon = Icons.Default.Favorite,
                        text = likes.toString(),
                        color = if (isEmergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ActionButton(
                        icon = Icons.Default.ChatBubbleOutline,
                        text = comments.toString()
                    )
                    if (shares > 0) {
                        ActionButton(
                            icon = Icons.Default.Share,
                            text = shares.toString()
                        )
                    }
                }

                if (hasCompanionInfo) {
                    Button(
                        onClick = { /* Join companion */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("참여하기", fontSize = 12.sp)
                    }
                } else if (isEmergency) {
                    Button(
                        onClick = { /* Call for rescue */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "구조요청",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("구조요청", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = color,
            fontSize = 14.sp
        )
    }
} 