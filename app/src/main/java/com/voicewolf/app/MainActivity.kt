package com.voicewolf.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.voicewolf.app.databinding.ActivityMainBinding
import com.voicewolf.app.databinding.DialogAddSpeechBinding
import com.voicewolf.app.databinding.DialogAddVoteBinding
import com.voicewolf.app.databinding.DialogHistoryBinding
import com.voicewolf.app.databinding.DialogSelectCheckBinding
import com.voicewolf.app.databinding.DialogSelectNumberBinding
import com.voicewolf.app.databinding.DialogSelectNumbersMultiBinding
import com.voicewolf.app.databinding.ItemPlayerBinding

/**
 * Main activity for Werewolf face-to-face assistant app
 * Supports dynamic player count (8-15 players)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: GameViewModel

    // Left column player views (1-6, fixed)
    private val leftPlayerBindings = mutableMapOf<Int, ItemPlayerBinding>()

    // Right column player views (7-N, dynamic)
    private val rightPlayerViews = mutableMapOf<Int, View>()

    // Voter checkboxes for vote dialog
    private val voterCheckboxes = mutableMapOf<Int, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[GameViewModel::class.java]

        setupLeftColumnPlayers()
        setupButtons()
        observeViewModel()
    }

    private fun setupLeftColumnPlayers() {
        // Setup fixed left column players (1-6)
        leftPlayerBindings[1] = binding.player1
        leftPlayerBindings[2] = binding.player2
        leftPlayerBindings[3] = binding.player3
        leftPlayerBindings[4] = binding.player4
        leftPlayerBindings[5] = binding.player5
        leftPlayerBindings[6] = binding.player6

        for ((playerId, playerBinding) in leftPlayerBindings) {
            setupPlayerView(playerBinding.root, playerId)
        }
    }

    private fun setupPlayerView(playerView: View, playerId: Int) {
        val binding = ItemPlayerBinding.bind(playerView)

        // Set player number
        binding.playerNumber.text = playerId.toString()

        // Setup pencil button for quick role marking
        binding.btnMarkRole.setOnClickListener {
            showMarkRoleDialog(playerId)
        }

        // Setup remove button (only visible for players 13-15 when game not started)
        binding.btnRemove.setOnClickListener {
            showRemovePlayerConfirmDialog(playerId)
        }

        // Click listener for options menu
        playerView.setOnClickListener {
            showPlayerOptionsDialog(playerId)
        }

        // Long click to toggle alive/dead status
        playerView.setOnLongClickListener {
            val player = viewModel.getPlayerById(playerId)
            if (player != null && player.isActive) {
                viewModel.setPlayerAlive(playerId, !player.isAlive)
                updatePlayerView(playerId)
            }
            true
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

        binding.btnAddPlayer.setOnClickListener {
            if (viewModel.addPlayer()) {
                Toast.makeText(this, "已添加玩家", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "无法添加玩家（已达上限或游戏已开始）", Toast.LENGTH_SHORT).show()
            }
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

        // Observe active player IDs - rebuild right column dynamically
        viewModel.activePlayerIds.observe(this) { activeIds ->
            rebuildRightColumn(activeIds)
            updateAddButtonVisibility()
            updateAllPlayerViews()
        }

        // Observe players for individual updates
        viewModel.players.observe(this) { players ->
            players.forEach { player ->
                if (player.isActive) {
                    updatePlayerView(player.id)
                }
            }
        }
    }

    private fun rebuildRightColumn(activeIds: Set<Int>) {
        val container = binding.rightColumnContainer

        // Remove all dynamic player views (keep the add button)
        rightPlayerViews.values.forEach { container.removeView(it) }
        rightPlayerViews.clear()

        // Add player views for IDs >= 7
        activeIds.filter { it >= 7 }.sorted().forEach { playerId ->
            val playerView = createPlayerView(playerId)
            container.addView(playerView, container.childCount - 1) // Add before add button
            rightPlayerViews[playerId] = playerView
        }
    }

    private fun createPlayerView(playerId: Int): View {
        val binding = ItemPlayerBinding.inflate(layoutInflater)

        // Set layout params with fixed size and margin (same as left column players)
        val density = resources.displayMetrics.density
        val sizePx = (60 * density).toInt()
        val marginPx = (4 * density).toInt()
        binding.root.layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
            setMargins(marginPx, marginPx, marginPx, marginPx)
        }

        // Set player number
        binding.playerNumber.text = playerId.toString()

        // Setup pencil button for quick role marking
        binding.btnMarkRole.setOnClickListener {
            showMarkRoleDialog(playerId)
        }

        // Setup remove button (only visible for players 13-15 when game not started)
        binding.btnRemove.setOnClickListener {
            showRemovePlayerConfirmDialog(playerId)
        }

        // Click listener for options menu
        binding.root.setOnClickListener {
            showPlayerOptionsDialog(playerId)
        }

        // Long click to toggle alive/dead status
        binding.root.setOnLongClickListener {
            val player = viewModel.getPlayerById(playerId)
            if (player != null && player.isActive) {
                viewModel.setPlayerAlive(playerId, !player.isAlive)
                updatePlayerView(playerId)
            }
            true
        }

        return binding.root
    }

    private fun updateAddButtonVisibility() {
        val gameStarted = viewModel.hasGameStarted()
        val playerCount = viewModel.getActivePlayerCount()
        binding.btnAddPlayer.visibility = if (!gameStarted && playerCount < GameViewModel.MAX_PLAYERS) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun updateAllPlayerViews() {
        // Update left column
        leftPlayerBindings.keys.forEach { playerId ->
            updatePlayerView(playerId)
        }
        // Update right column
        rightPlayerViews.keys.forEach { playerId ->
            updatePlayerView(playerId)
        }
    }

    private fun updatePlayerView(playerId: Int) {
        val player = viewModel.getPlayerById(playerId)
        if (player == null || !player.isActive) return

        // Find the view
        val playerView = leftPlayerBindings[playerId]?.root ?: rightPlayerViews[playerId] ?: return
        val binding = ItemPlayerBinding.bind(playerView)

        // Update role text badge
        if (player.markedRole != Player.MarkedRole.NONE) {
            binding.roleText.visibility = View.VISIBLE
            binding.roleText.text = player.getMarkedRoleDisplayName()

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
            binding.roleText.setBackgroundColor(ContextCompat.getColor(this, bgColorRes))
        } else {
            binding.roleText.visibility = View.GONE
        }

        // Update border color based on marked role faction
        val cardView = playerView as MaterialCardView
        val borderColor = when {
            player.isMarkedEvil() -> ContextCompat.getColor(this, R.color.border_werewolf)
            player.markedRole != Player.MarkedRole.NONE -> ContextCompat.getColor(this, R.color.border_good)
            else -> ContextCompat.getColor(this, R.color.border_default)
        }
        cardView.strokeColor = borderColor

        // Update dead overlay
        binding.deadOverlay.visibility = if (!player.isAlive) View.VISIBLE else View.GONE
        // Update remove button visibility (only for 13-15, game not started)
        binding.btnRemove.visibility = if (playerId >= 13 && !viewModel.hasGameStarted()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun showRemovePlayerConfirmDialog(playerId: Int) {
        AlertDialog.Builder(this)
            .setTitle("移除玩家")
            .setMessage("确定要移除${playerId}号玩家吗？")
            .setPositiveButton("确认") { _, _ ->
                if (viewModel.removePlayer(playerId)) {
                    Toast.makeText(this, "已移除${playerId}号玩家", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "无法移除该玩家", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPlayerOptionsDialog(playerId: Int) {
        val player = viewModel.getPlayerById(playerId)
        if (player == null || !player.isActive) return

        val hasMarkedRole = player.markedRole != Player.MarkedRole.NONE

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
        val roleOptionsGood = arrayOf("预言家", "女巫", "猎人", "守卫", "好人", "平民")
        val roleOptionsEvil = arrayOf("狼人", "机械狼")

        val currentPlayer = viewModel.getPlayerById(playerId)
        val currentMarkedRole = currentPlayer?.markedRole ?: Player.MarkedRole.NONE

        AlertDialog.Builder(this)
            .setTitle("标记身份 - ${playerId}号")
            .setItems(roleOptionsGood + roleOptionsEvil) { _, which ->
                val markedRole = when (which) {
                    0 -> Player.MarkedRole.SEER
                    1 -> Player.MarkedRole.WITCH
                    2 -> Player.MarkedRole.HUNTER
                    3 -> Player.MarkedRole.GUARD
                    4 -> Player.MarkedRole.GOOD
                    5 -> Player.MarkedRole.VILLAGER
                    6 -> Player.MarkedRole.WEREWOLF
                    7 -> Player.MarkedRole.MECHANICAL_WOLF
                    else -> Player.MarkedRole.NONE
                }
                viewModel.setPlayerMarkedRole(playerId, markedRole)
                updatePlayerView(playerId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddSpeechDialog(playerId: Int? = null) {
        val dialogBinding = DialogAddSpeechBinding.inflate(layoutInflater)
        val currentDay = viewModel.currentDay.value ?: 1

        // Get active players for dynamic spinner
        val activePlayers = viewModel.getActivePlayers()
        val playerNames = activePlayers.map { "${it.id}号" }.toTypedArray()
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, playerNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerPlayer.adapter = adapter

        // Set selected player if provided
        if (playerId != null) {
            val index = activePlayers.indexOfFirst { it.id == playerId }
            if (index >= 0) {
                dialogBinding.spinnerPlayer.setSelection(index)
            }
        }

        // Load existing record if any
        val selectedPlayerId = playerId ?: activePlayers.firstOrNull()?.id
        val existingRecord = selectedPlayerId?.let { viewModel.getSpeechRecordForPlayer(it, currentDay) }
        if (existingRecord != null) {
            dialogBinding.editContent.setText(existingRecord.summary)
        }

        // Setup template button click listeners
        // Direct insert templates
        dialogBinding.btnTemplateSeer.setOnClickListener {
            appendText(dialogBinding.editContent, "跳预言家 ")
        }
        dialogBinding.btnTemplateWitch.setOnClickListener {
            appendText(dialogBinding.editContent, "跳女巫 ")
        }
        dialogBinding.btnTemplateGuard.setOnClickListener {
            appendText(dialogBinding.editContent, "跳守卫 ")
        }
        dialogBinding.btnTemplateVillager.setOnClickListener {
            appendText(dialogBinding.editContent, "跳平民 ")
        }

        // Single number selection templates
        dialogBinding.btnTemplateSilver.setOnClickListener {
            showSingleNumberSelectDialog("银水") { number ->
                appendText(dialogBinding.editContent, "银水${number}号 ")
            }
        }
        dialogBinding.btnTemplatePoison.setOnClickListener {
            showSingleNumberSelectDialog("毒") { number ->
                appendText(dialogBinding.editContent, "毒${number}号 ")
            }
        }
        dialogBinding.btnTemplateShield.setOnClickListener {
            showSingleNumberSelectDialog("盾") { number ->
                appendText(dialogBinding.editContent, "盾${number}号 ")
            }
        }

        // Check template (number + identity)
        dialogBinding.btnTemplateCheck.setOnClickListener {
            showCheckSelectDialog { number, isWerewolf ->
                val result = if (isWerewolf) "狼" else "好人"
                appendText(dialogBinding.editContent, "查验${number}号 $result ")
            }
        }

        // Multi-select templates
        dialogBinding.btnTemplateBadgeFlow.setOnClickListener {
            showMultiNumberSelectDialog("警徽流") { numbers ->
                appendText(dialogBinding.editContent, "警徽流 ${numbers.joinToString(" ") { "${it}号" }} ")
            }
        }
        dialogBinding.btnTemplateProtect.setOnClickListener {
            showMultiNumberSelectDialog("保") { numbers ->
                appendText(dialogBinding.editContent, "保${numbers.joinToString("、") { "${it}号" }} ")
            }
        }
        dialogBinding.btnTemplateAttack.setOnClickListener {
            showMultiNumberSelectDialog("踩") { numbers ->
                appendText(dialogBinding.editContent, "踩${numbers.joinToString("、") { "${it}号" }} ")
            }
        }
        dialogBinding.btnTemplateWolfAt.setOnClickListener {
            showMultiNumberSelectDialog("狼在") { numbers ->
                appendText(dialogBinding.editContent, "觉得狼在${numbers.joinToString("、") { "${it}号" }} ")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("记录发言 - 第${currentDay}天")
            .setView(dialogBinding.root)
            .setPositiveButton("保存") { _, _ ->
                val selectedIndex = dialogBinding.spinnerPlayer.selectedItemPosition
                val selectedPlayer = activePlayers[selectedIndex]
                val content = dialogBinding.editContent.text.toString()

                if (content.isNotBlank()) {
                    viewModel.addSpeechRecord(selectedPlayer.id, currentDay, content)
                    Toast.makeText(this, "发言已记录", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun appendText(editText: android.widget.EditText, text: String) {
        val currentText = editText.text.toString()
        editText.setText(currentText + text)
        editText.setSelection(editText.text.length) // Move cursor to end
    }

    private fun showSingleNumberSelectDialog(title: String, onSelected: (Int) -> Unit) {
        val dialogBinding = com.voicewolf.app.databinding.DialogSelectNumberBinding.inflate(layoutInflater)
        dialogBinding.titleText.text = title

        val activePlayers = viewModel.getActivePlayers()

        // Populate number grid dynamically
        dialogBinding.numberGrid.removeAllViews()
        activePlayers.forEach { player ->
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text = "${player.id}"
                textSize = 14f
                layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = androidx.gridlayout.widget.GridLayout.spec(androidx.gridlayout.widget.GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
            }
            btn.setOnClickListener {
                onSelected(player.id)
                // Find and dismiss the dialog
                (btn.parent.parent.parent as android.view.View).let { dialogView ->
                    (dialogView.parent.parent as? AlertDialog)?.dismiss()
                }
            }
            dialogBinding.numberGrid.addView(btn)
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCheckSelectDialog(onSelected: (Int, Boolean) -> Unit) {
        val dialogBinding = com.voicewolf.app.databinding.DialogSelectCheckBinding.inflate(layoutInflater)

        val activePlayers = viewModel.getActivePlayers()
        var selectedNumber: Int? = null
        var isWerewolf: Boolean? = null

        // Populate number grid dynamically
        dialogBinding.numberGrid.removeAllViews()
        activePlayers.forEach { player ->
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text = "${player.id}"
                textSize = 14f
                layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = androidx.gridlayout.widget.GridLayout.spec(androidx.gridlayout.widget.GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
            }
            btn.setOnClickListener {
                selectedNumber = player.id
                // Update button appearance to show selection
                btn.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.teal_700))
            }
            dialogBinding.numberGrid.addView(btn)
        }

        // Setup identity buttons
        dialogBinding.btnGood.setOnClickListener {
            isWerewolf = false
            if (selectedNumber != null) {
                onSelected(selectedNumber!!, false)
                // Dismiss dialog
                (dialogBinding.root.parent.parent as? AlertDialog)?.dismiss()
            } else {
                Toast.makeText(this, "请先选择玩家", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnWerewolf.setOnClickListener {
            isWerewolf = true
            if (selectedNumber != null) {
                onSelected(selectedNumber!!, true)
                // Dismiss dialog
                (dialogBinding.root.parent.parent as? AlertDialog)?.dismiss()
            } else {
                Toast.makeText(this, "请先选择玩家", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showMultiNumberSelectDialog(title: String, onSelected: (List<Int>) -> Unit) {
        val dialogBinding = com.voicewolf.app.databinding.DialogSelectNumbersMultiBinding.inflate(layoutInflater)
        dialogBinding.titleText.text = "$title（可多选）"

        val activePlayers = viewModel.getActivePlayers()
        val selectedNumbers = mutableSetOf<Int>()
        val buttons = mutableMapOf<Int, com.google.android.material.button.MaterialButton>()

        // Populate number grid with toggle buttons
        dialogBinding.numberGrid.removeAllViews()
        activePlayers.forEach { player ->
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text = "${player.id}"
                textSize = 14f
                layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = androidx.gridlayout.widget.GridLayout.spec(androidx.gridlayout.widget.GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
            }
            btn.setOnClickListener {
                if (selectedNumbers.contains(player.id)) {
                    selectedNumbers.remove(player.id)
                    btn.setBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))
                } else {
                    selectedNumbers.add(player.id)
                    btn.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.teal_700))
                }
                // Update selected text
                dialogBinding.selectedText.text = if (selectedNumbers.isEmpty()) {
                    "已选: 无"
                } else {
                    "已选: ${selectedNumbers.sorted().joinToString("、") { "${it}号" }}"
                }
            }
            buttons[player.id] = btn
            dialogBinding.numberGrid.addView(btn)
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("确认") { _, _ ->
                onSelected(selectedNumbers.sorted())
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddVoteDialog() {
        val dialogBinding = DialogAddVoteBinding.inflate(layoutInflater)
        val currentDay = viewModel.currentDay.value ?: 1

        // Get active players
        val activePlayers = viewModel.getActivePlayers()

        // Set target spinner with dynamic player list (add abstain option)
        val targetNames = arrayOf("弃票") + activePlayers.map { "${it.id}号" }.toTypedArray()
        val targetAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, targetNames)
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerTarget.adapter = targetAdapter

        // Create voter checkboxes dynamically
        voterCheckboxes.clear()
        dialogBinding.votersGrid.removeAllViews()
        activePlayers.forEach { player ->
            val checkBox = CheckBox(this).apply {
                text = "${player.id}号"
                setTextColor(ContextCompat.getColor(context, android.R.color.primary_text_light))
                layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = androidx.gridlayout.widget.GridLayout.spec(androidx.gridlayout.widget.GridLayout.UNDEFINED, 1f)
                }
            }
            voterCheckboxes[player.id] = checkBox
            dialogBinding.votersGrid.addView(checkBox)
        }

        // Load existing votes for this target when target changes
        var lastTarget = -1
        dialogBinding.spinnerTarget.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val targetId = if (position == 0) 0 else activePlayers[position - 1].id

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
                val targetId = if (targetPosition == 0) 0 else activePlayers[targetPosition - 1].id

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
            .setMessage("确定要重置游戏吗？所有记录将被清空，玩家数量将恢复为12人。")
            .setPositiveButton("确认") { _, _ ->
                viewModel.resetGame()
                Toast.makeText(this, "游戏已重置", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}