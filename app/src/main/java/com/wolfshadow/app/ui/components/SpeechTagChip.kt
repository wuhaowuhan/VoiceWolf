package com.wolfshadow.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wolfshadow.app.model.SpeechTag
import com.wolfshadow.app.ui.theme.SecondaryDark
import com.wolfshadow.app.ui.theme.White

@Composable
fun SpeechTagSelector(
    selectedTags: List<SpeechTag>,
    onTagsChanged: (List<SpeechTag>) -> Unit,
    modifier: Modifier = Modifier
) {
    var tags by remember(selectedTags) { mutableStateOf(selectedTags.toMutableList()) }

    Column(modifier = modifier) {
        Text(
            text = "快捷标签：",
            style = MaterialTheme.typography.labelLarge,
            color = White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SpeechTag.entries) { tag ->
                val isSelected = tags.contains(tag)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        tags = if (isSelected) {
                            (tags - tag).toMutableList()
                        } else {
                            (tags + tag).toMutableList()
                        }
                        onTagsChanged(tags)
                    },
                    label = {
                        Text(
                            text = tag.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SecondaryDark,
                        selectedLabelColor = White
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}