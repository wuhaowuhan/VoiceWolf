package com.wolfshadow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wolfshadow.app.GameViewModel

@Composable
fun MainScreen(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("玩家") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Default.ThumbUp, contentDescription = null) },
                    label = { Text("投票图") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("历史") }
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTabIndex) {
                0 -> PlayersTab(viewModel = viewModel)
                1 -> VoteGraphTab(viewModel = viewModel)
                2 -> HistoryTab(viewModel = viewModel)
            }
        }
    }
}