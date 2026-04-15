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
import com.voicewolf.app.databinding.ItemPlayerBinding

/**
 * Main activity for Werewolf face-to-face assistant app
 * Simplified for player perspective - focuses on speech and vote recording
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: GameViewModel
    private var setupName: String? = null

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

        // Receive setup and player count from intent
        setupName = intent.getStringExtra("SETUP_NAME")
        val playerCount = intent.getIntExtra("PLAYER_COUNT", 12)

        if (setupName != null) {
            val setup = GameSetupPresets.getByName(setupName!!)
            if (setup != null) {
                viewModel.setSetup(setup)
            }
        }

        // Initialize players with specified count
        viewModel.initializePlayers(playerCount)

        setupPlayerViews()
        setupButtons()
        observeViewModel()
    }

    private fun setupPlayerViews() {
        // Clear player views map
        playerViews.clear()

        // Left column: 1-6 (static from layout)
        playerViews[1] = binding.player1.root
        playerViews[2] = binding.player2.root
        playerViews[3] = binding.player3.root
        playerViews[4] = binding.player4.root
        playerViews[5] = binding.player5.root
        playerViews[6] = binding.player6.root

        // Set up left column players
        for (playerId in 1..6) {
            setupPlayerCard(playerId, playerViews[playerId]!!)
        }

        // Right column: dynamic rendering based on active players
        renderRightColumn()
    }

    private fun renderRightColumn() {
        val container = binding.rightColumnContainer

        // Remove all existing player cards
        val toRemove = mutableListOf<android.view.View>()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child.findViewById<TextView>(R.id.playerNumber) != null) {
                toRemove.add(child)
            }
        }
        toRemove.forEach { container.removeView(it) }

        // Clear playerViews map for ID >= 7
        playerViews.keys.filter { it >= 7 }.forEach { playerViews.remove(it) }

        // Get active players with ID >= 7
        val rightColumnPlayers = viewModel.getActivePlayers().filter { it.id >= 7 }.sortedBy { it.id }

        // Add player cards dynamically
        rightColumnPlayers.forEach { player ->
            val playerCard = createPlayerCard(player.id)
            container.addView(playerCard)
            playerViews[player.id] = playerCard
            setupPlayerCard(player.id, playerCard)
        }
    }

    private fun createPlayerCard(playerId: Int): android.view.View {
        val binding = ItemPlayerBinding.inflate(layoutInflater)
        binding.playerNumber.text = playerId.toString()
        // Set fixed size (51dp) to match layout XML
        val sizePx = (51 * resources.displayMetrics.density).toInt()
        val marginPx = (4 * resources.displayMetrics.density).toInt()
        binding.root.layoutParams = android.widget.LinearLayout.LayoutParams(sizePx, sizePx).apply {
            setMargins(marginPx, marginPx, marginPx, marginPx)
        }
        return binding.root
    }

    private fun setupPlayerCard(playerId: Int, playerView: android.view.View) {
        val binding = ItemPlayerBinding.bind(playerView)

        // Set player number
        binding.playerNumber.text = playerId.toString()

        // Click listener - show options dialog
        playerView.setOnClickListener {
            showPlayerOptionsDialog(playerId)
        }

        // Long click - toggle alive/dead
        playerView.setOnLongClickListener {
            val player = viewModel.getPlayerById(playerId)
            if (player != null) {
                viewModel.setPlayerAlive(playerId, !player.isAlive)
                updatePlayerView(playerId)
            }
            true
        }

        // Pencil icon click - quick role marking
        binding.btnMarkRole.setOnClickListener {
            showMarkRoleDialog(playerId)
        }

        // Hide remove button (not needed with fixed player count)
        binding.btnRemove.visibility = android.view.View.GONE
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
            binding.dayText.text = viewModel.getDayDisplayText(day)
        }

        // Observe all records text
        viewModel.allRecordsText.observe(this) { text ->
            binding.allRecordsText.text = text
        }

        // Observe players - re-render right column on change
        viewModel.players.observe(this) { players ->
            renderRightColumn()
            players.forEach { player ->
                updatePlayerView(player.id)
            }
        }
    }

    private fun updatePlayerView(playerId: Int) {
        val playerView = playerViews[playerId] ?: return
        val player = viewModel.getPlayerById(playerId) ?: return

        // Use ItemPlayerBinding to access views
        val binding = try {
            ItemPlayerBinding.bind(playerView)
        } catch (e: Exception) {
            // For static views in left column, use findViewById
            null
        }

        val roleText = binding?.roleText ?: playerView.findViewById<TextView>(R.id.roleText)
        val deadOverlay = binding?.deadOverlay ?: playerView.findViewById<android.widget.FrameLayout>(R.id.deadOverlay)
        val playerCard = binding?.playerCard ?: playerView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.playerCard)

        // Role text display
        if (player.markedRole != Player.MarkedRole.NONE) {
            roleText?.visibility = android.view.View.VISIBLE
            roleText?.text = player.getMarkedRoleDisplayName()

            val bgColorRes = when (player.markedRole) {
                Player.MarkedRole.SEER -> R.color.role_seer
                Player.MarkedRole.SEER_MIRROR -> R.color.role_seer_mirror
                Player.MarkedRole.WITCH -> R.color.role_witch
                Player.MarkedRole.HUNTER -> R.color.role_hunter
                Player.MarkedRole.GUARD -> R.color.role_guard
                Player.MarkedRole.KNIGHT -> R.color.role_knight
                Player.MarkedRole.IDIOT -> R.color.role_idiot
                Player.MarkedRole.GOOD -> R.color.role_good
                Player.MarkedRole.VILLAGER -> R.color.role_villager
                Player.MarkedRole.WEREWOLF -> R.color.role_werewolf
                Player.MarkedRole.WOLF_KING -> R.color.role_wolf_king
                Player.MarkedRole.WOLF_BEAUTY -> R.color.role_wolf_beauty
                Player.MarkedRole.MECHANICAL_WOLF -> R.color.role_mech_wolf
                Player.MarkedRole.NONE -> R.color.gray_light
            }
            roleText?.setBackgroundColor(resources.getColor(bgColorRes, null))
        } else {
            roleText?.visibility = android.view.View.GONE
        }

        // Dead overlay
        deadOverlay?.visibility = if (player.isAlive) android.view.View.GONE else android.view.View.VISIBLE

        // Card stroke color based on faction
        if (player.markedRole != Player.MarkedRole.NONE) {
            val strokeColorRes = if (player.isMarkedEvil()) R.color.border_werewolf else R.color.border_good
            playerCard?.strokeColor = resources.getColor(strokeColorRes, null)
        } else {
            playerCard?.strokeColor = resources.getColor(R.color.border_default, null)
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
        val setup = viewModel.currentSetup.value
        if (setup == null) {
            Toast.makeText(this, "请先选择游戏版型", Toast.LENGTH_SHORT).show()
            return
        }

        val currentPlayer = viewModel.getPlayerById(playerId)
        val currentMarkedRole = currentPlayer?.markedRole ?: Player.MarkedRole.NONE

        // Build role options from setup
        val goodRoles = setup.goodRoles.map { it.role }
        val evilRoles = setup.evilRoles.map { it.role }

        // Create custom dialog view
        val dialogView = android.view.View.inflate(this, R.layout.dialog_mark_role, null)
        val goodContainer = dialogView.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.goodRolesContainer)
        val evilContainer = dialogView.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.evilRolesContainer)

        // Add good role buttons
        goodRoles.forEach { role ->
            addRoleButton(goodContainer, role, playerId, currentMarkedRole)
        }

        // Add evil role buttons
        evilRoles.forEach { role ->
            addRoleButton(evilContainer, role, playerId, currentMarkedRole)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("标记身份 - ${playerId}号")
            .setView(dialogView)
            .setNegativeButton("取消", null)
            .create()

        // Store dialog reference for button clicks to close
        roleDialog = dialog
        dialog.show()
    }

    // Helper to add role button
    private var roleDialog: AlertDialog? = null

    private fun addRoleButton(
        container: com.google.android.flexbox.FlexboxLayout,
        role: Player.MarkedRole,
        playerId: Int,
        currentRole: Player.MarkedRole
    ) {
        val button = com.google.android.material.button.MaterialButton(this).apply {
            text = role.getMarkedRoleDisplayName()
            layoutParams = com.google.android.flexbox.FlexboxLayout.LayoutParams(
                com.google.android.flexbox.FlexboxLayout.LayoutParams.WRAP_CONTENT,
                com.google.android.flexbox.FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(4, 4, 4, 4)
                flexBasisPercent = -1f
            }

            // Highlight if currently selected
            if (role == currentRole) {
                strokeWidth = 2
                strokeColor = resources.getColorStateList(R.color.white, null)
            }

            // Set color based on faction
            setBackgroundColor(getRoleBackgroundColor(role))
            setTextColor(resources.getColor(R.color.white, null))

            setOnClickListener {
                viewModel.setPlayerMarkedRole(playerId, role)
                updatePlayerView(playerId)
                roleDialog?.dismiss()
            }
        }
        container.addView(button)
    }

    private fun getRoleBackgroundColor(role: Player.MarkedRole): Int {
        val colorRes = when (role) {
            Player.MarkedRole.SEER -> R.color.role_seer
            Player.MarkedRole.SEER_MIRROR -> R.color.role_seer_mirror
            Player.MarkedRole.WITCH -> R.color.role_witch
            Player.MarkedRole.HUNTER -> R.color.role_hunter
            Player.MarkedRole.GUARD -> R.color.role_guard
            Player.MarkedRole.KNIGHT -> R.color.role_knight
            Player.MarkedRole.IDIOT -> R.color.role_idiot
            Player.MarkedRole.VILLAGER -> R.color.role_villager
            Player.MarkedRole.GOOD -> R.color.role_good
            Player.MarkedRole.WEREWOLF -> R.color.role_werewolf
            Player.MarkedRole.WOLF_KING -> R.color.role_wolf_king
            Player.MarkedRole.WOLF_BEAUTY -> R.color.role_wolf_beauty
            Player.MarkedRole.MECHANICAL_WOLF -> R.color.role_mech_wolf
            Player.MarkedRole.NONE -> R.color.gray_light
        }
        return resources.getColor(colorRes, null)
    }

    private fun showAddSpeechDialog(playerId: Int? = null) {
        val dialogView = android.view.View.inflate(this, R.layout.dialog_add_speech, null)
        val templateContainer = dialogView.findViewById<android.widget.LinearLayout>(R.id.templateContainer)
        val spinnerPlayer = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerPlayer)
        val editContent = dialogView.findViewById<android.widget.EditText>(R.id.editContent)

        val currentDay = viewModel.currentDay.value ?: 0
        val setup = viewModel.currentSetup.value

        // Create template buttons based on setup
        createTemplateButtons(templateContainer, setup, editContent)

        // Set player spinner - dynamic player count
        val activePlayers = viewModel.getActivePlayers()
        val playerNames = activePlayers.map { "${it.id}号" }.toTypedArray()
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, playerNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPlayer.adapter = adapter

        // Set selected player if provided
        if (playerId != null) {
            val index = activePlayers.indexOfFirst { it.id == playerId }
            if (index >= 0) {
                spinnerPlayer.setSelection(index)
            }
        }

        // Load existing record if any
        val existingRecord = playerId?.let { viewModel.getSpeechRecordForPlayer(it, currentDay) }
        if (existingRecord != null) {
            editContent.setText(existingRecord.summary)
        }

        AlertDialog.Builder(this)
            .setTitle("记录发言 - ${viewModel.getDayDisplayText(currentDay)}")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val selectedIndex = spinnerPlayer.selectedItemPosition
                if (selectedIndex >= 0 && selectedIndex < activePlayers.size) {
                    val selectedPlayerId = activePlayers[selectedIndex].id
                    val content = editContent.text.toString()

                    if (content.isNotBlank()) {
                        viewModel.addSpeechRecord(selectedPlayerId, currentDay, content)
                        Toast.makeText(this, "发言已记录", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddVoteDialog() {
        val dialogBinding = DialogAddVoteBinding.inflate(layoutInflater)
        val currentDay = viewModel.currentDay.value ?: 0

        // Get active players dynamically
        val activePlayers = viewModel.getActivePlayers()

        // Set target spinner (add abstain option)
        val targetNames = arrayOf("弃票") + activePlayers.map { "${it.id}号" }.toTypedArray()
        val targetAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, targetNames)
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerTarget.adapter = targetAdapter

        // Create voter checkboxes in grid - dynamic count
        voterCheckboxes.clear()
        dialogBinding.votersGrid.removeAllViews()
        activePlayers.forEach { player ->
            val checkBox = CheckBox(this).apply {
                text = "${player.id}号"
                setTextColor(resources.getColor(R.color.white, null))
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
            .setTitle("记录投票 - ${viewModel.getDayDisplayText(currentDay)}")
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
                sb.append("═══ ${viewModel.getDayDisplayText(day)} ═══\n\n")

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
            .setTitle("跳转到指定阶段（0=警上）")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val day = input.text.toString().toIntOrNull()
                if (day != null && day >= 0) {
                    viewModel.setDay(day)
                } else {
                    Toast.makeText(this, "请输入有效的天数（0为警上）", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showResetConfirmDialog() {
        val currentSetup = viewModel.currentSetup.value
        val setupName = currentSetup?.name ?: "未选择"

        val options = arrayOf(
            "重新选择版型",
            "继续使用当前版型（$setupName）",
            "取消"
        )

        AlertDialog.Builder(this)
            .setTitle("重置游戏")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // 重新选择版型：清空记录并跳转到SetupActivity
                        viewModel.resetGame(keepSetup = false)
                        val intent = android.content.Intent(this, SetupActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    1 -> {
                        // 继续使用当前版型：只清空记录
                        viewModel.resetGame(keepSetup = true)
                        Toast.makeText(this, "游戏已重置，保持版型：$setupName", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        // 取消：不做任何操作
                    }
                }
            }
            .show()
    }

    // Template button configuration
    private data class TemplateButton(
        val text: String,
        val category: String,
        val role: Player.MarkedRole?,
        val actionType: TemplateActionType
    )

    private enum class TemplateActionType {
        DIRECT_INSERT,      // 直接插入文字
        SINGLE_SELECT,      // 单选号码
        MULTI_SELECT,       // 多选号码
        CHECK_SELECT        // 查验（号码+好人/狼人）
    }

    private val templateButtons = listOf(
        // 预言家类
        TemplateButton("跳预言", "预言家", Player.MarkedRole.SEER, TemplateActionType.DIRECT_INSERT),
        TemplateButton("查验X", "预言家", Player.MarkedRole.SEER, TemplateActionType.CHECK_SELECT),
        TemplateButton("警徽流", "预言家", Player.MarkedRole.SEER, TemplateActionType.MULTI_SELECT),
        // 通灵师类
        TemplateButton("跳通灵", "通灵师", Player.MarkedRole.SEER_MIRROR, TemplateActionType.DIRECT_INSERT),
        TemplateButton("查验X", "通灵师", Player.MarkedRole.SEER_MIRROR, TemplateActionType.CHECK_SELECT),
        TemplateButton("警徽流", "通灵师", Player.MarkedRole.SEER_MIRROR, TemplateActionType.MULTI_SELECT),
        // 女巫类
        TemplateButton("跳女巫", "女巫", Player.MarkedRole.WITCH, TemplateActionType.DIRECT_INSERT),
        TemplateButton("银水X", "女巫", Player.MarkedRole.WITCH, TemplateActionType.SINGLE_SELECT),
        TemplateButton("毒X", "女巫", Player.MarkedRole.WITCH, TemplateActionType.SINGLE_SELECT),
        // 守卫类
        TemplateButton("跳守卫", "守卫", Player.MarkedRole.GUARD, TemplateActionType.DIRECT_INSERT),
        TemplateButton("盾X", "守卫", Player.MarkedRole.GUARD, TemplateActionType.SINGLE_SELECT),
        // 猎人类
        TemplateButton("跳猎人", "猎人", Player.MarkedRole.HUNTER, TemplateActionType.DIRECT_INSERT),
        // 骑士类
        TemplateButton("跳骑士", "骑士", Player.MarkedRole.KNIGHT, TemplateActionType.DIRECT_INSERT),
        // 白痴类
        TemplateButton("跳白痴", "白痴", Player.MarkedRole.IDIOT, TemplateActionType.DIRECT_INSERT),
        // 狼王类
        TemplateButton("跳狼王", "狼王", Player.MarkedRole.WOLF_KING, TemplateActionType.DIRECT_INSERT),
        // 狼美人类
        TemplateButton("跳狼美", "狼美人", Player.MarkedRole.WOLF_BEAUTY, TemplateActionType.DIRECT_INSERT),
        // 机械狼类
        TemplateButton("跳机械狼", "机械狼", Player.MarkedRole.MECHANICAL_WOLF, TemplateActionType.DIRECT_INSERT),
        // 通用类（始终显示）
        TemplateButton("跳平民", "通用", null, TemplateActionType.MULTI_SELECT),
        TemplateButton("保X", "通用", null, TemplateActionType.MULTI_SELECT),
        TemplateButton("踩X", "通用", null, TemplateActionType.MULTI_SELECT),
        TemplateButton("狼在X", "通用", null, TemplateActionType.MULTI_SELECT)
    )

    private fun createTemplateButtons(
        container: android.widget.LinearLayout,
        setup: GameSetup?,
        editText: android.widget.EditText
    ) {
        container.removeAllViews()

        // Group buttons by category
        val categories = templateButtons.groupBy { it.category }

        categories.forEach { (category, buttons) ->
            // Filter buttons based on setup (null role = always show)
            val visibleButtons = buttons.filter { btn ->
                btn.role == null || (setup != null && setup.hasRole(btn.role))
            }

            if (visibleButtons.isEmpty()) return@forEach

            // Category label
            val categoryLabel = android.widget.TextView(this).apply {
                text = "【${category}】"
                setTextColor(resources.getColor(R.color.white, null))
                textSize = 12f
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 4)
                }
            }
            container.addView(categoryLabel)

            // Button row
            val buttonRow = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            container.addView(buttonRow)

            // Add buttons
            visibleButtons.forEach { btn ->
                val button = com.google.android.material.button.MaterialButton(this).apply {
                    text = btn.text
                    textSize = 12f
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        setMargins(4, 4, 4, 4)
                    }
                    setOnClickListener {
                        handleTemplateClick(btn, editText)
                    }
                }
                buttonRow.addView(button)
            }
        }
    }

    private fun handleTemplateClick(btn: TemplateButton, editText: android.widget.EditText) {
        when (btn.actionType) {
            TemplateActionType.DIRECT_INSERT -> {
                // 直接插入文字
                val insertText = when (btn.text) {
                    "跳预言" -> "跳预言家 "
                    "跳通灵" -> "跳通灵师 "
                    "跳女巫" -> "跳女巫 "
                    "跳守卫" -> "跳守卫 "
                    "跳猎人" -> "跳猎人 "
                    "跳骑士" -> "跳骑士 "
                    "跳白痴" -> "跳白痴 "
                    "跳狼王" -> "跳狼王 "
                    "跳狼美" -> "跳狼美人 "
                    "跳机械狼" -> "跳机械狼 "
                    "跳平民" -> "跳平民 "
                    else -> "${btn.text} "
                }
                appendToEditText(editText, insertText)
            }
            TemplateActionType.SINGLE_SELECT -> {
                showSingleSelectDialog(btn.text, editText)
            }
            TemplateActionType.MULTI_SELECT -> {
                showMultiSelectDialog(btn.text, editText)
            }
            TemplateActionType.CHECK_SELECT -> {
                showCheckSelectDialog(btn.text, editText)
            }
        }
    }

    private fun appendToEditText(editText: android.widget.EditText, text: String) {
        val currentText = editText.text.toString()
        editText.setText(currentText + text)
        editText.setSelection(editText.text.length)
    }

    private fun showSingleSelectDialog(templateName: String, editText: android.widget.EditText) {
        val dialogView = android.view.View.inflate(this, R.layout.dialog_select_number, null)
        val gridLayout = dialogView.findViewById<android.widget.GridLayout>(R.id.numbersGrid)

        var selectedId: Int? = null

        // Populate grid with player numbers
        val activePlayers = viewModel.getActivePlayers()
        activePlayers.forEach { player ->
            val button = com.google.android.material.button.MaterialButton(this).apply {
                text = "${player.id}号"
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
                setOnClickListener {
                    // Update selection state
                    selectedId = player.id
                    // Reset all buttons' stroke
                    for (i in 0 until gridLayout.childCount) {
                        val child = gridLayout.getChildAt(i) as? com.google.android.material.button.MaterialButton
                        child?.strokeWidth = 0
                    }
                    // Highlight selected button
                    strokeWidth = 2
                    strokeColor = resources.getColorStateList(R.color.white, null)
                }
            }
            gridLayout.addView(button)
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("确认") { _, _ ->
                selectedId?.let { id ->
                    // Replace X in template name with the selected number
                    val resultText = templateName.replace("X", "${id}号") + " "
                    appendToEditText(editText, resultText)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showMultiSelectDialog(templateName: String, editText: android.widget.EditText) {
        val dialogView = android.view.View.inflate(this, R.layout.dialog_select_numbers_multi, null)
        val gridLayout = dialogView.findViewById<android.widget.GridLayout>(R.id.numbersGrid)
        val selectedText = dialogView.findViewById<android.widget.TextView>(R.id.selectedText)

        val selectedIds = mutableSetOf<Int>()

        // Populate grid with player buttons
        val activePlayers = viewModel.getActivePlayers()
        activePlayers.forEach { player ->
            val button = com.google.android.material.button.MaterialButton(this).apply {
                text = "${player.id}号"
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
                setOnClickListener {
                    if (selectedIds.contains(player.id)) {
                        selectedIds.remove(player.id)
                        strokeWidth = 0
                    } else {
                        selectedIds.add(player.id)
                        strokeWidth = 2
                        strokeColor = resources.getColorStateList(R.color.white, null)
                    }
                    selectedText.text = "已选: ${selectedIds.sorted().joinToString("号 ") { "$it" }}号"
                }
            }
            gridLayout.addView(button)
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("确认") { _, _ ->
                if (selectedIds.isNotEmpty()) {
                    // Replace X in template name with the selected numbers
                    val numbersText = selectedIds.sorted().joinToString("号 ") { "$it" } + "号"
                    val resultText = templateName.replace("X", numbersText) + " "
                    appendToEditText(editText, resultText)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCheckSelectDialog(templateName: String, editText: android.widget.EditText) {
        val dialogView = android.view.View.inflate(this, R.layout.dialog_select_check, null)
        val gridLayout = dialogView.findViewById<android.widget.GridLayout>(R.id.numbersGrid)
        val btnGood = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGood)
        val btnEvil = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEvil)

        var selectedPlayerId: Int? = null
        var selectedResult: String? = null

        // Populate grid with player buttons
        val activePlayers = viewModel.getActivePlayers()
        activePlayers.forEach { player ->
            val button = com.google.android.material.button.MaterialButton(this).apply {
                text = "${player.id}号"
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
                setOnClickListener {
                    // Clear previous selection using for loop (API compatibility)
                    for (i in 0 until gridLayout.childCount) {
                        val child = gridLayout.getChildAt(i)
                        if (child is com.google.android.material.button.MaterialButton) {
                            child.strokeWidth = 0
                        }
                    }
                    // Highlight this one
                    selectedPlayerId = player.id
                    strokeWidth = 2
                    strokeColor = resources.getColorStateList(R.color.white, null)
                }
            }
            gridLayout.addView(button)
        }

        // Result buttons
        btnGood.setOnClickListener {
            selectedResult = "好人"
            btnGood.strokeWidth = 2
            btnGood.strokeColor = resources.getColorStateList(R.color.white, null)
            btnEvil.strokeWidth = 0
        }

        btnEvil.setOnClickListener {
            selectedResult = "狼人"
            btnEvil.strokeWidth = 2
            btnEvil.strokeColor = resources.getColorStateList(R.color.white, null)
            btnGood.strokeWidth = 0
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("确认") { _, _ ->
                if (selectedPlayerId != null && selectedResult != null) {
                    // Replace X in template name with player number and result
                    val resultText = templateName.replace("X", "${selectedPlayerId}号($selectedResult)") + " "
                    appendToEditText(editText, resultText)
                } else {
                    Toast.makeText(this, "请选择玩家和查验结果", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}