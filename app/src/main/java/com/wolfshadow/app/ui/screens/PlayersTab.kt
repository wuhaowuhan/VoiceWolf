package com.wolfshadow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wolfshadow.app.GameViewModel
import com.wolfshadow.app.model.MarkedRole
import com.wolfshadow.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersTab(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    var showSpeechDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var selectedPlayerId by remember { mutableStateOf<Int?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部信息栏
        TopAppBar(
            title = {
                Text(
                    text = "第${gameState.currentDay}天",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            actions = {
                IconButton(onClick = { viewModel.nextDay() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "下一天",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重置",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // 玩家网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(gameState.players.size) { index ->
                val player = gameState.players[index]
                PlayerCard(
                    player = player,
                    onClick = {
                        selectedPlayerId = player.id
                        showSpeechDialog = true
                    },
                    onLongClick = {
                        selectedPlayerId = player.id
                        showProfileSheet = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // 发言记录弹窗
    if (showSpeechDialog && selectedPlayerId != null) {
        val existingRecord = viewModel.getSpeechRecord(
            day = gameState.currentDay,
            playerId = selectedPlayerId!!
        )
        SpeechDialog(
            playerId = selectedPlayerId!!,
            day = gameState.currentDay,
            existingRecord = existingRecord,
            onDismiss = {
                showSpeechDialog = false
                selectedPlayerId = null
            },
            onSave = { tags, summary ->
                viewModel.addSpeechRecord(
                    com.wolfshadow.app.model.SpeechRecord(
                        day = gameState.currentDay,
                        playerId = selectedPlayerId!!,
                        tags = tags,
                        summary = summary
                    )
                )
                showSpeechDialog = false
                selectedPlayerId = null
            }
        )
    }

    // 玩家画像
    if (showProfileSheet && selectedPlayerId != null) {
        val player = viewModel.getPlayerById(selectedPlayerId!!)
        val speechRecords = viewModel.getSpeechRecordsForPlayer(selectedPlayerId!!)
        val voteRecords = viewModel.getVoteRecordsForPlayer(selectedPlayerId!!)

        if (player != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showProfileSheet = false
                    selectedPlayerId = null
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                PlayerProfileSheet(
                    player = player,
                    speechRecords = speechRecords,
                    voteRecords = voteRecords,
                    allDays = gameState.getAllDays(),
                    onMarkRole = { role ->
                        viewModel.setPlayerMarkedRole(selectedPlayerId!!, role)
                    },
                    onToggleAlive = {
                        viewModel.setPlayerAlive(selectedPlayerId!!, !player.isAlive)
                    }
                )
            }
        }
    }

    // 重置确认对话框
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("确认重置") },
            text = { Text("确定要重置游戏吗？所有记录将被清空。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetGame()
                    showResetDialog = false
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}