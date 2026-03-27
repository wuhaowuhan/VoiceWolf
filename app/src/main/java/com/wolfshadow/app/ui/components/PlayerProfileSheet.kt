package com.wolfshadow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wolfshadow.app.model.*
import com.wolfshadow.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileSheet(
    player: Player,
    speechRecords: List<SpeechRecord>,
    voteRecords: List<VoteRecord>,
    allDays: List<Int>,
    onMarkRole: (MarkedRole) -> Unit,
    onToggleAlive: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRoleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 头部信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${player.id}号玩家",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (player.isAlive) White else GrayLight,
                    textDecoration = if (!player.isAlive) TextDecoration.LineThrough else null
                )
                if (player.markedRole != MarkedRole.NONE) {
                    Text(
                        text = "身份标记：${player.markedRole.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = White.copy(alpha = 0.7f)
                    )
                }
                if (!player.isAlive) {
                    Text(
                        text = "已出局",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusDead
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showRoleDialog = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("标记身份", color = White)
                }
                OutlinedButton(
                    onClick = onToggleAlive,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (player.isAlive) "标记出局" else "复活",
                        color = if (player.isAlive) StatusDead else StatusAlive
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = White.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        // 历史记录
        if (allDays.isEmpty()) {
            Text(
                text = "暂无记录",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayLight,
                modifier = Modifier.padding(vertical = 32.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allDays) { day ->
                    DayRecordCard(
                        day = day,
                        speechRecord = speechRecords.find { it.day == day },
                        voteRecord = voteRecords.find { it.day == day }
                    )
                }
            }
        }
    }

    // 身份标记对话框
    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            containerColor = SurfaceDark,
            title = { Text("标记身份", color = White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarkedRole.entries.filter { it != MarkedRole.NONE }.forEach { role ->
                        TextButton(
                            onClick = {
                                onMarkRole(role)
                                showRoleDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = role.displayName,
                                color = White,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            onMarkRole(MarkedRole.NONE)
                            showRoleDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("清除标记", color = GrayLight)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleDialog = false }) {
                    Text("取消", color = GrayLight)
                }
            }
        )
    }
}

@Composable
private fun DayRecordCard(
    day: Int,
    speechRecord: SpeechRecord?,
    voteRecord: VoteRecord?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InfoPanel, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "第${day}天",
            style = MaterialTheme.typography.labelLarge,
            color = SecondaryDark
        )
        Spacer(modifier = Modifier.height(8.dp))

        speechRecord?.let { record ->
            Row {
                Text(
                    text = "发言：",
                    style = MaterialTheme.typography.labelMedium,
                    color = GrayLight
                )
                Text(
                    text = record.formatDisplay(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = White
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        voteRecord?.let { record ->
            Row {
                Text(
                    text = "投票：",
                    style = MaterialTheme.typography.labelMedium,
                    color = GrayLight
                )
                Text(
                    text = if (record.isAbstain()) "弃票" else "投给${record.targetId}号",
                    style = MaterialTheme.typography.bodyMedium,
                    color = White
                )
            }
        }

        if (speechRecord == null && voteRecord == null) {
            Text(
                text = "暂无记录",
                style = MaterialTheme.typography.bodySmall,
                color = GrayLight
            )
        }
    }
}