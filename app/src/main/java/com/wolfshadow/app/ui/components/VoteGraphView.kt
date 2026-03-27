package com.wolfshadow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wolfshadow.app.model.*
import com.wolfshadow.app.ui.theme.*

@Composable
fun VoteGraphView(
    players: List<Player>,
    voteRecords: List<VoteRecord>,
    selectedPlayerId: Int? = null,
    onPlayerClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 构建投票关系映射: targetId -> voters
    val votesByTarget = remember(voteRecords) {
        voteRecords
            .filter { !it.isAbstain() }
            .groupBy { it.targetId }
    }

    // 构建投票关系映射: voterId -> targetId
    val votesByVoter = remember(voteRecords) {
        voteRecords.associate { it.voterId to it.targetId }
    }

    Column(modifier = modifier) {
        // 投票网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(players.size) { index ->
                val player = players[index]
                val votesReceived = votesByTarget[player.id]?.size ?: 0
                val votedFor = votesByVoter[player.id]
                val isSelected = selectedPlayerId == player.id

                VoteNodeCard(
                    player = player,
                    votedFor = votedFor,
                    votesReceived = votesReceived,
                    isSelected = isSelected,
                    onClick = { onPlayerClick(player.id) }
                )
            }
        }

        // 投票统计
        if (voteRecords.isNotEmpty()) {
            VoteStatistics(
                voteRecords = voteRecords,
                players = players,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun VoteNodeCard(
    player: Player,
    votedFor: Int?,
    votesReceived: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (player.markedRole) {
        MarkedRole.SEER -> RoleSeer
        MarkedRole.WITCH -> RoleWitch
        MarkedRole.HUNTER -> RoleHunter
        MarkedRole.GUARD -> RoleGuard
        MarkedRole.GOOD -> RoleGood
        MarkedRole.VILLAGER -> RoleVillager
        MarkedRole.WEREWOLF -> RoleWerewolf
        MarkedRole.MECHANICAL_WOLF -> RoleMechWolf
        MarkedRole.NONE -> InfoPanel
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .then(
                if (!player.isAlive) {
                    Modifier.alpha(0.5f)
                } else {
                    Modifier
                }
            )
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, SecondaryDark, RoundedCornerShape(8.dp))
                } else {
                    Modifier.border(1.dp, GrayDark, RoundedCornerShape(8.dp))
                }
            )
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${player.id}号",
                style = MaterialTheme.typography.titleMedium,
                color = if (player.isAlive) White else GrayLight,
                textDecoration = if (!player.isAlive) TextDecoration.LineThrough else null
            )

            if (player.markedRole != MarkedRole.NONE) {
                Text(
                    text = player.markedRole.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 投票信息
            if (votedFor != null && votedFor > 0) {
                Text(
                    text = "→${votedFor}号",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryDark
                )
            }

            // 票数
            if (votesReceived > 0) {
                Text(
                    text = "${votesReceived}票",
                    style = MaterialTheme.typography.labelSmall,
                    color = White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun VoteStatistics(
    voteRecords: List<VoteRecord>,
    players: List<Player>,
    modifier: Modifier = Modifier
) {
    val validVotes = voteRecords.filter { !it.isAbstain() }
    val abstainVotes = voteRecords.filter { it.isAbstain() }

    // 票数统计
    val voteCounts = validVotes.groupingBy { it.targetId }.eachCount()
        .entries.sortedByDescending { it.value }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(InfoPanel, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "投票统计",
            style = MaterialTheme.typography.labelLarge,
            color = White
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (voteCounts.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                voteCounts.forEach { (targetId, count) ->
                    Text(
                        text = "${targetId}号(${count}票)",
                        style = MaterialTheme.typography.labelMedium,
                        color = SecondaryDark
                    )
                }
            }
        }

        if (abstainVotes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "弃票: ${abstainVotes.joinToString(", ") { "${it.voterId}号" }}",
                style = MaterialTheme.typography.labelMedium,
                color = GrayLight
            )
        }
    }
}