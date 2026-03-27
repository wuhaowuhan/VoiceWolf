package com.wolfshadow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wolfshadow.app.GameViewModel
import com.wolfshadow.app.ui.theme.InfoPanel
import com.wolfshadow.app.ui.theme.SecondaryDark
import com.wolfshadow.app.ui.theme.White
import com.wolfshadow.app.ui.theme.GrayLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTab(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    val allDays = gameState.getAllDays()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("历史记录") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (allDays.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "暂无历史记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(allDays) { day ->
                    DayHistoryCard(
                        day = day,
                        speechRecords = viewModel.getSpeechRecordsForDay(day),
                        voteRecords = viewModel.getVoteRecordsForDay(day)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHistoryCard(
    day: Int,
    speechRecords: List<com.wolfshadow.app.model.SpeechRecord>,
    voteRecords: List<com.wolfshadow.app.model.VoteRecord>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InfoPanel, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "第${day}天",
            style = MaterialTheme.typography.titleMedium,
            color = SecondaryDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 发言记录
        if (speechRecords.isNotEmpty()) {
            Text(
                text = "【发言】",
                style = MaterialTheme.typography.labelLarge,
                color = White
            )
            Spacer(modifier = Modifier.height(8.dp))
            speechRecords.forEach { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "${record.playerId}号：",
                        style = MaterialTheme.typography.labelMedium,
                        color = SecondaryDark,
                        modifier = Modifier.widthIn(min = 40.dp)
                    )
                    Text(
                        text = record.formatDisplay(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = White
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 投票记录
        if (voteRecords.isNotEmpty()) {
            Text(
                text = "【投票】",
                style = MaterialTheme.typography.labelLarge,
                color = White
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 投票详情
            val validVotes = voteRecords.filter { !it.isAbstain() }
            val abstainVotes = voteRecords.filter { it.isAbstain() }

            if (validVotes.isNotEmpty()) {
                Text(
                    text = validVotes.joinToString("  ") {
                        "${it.voterId}→${it.targetId}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = White
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 投票统计
            val voteCounts = validVotes.groupingBy { it.targetId }.eachCount()
            if (voteCounts.isNotEmpty()) {
                Text(
                    text = "统计：" + voteCounts.entries
                        .sortedByDescending { it.value }
                        .joinToString("  ") { "${it.key}号(${it.value}票)" },
                    style = MaterialTheme.typography.labelMedium,
                    color = GrayLight
                )
            }

            if (abstainVotes.isNotEmpty()) {
                Text(
                    text = "弃票：" + abstainVotes.joinToString(", ") { "${it.voterId}号" },
                    style = MaterialTheme.typography.labelMedium,
                    color = GrayLight
                )
            }
        }
    }
}