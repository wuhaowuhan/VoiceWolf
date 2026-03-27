
package com.wolfshadow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wolfshadow.app.model.MarkedRole
import com.wolfshadow.app.model.Player
import com.wolfshadow.app.ui.theme.*

@Composable
fun PlayerCard(
    player: Player,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (!player.isAlive) {
                    Modifier.alpha(0.5f)
                } else {
                    Modifier
                }
            )
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(
                width = if (player.markedRole != MarkedRole.NONE) 2.dp else 1.dp,
                color = if (player.markedRole != MarkedRole.NONE) White.copy(alpha = 0.3f) else GrayDark,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 玩家编号
            Text(
                text = "${player.id}号",
                style = MaterialTheme.typography.titleMedium,
                color = if (player.isAlive) White else GrayLight,
                textDecoration = if (!player.isAlive) TextDecoration.LineThrough else null
            )

            // 身份标记
            if (player.markedRole != MarkedRole.NONE) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = player.markedRole.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 死亡标记
            if (!player.isAlive) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "出局",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusDead
                )
            }
        }
    }
}