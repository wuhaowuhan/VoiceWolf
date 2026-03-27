package com.voicewolf.app

import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import com.voicewolf.app.R
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.voicewolf.app.databinding.ActivityMainBinding
import com.voicewolf.app.databinding.DialogAddSpeechBinding
import com.voicewolf.app.databinding.DialogAddVoteBinding
import com.voicewolf.app.databinding.DialogHistoryBinding

/**
 * Main activity for Werewolf face-to-face assistant app
 * Simplified for player perspective - focuses on speech and vote recording
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: GameViewModel

    // Player views map
    private val playerViews = mutableMapOf<Int, android.view.View>()

    // Voter checkboxes for vote dialog
    private val voterCheckboxes = mutableMapOf<Int, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[GameViewModel::class.java]

        setupPlayerViews()
        setupButtons()
        observeViewModel()
    }

    private fun setupPlayerViews() {
        // Map player IDs to their views
        // Left column: 1-6
        playerViews[1] = binding.player1.root
        playerViews[2] = binding.player2.root
        playerViews[3] = binding.player3.root
        playerViews[4] = binding.player4.root
        playerViews[5] = binding.player5.root
        playerViews[6] = binding.player6.root

        // Right column: 7-12
        playerViews[7] = binding.player7.root
        playerViews[8] = binding.player8.root
        playerViews[9] = binding.player9.root
        playerViews[10] = binding.player10.root
        playerViews[11] = binding.player11.root
        playerViews[12] = binding.player12.root

        // Set player numbers and click listeners
        for ((playerId, playerView) in playerViews) {
            // Set player number
            val numberText = playerView.findViewById<TextView>(R.id.playerNumber)
            numberText?.text = playerId.toString()

            // Click listener
            playerView.setOnClickListener {
                showPlayerOptionsDialog(playerId)
            }

            // Long click to toggle alive/dead status
            playerView.setOnLongClickListener {
                val player = viewModel.getPlayerById(playerId)
                if (player != null) {
                    viewModel.setPlayerAlive(playerId, !player.isAlive)
                    updatePlayerView(playerId)
                }
                true
            }
        }
    }

    private fun setupButtons() {
        binding.btnAddSpeech.setOnClickListener {
            showAddSpeechDialog()
        }

        binding.btnAddVote.setOnClickListener {
            showAddVoteDialog()
        }

        binding.btnNextDay.setOnClickListener {
            viewModel.nextDay()
        }

        binding.btnReset.setOnClickListener {
            showResetConfirmDialog()
        }

        binding.btnMenu.setOnClickListener {
            showMenuDialog()
        }
    }

    private fun observeViewModel() {
        // Observe current day
        viewModel.currentDay.observe(this) { day ->
            binding.dayText.text = "第${day}天"
        }

        // Observe all records text
        viewModel.allRecordsText.observe(this) { text ->
            binding.allRecordsText.text = text
        }

        // Observe players
        viewModel.players.observe(this) { players ->
            players.forEach { player ->
                updatePlayerView(player.id)
            }
        }
    }

    private fun updatePlayerView(playerId: Int) {
        val playerView = playerViews[playerId] ?: return
        val player = viewModel.getPlayerById(playerId) ?: return

        val roleText = playerView.findViewById<TextView>(R.id.roleText)

        if (player.markedRole != Player.MarkedRole.NONE) {
            roleText?.visibility = android.view.View.VISIBLE
            roleText?.text = player.getMarkedRoleDisplayName()

            // Set role text background color based on role
            val bgColorRes = when (player.markedRole) {
                Player.MarkedRole.SEER -> R.color.role_seer
                Player.MarkedRole.WITCH -> R.color.role_witch
                Player.MarkedRole.HUNTER -> R.color.role_hunter
                Player.MarkedRole.GUARD -> R.color.role_guard
                Player.MarkedRole.GOOD -> R.color.role_good
                Player.MarkedRole.VILLAGER -> R.color.role_villager
                Player.MarkedRole.WEREWOLF -> R.color.role_werewolf
                Player.MarkedRole.MECHANICAL_WOLF -> R.color.role_mech_wolf
                Player.MarkedRole.NONE -> R.color.gray_light
            }
            roleText?.setBackgroundColor(resources.getColor(bgColorRes, null))
        } else {
            roleText?.visibility = android.view.View.GONE
        }
    }

    private fun showPlayerOptionsDialog(playerId: Int) {
        val player = viewModel.getPlayerById(playerId)
        val hasMarkedRole = player?.markedRole != Player.MarkedRole.NONE

        val options = if (hasMarkedRole) {
            arrayOf("记录发言", "查看投票", "标记身份", "清除身份标记")
        } else {
            arrayOf("记录发言", "查看投票", "标记身份")
        }

        AlertDialog.Builder(this)
            .setTitle("${playerId}号玩家")
            .setItems(options) { _, which ->
                when {
                    which == 0 -> showAddSpeechDialog(playerId)
                    which == 1 -> showAddVoteDialog()
                    which == 2 -> showMarkRoleDialog(playerId)
                    hasMarkedRole && which == 3 -> {
                        viewModel.setPlayerMarkedRole(playerId, Player.MarkedRole.NONE)
                        updatePlayerView(playerId)
                        Toast.makeText(this, "已清除身份标记", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showMarkRoleDialog(playerId: Int) {
        val roleOptions = arrayOf(
            "预言家",
            "好人",
            "狼人",
            "平民",
            "女巫",
            "猎人",
            "守卫",
            "机械狼"
        )

        val currentPlayer = viewModel.getPlayerById(playerId)
        val currentMarkedRole = currentPlayer?.markedRole ?: Player.MarkedRole.NONE

        // Find current selection index (-1 means no selection)
        val selectedIndex = when (currentMarkedRole) {
            Player.MarkedRole.NONE -> -1
            Player.MarkedRole.SEER -> 0
            Player.MarkedRole.GOOD -> 1
            Player.MarkedRole.WEREWOLF -> 2
            Player.MarkedRole.VILLAGER -> 3
            Player.MarkedRole.WITCH -> 4
            Player.MarkedRole.HUNTER -> 5
            Player.MarkedRole.GUARD -> 6
            Player.MarkedRole.MECHANICAL_WOLF -> 7
        }

        AlertDialog.Builder(this)
            .setTitle("标记身份 - ${playerId}号")
            .setSingleChoiceItems(roleOptions, selectedIndex) { dialog, which ->
                val markedRole = when (which) {
                    0 -> Player.MarkedRole.SEER
                    1 -> Player.MarkedRole.GOOD
                    2 -> Player.MarkedRole.WEREWOLF
                    3 -> Player.MarkedRole.VILLAGER
                    4 -> Player.MarkedRole.WITCH
                    5 -> Player.MarkedRole.HUNTER
                    6 -> Player.MarkedRole.GUARD
                    7 -> Player.MarkedRole.MECHANICAL_WOLF
                    else -> Player.MarkedRole.NONE
                }
                viewModel.setPlayerMarkedRole(playerId, markedRole)
                updatePlayerView(playerId)
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddSpeechDialog(playerId: Int? = null) {
        val dialogBinding = DialogAddSpeechBinding.inflate(layoutInflater)
        val currentDay = viewModel.currentDay.value ?: 1

        // Set player spinner
        val playerNames = (1..12).map { "${it}号" }.toTypedArray()
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, playerNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerPlayer.adapter = adapter

        // Set selected player if provided
        if (playerId != null) {
            dialogBinding.spinnerPlayer.setSelection(playerId - 1)
        }

        // Load existing record if any
        val existingRecord = playerId?.let { viewModel.getSpeechRecordForPlayer(it, currentDay) }
        if (existingRecord != null) {
            dialogBinding.editContent.setText(existingRecord.summary)
        }

        AlertDialog.Builder(this)
            .setTitle("记录发言 - 第${currentDay}天")
            .setView(dialogBinding.root)
            .setPositiveButton("保存") { _, _ ->
                val selectedPlayerId = dialogBinding.spinnerPlayer.selectedItemPosition + 1
                val content = dialogBinding.editContent.text.toString()

                if (content.isNotBlank()) {
                    viewModel.addSpeechRecord(selectedPlayerId, currentDay, content)
                    Toast.makeText(this, "发言已记录", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddVoteDialog() {
        val dialogBinding = DialogAddVoteBinding.inflate(layoutInflater)
        val currentDay = viewModel.currentDay.value ?: 1

        // Set target spinner (add abstain option)
        val targetNames = arrayOf("弃票") + (1..12).map { "${it}号" }.toTypedArray()
        val targetAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, targetNames)
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerTarget.adapter = targetAdapter

        // Create voter checkboxes in grid (4 columns, 3 rows)
        voterCheckboxes.clear()
        dialogBinding.votersGrid.removeAllViews()
        for (i in 1..12) {
            val checkBox = CheckBox(this).apply {
                text = "${i}号"
                setTextColor(resources.getColor(R.color.white, null))
                layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = androidx.gridlayout.widget.GridLayout.spec(androidx.gridlayout.widget.GridLayout.UNDEFINED, 1f)
                }
            }
            voterCheckboxes[i] = checkBox
            dialogBinding.votersGrid.addView(checkBox)
        }

        // Load existing votes for this target when target changes
        var lastTarget = -1
        dialogBinding.spinnerTarget.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val targetId = if (position == 0) 0 else position

                // Save previous selections if valid target was selected
                if (lastTarget > 0) {
                    voterCheckboxes.filter { it.value.isChecked }.keys.forEach { voterId ->
                        viewModel.recordVote(voterId, lastTarget, currentDay)
                    }
                }

                // Clear all checkboxes
                voterCheckboxes.values.forEach { it.isChecked = false }

                // Load existing voters for this target
                if (targetId > 0) {
                    val existingVotes = viewModel.getVoteRecordsForDay(currentDay)
                        .filter { it.targetId == targetId }
                    existingVotes.forEach { vote ->
                        voterCheckboxes[vote.voterId]?.isChecked = true
                    }
                }

                lastTarget = targetId
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        AlertDialog.Builder(this)
            .setTitle("记录投票 - 第${currentDay}天")
            .setView(dialogBinding.root)
            .setPositiveButton("完成") { _, _ ->
                val targetPosition = dialogBinding.spinnerTarget.selectedItemPosition
                val targetId = if (targetPosition == 0) 0 else targetPosition

                // Save votes for current target
                if (targetId > 0) {
                    voterCheckboxes.filter { it.value.isChecked }.keys.forEach { voterId ->
                        viewModel.recordVote(voterId, targetId, currentDay)
                    }
                }
                Toast.makeText(this, "投票已记录", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showHistoryDialog() {
        val dialogBinding = DialogHistoryBinding.inflate(layoutInflater)
        val allDays = viewModel.getAllDays()

        if (allDays.isEmpty()) {
            dialogBinding.historyContent.text = "暂无历史记录"
        } else {
            val sb = StringBuilder()
            allDays.forEach { day ->
                sb.append("═══ 第${day}天 ═══\n\n")

                // Speech records
                val speeches = viewModel.getSpeechRecordsForDay(day)
                if (speeches.isNotEmpty()) {
                    sb.append("【发言】\n")
                    speeches.forEach { record ->
                        sb.append("${record.playerId}号: ${record.summary}\n\n")
                    }
                }

                // Vote records
                val votes = viewModel.getVoteRecordsForDay(day)
                if (votes.isNotEmpty()) {
                    sb.append("【投票】\n")
                    votes.forEach { record ->
                        if (record.targetId == 0) {
                            sb.append("${record.voterId}号: 弃票\n")
                        } else {
                            sb.append("${record.voterId}号 → ${record.targetId}号\n")
                        }
                    }
                    sb.append("\n")

                    // Vote stats for this day
                    val voteCounts = mutableMapOf<Int, Int>()
                    votes.filter { it.targetId > 0 }.forEach {
                        voteCounts[it.targetId] = voteCounts.getOrDefault(it.targetId, 0) + 1
                    }
                    if (voteCounts.isNotEmpty()) {
                        sb.append("统计: ")
                        sb.append(voteCounts.entries.sortedByDescending { it.value }.joinToString("  ") {
                            "${it.key}号(${it.value}票)"
                        })
                        sb.append("\n")
                    }
                }

                sb.append("\n")
            }
            dialogBinding.historyContent.text = sb.toString()
        }

        AlertDialog.Builder(this)
            .setTitle("历史记录")
            .setView(dialogBinding.root)
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun showMenuDialog() {
        val options = arrayOf("查看历史", "跳转到指定天")

        AlertDialog.Builder(this)
            .setTitle("菜单")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showHistoryDialog()
                    1 -> showJumpDayDialog()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showJumpDayDialog() {
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(viewModel.currentDay.value.toString())

        AlertDialog.Builder(this)
            .setTitle("跳转到指定天")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val day = input.text.toString().toIntOrNull()
                if (day != null && day >= 1) {
                    viewModel.setDay(day)
                } else {
                    Toast.makeText(this, "请输入有效的天数", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showResetConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("确认重置")
            .setMessage("确定要重置游戏吗？所有记录将被清空。")
            .setPositiveButton("确认") { _, _ ->
                viewModel.resetGame()
                Toast.makeText(this, "游戏已重置", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}