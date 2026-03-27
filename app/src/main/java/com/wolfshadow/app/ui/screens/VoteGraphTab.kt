package com.wolfshadow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wolfshadow.app.GameViewModel
import com.wolfshadow.app.ui.components.VoteGraphView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoteGraphTab(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    var selectedDay by remember(gameState.currentDay) { mutableStateOf(gameState.currentDay) }
    var selectedPlayerId by remember { mutableStateOf<Int?>(null) }

    val allDays = gameState.getAllDays()
    val displayDay = if (allDays.contains(selectedDay)) selectedDay else gameState.currentDay
    val voteRecords = viewModel.getVoteRecordsForDay(displayDay)

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部栏
        TopAppBar(
            title = {
                Text("投票关系图")
            },
            actions = {
                // 天数选择
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (selectedDay > 1) selectedDay--
                        },
                        enabled = selectedDay > 1
                    ) {
                        Icon(Icons.Default.ArrowBack, "前一天")
                    }

                    Text(
                        text = "第${displayDay}天",
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(
                        onClick = {
                            if (selectedDay < gameState.currentDay) selectedDay++
                        },
                        enabled = selectedDay < gameState.currentDay
                    ) {
                        Icon(Icons.Default.ArrowForward, "后一天")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // 投票关系图
        if (voteRecords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "第${displayDay}天暂无投票记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            VoteGraphView(
                players = gameState.players,
                voteRecords = voteRecords,
                selectedPlayerId = selectedPlayerId,
                onPlayerClick = { playerId ->
                    selectedPlayerId = if (selectedPlayerId == playerId) null else playerId
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}