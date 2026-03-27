package com.wolfshadow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.wolfshadow.app.model.SpeechRecord
import com.wolfshadow.app.model.SpeechTag
import com.wolfshadow.app.ui.theme.*

@Composable
fun SpeechDialog(
    playerId: Int,
    day: Int,
    existingRecord: SpeechRecord? = null,
    onDismiss: () -> Unit,
    onSave: (List<SpeechTag>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTags by remember { mutableStateOf(existingRecord?.tags ?: emptyList()) }
    var summary by remember { mutableStateOf(existingRecord?.summary ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "记录发言 - ${playerId}号 - 第${day}天",
                color = White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SpeechTagSelector(
                    selectedTags = selectedTags,
                    onTagsChanged = { selectedTags = it }
                )

                Column {
                    Text(
                        text = "摘要（可选）：",
                        style = MaterialTheme.typography.labelLarge,
                        color = White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .background(InfoPanel, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = White),
                        cursorBrush = SolidColor(White)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedTags, summary) }) {
                Text("保存", color = SecondaryDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = GrayLight)
            }
        }
    )
}