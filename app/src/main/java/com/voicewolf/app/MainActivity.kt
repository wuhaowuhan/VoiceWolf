package com.voicewolf.app

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.voicewolf.app.databinding.ActivityMainBinding
import com.voicewolf.app.databinding.DialogAddRecordBinding
import com.voicewolf.app.databinding.DialogPlayerActionsBinding
import com.voicewolf.app.databinding.DialogRoundReviewBinding

/**
 * Main activity for Werewolf face-to-face assistant app
 * Uses MVVM architecture with ViewModel + LiveData
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: GameViewModel
    private lateinit var infoLogAdapter: InfoLogAdapter

    // Player views map
    private val playerViews = mutableMapOf<Int, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[GameViewModel::class.java]

        setupPlayerViews()
        setupRecyclerViews()
        setupTabs()
        setupGameModeSelection()
        setupButtons()
        setupTopBar()
        observeViewModel()
    }

    private fun setupPlayerViews() {
        // Map player IDs to their views using the include tags
        playerViews[1] = findViewById(R.id.player1)
        playerViews[2] = findViewById(R.id.player2)
        playerViews[3] = findViewById(R.id.player3)
        playerViews[4] = findViewById(R.id.player4)
        playerViews[5] = findViewById(R.id.player5)
        playerViews[6] = findViewById(R.id.player6)
        playerViews[7] = findViewById(R.id.player7)
        playerViews[8] = findViewById(R.id.player8)
        playerViews[9] = findViewById(R.id.player9)
        playerViews[10] = findViewById(R.id.player10)
        playerViews[11] = findViewById(R.id.player11)
        playerViews[12] = findViewById(R.id.player12)

        // Setup click listeners for each player
        for ((playerId, playerView) in playerViews) {
            playerView.setOnClickListener {
                viewModel.getPlayerById(playerId)?.let { onPlayerClick(it) }
            }
            playerView.setOnLongClickListener {
                viewModel.getPlayerById(playerId)?.let { showPlayerActionDialog(it) }
                true
            }
        }
    }

    private fun setupRecyclerViews() {
        // Setup info log RecyclerView
        infoLogAdapter = InfoLogAdapter()
        binding.infoRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = infoLogAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("房间信息"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("聊天信息"))
    }

    private fun setupGameModeSelection() {
        binding.gameModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val gameMode = when (position) {
                    0 -> GameMode.STANDARD
                    1 -> GameMode.WOLF_BEAUTY_KNIGHT
                    2 -> GameMode.MECHANIC_WOLF
                    3 -> GameMode.WHITE_WOLF_KING
                    4 -> GameMode.GARGOYLE_GRAVEDIGGER
                    5 -> GameMode.FORTUNE_TELLER
                    else -> GameMode.STANDARD
                }
                viewModel.setGameMode(gameMode)
                Toast.makeText(this@MainActivity, gameMode.description, Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupTopBar() {
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun setupButtons() {
        binding.btnStartGame.setOnClickListener {
            if (!(viewModel.isGameStarted.value ?: false)) {
                viewModel.startGame()
            } else {
                viewModel.toggleDayNight()
            }
        }

        binding.btnResetGame.setOnClickListener {
            showResetGameDialog()
        }

        binding.btnPlayerAction.setOnClickListener {
            showSelectPlayerDialog()
        }

        binding.btnLike.setOnClickListener {
            Toast.makeText(this, "点赞", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        // Observe game state
        viewModel.isGameStarted.observe(this) { isStarted ->
            if (isStarted) {
                binding.btnStartGame.text = if (viewModel.isDayPhase.value == true) "进入夜晚" else "进入白天"
            } else {
                binding.btnStartGame.text = getString(R.string.start_game)
            }
        }

        viewModel.currentRound.observe(this) { round ->
            binding.roundNumber.text = round.toString()
        }

        viewModel.isDayPhase.observe(this) { isDay ->
            binding.roundType.text = if (isDay) "天" else "夜"
            
            // Update day/night indicator color
            binding.dayIndicator.setCardBackgroundColor(
                if (isDay) getColor(android.R.color.holo_blue_light)
                else getColor(android.R.color.holo_purple)
            )
            
            // Update center panel background
            binding.centerPanel.setCardBackgroundColor(
                if (isDay) getColor(R.color.info_panel_day)
                else getColor(R.color.info_panel_night)
            )
        }

        // Observe players
        viewModel.players.observe(this) { players ->
            players.forEach { player ->
                updatePlayerView(player)
            }
        }

        // Observe game events
        viewModel.gameEvents.observe(this) { events ->
            infoLogAdapter.clear()
            events.forEach { event ->
                infoLogAdapter.addEvent(event)
            }
        }

        // Observe death info
        viewModel.deathInfo.observe(this) { info ->
            binding.deathInfo.text = info
        }

        // Observe vote info
        viewModel.voteInfo.observe(this) { info ->
            binding.voteInfo.text = info
        }

        // Observe speaker info
        viewModel.speakerText.observe(this) { text ->
            binding.speakerText.text = text
        }

        viewModel.speakerTimer.observe(this) { timer ->
            binding.speakerTimer.text = timer
        }
    }

    private fun showResetGameDialog() {
        AlertDialog.Builder(this)
            .setTitle("确认重置")
            .setMessage("确定要重置游戏吗？所有数据将被清空。")
            .setPositiveButton("确认") { _, _ ->
                viewModel.resetGame()
                Toast.makeText(this@MainActivity, "游戏已重置", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun onPlayerClick(player: Player) {
        if (!(viewModel.isGameStarted.value ?: false)) {
            Toast.makeText(this, "请先开始游戏", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!player.isAlive) {
            Toast.makeText(this, "${player.id}号玩家已出局", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.selectPlayer(player)
        viewModel.setSpeaker(player.id)
        showPlayerActionDialog(player)
    }

    private fun showPlayerActionDialog(player: Player) {
        val dialogBinding = DialogPlayerActionsBinding.inflate(layoutInflater)
        dialogBinding.dialogTitle.text = "${player.id}号玩家 - 操作"
        
        // Replace radio group with spinner for role selection
        val roleSpinner = androidx.appcompat.widget.AppCompatSpinner(this)
        val roles = listOf(
            "未知",
            "村民",
            "预言家",
            "女巫",
            "猎人",
            "守卫",
            "骑士",
            "通灵师",
            "狼人",
            "狼美人",
            "机械狼"
        )
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = roleAdapter
        
        // Set current role selection
        val currentRoleIndex = when (player.role) {
            Player.Role.UNKNOWN -> 0
            Player.Role.VILLAGER -> 1
            Player.Role.SEER -> 2
            Player.Role.WITCH -> 3
            Player.Role.HUNTER -> 4
            Player.Role.GUARD -> 5
            Player.Role.KNIGHT -> 6
            Player.Role.MEDIUM -> 7
            Player.Role.WEREWOLF -> 8
            Player.Role.WEREWOLF_BEAUTY -> 9
            Player.Role.MECHANICAL_WOLF -> 10
        }
        roleSpinner.setSelection(currentRoleIndex)
        
        // Role selection listener
        roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val newRole = when (position) {
                    0 -> Player.Role.UNKNOWN
                    1 -> Player.Role.VILLAGER
                    2 -> Player.Role.SEER
                    3 -> Player.Role.WITCH
                    4 -> Player.Role.HUNTER
                    5 -> Player.Role.GUARD
                    6 -> Player.Role.KNIGHT
                    7 -> Player.Role.MEDIUM
                    8 -> Player.Role.WEREWOLF
                    9 -> Player.Role.WEREWOLF_BEAUTY
                    10 -> Player.Role.MECHANICAL_WOLF
                    else -> Player.Role.UNKNOWN
                }
                player.role = newRole
                viewModel.setPlayerRole(player.id, newRole)
                updatePlayerView(player)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        
        // Replace radio group with spinner
        val roleLayout = dialogBinding.roleRadioGroup.parent as ViewGroup
        val radioGroupIndex = roleLayout.indexOfChild(dialogBinding.roleRadioGroup)
        roleLayout.removeView(dialogBinding.roleRadioGroup)
        roleLayout.addView(roleSpinner, radioGroupIndex)
        
        // Speech summary button
        dialogBinding.btnSpeechSummary.setOnClickListener {
            showSpeechSummaryDialog(player)
        }
        
        // Voting review button
        dialogBinding.btnVotingReview.setOnClickListener {
            showVotingReviewDialog(player)
        }
        
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showSpeechSummaryDialog(player: Player) {
        val dialogBinding = DialogRoundReviewBinding.inflate(layoutInflater)
        dialogBinding.reviewTitle.text = "${player.id}号玩家 - 发言总结回顾"
        
        val playerSpeeches = viewModel.getSpeechRecordsForPlayer(player.id)
        
        val rounds = playerSpeeches.map { "第${it.round}轮" }.toMutableList()
        rounds.add("+ 添加新记录")
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, rounds)
        dialogBinding.roundDropdown.setAdapter(adapter)
        
        dialogBinding.roundDropdown.setOnItemClickListener { parent, _, position, _ ->
            if (position < playerSpeeches.size) {
                val record = playerSpeeches[position]
                dialogBinding.reviewContent.text = "第${record.round}轮发言总结：\n\n${record.summary}"
                dialogBinding.btnAddRecord.text = "编辑记录"
                dialogBinding.btnAddRecord.setOnClickListener {
                    showAddSpeechDialog(player, record)
                }
            } else {
                showAddSpeechDialog(player, null)
            }
        }
        
        dialogBinding.btnAddRecord.setOnClickListener {
            showAddSpeechDialog(player, null)
        }
        
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showVotingReviewDialog(player: Player) {
        val dialogBinding = DialogRoundReviewBinding.inflate(layoutInflater)
        dialogBinding.reviewTitle.text = "${player.id}号玩家 - 投票回顾"
        dialogBinding.btnAddRecord.text = "添加投票记录"
        
        val playerVotes = viewModel.getVoteRecordsForPlayer(player.id)
        
        val rounds = playerVotes.map { "第${it.round}轮" }.toMutableList()
        rounds.add("+ 添加新记录")
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, rounds)
        dialogBinding.roundDropdown.setAdapter(adapter)
        
        dialogBinding.roundDropdown.setOnItemClickListener { parent, _, position, _ ->
            if (position < playerVotes.size) {
                val record = playerVotes[position]
                val targetName = viewModel.getPlayerById(record.targetId)?.let { "${it.id}号" } ?: "未知"
                dialogBinding.reviewContent.text = "第${record.round}轮投票：\n\n${player.id}号 投票给 ${targetName}"
                dialogBinding.btnAddRecord.text = "编辑记录"
                dialogBinding.btnAddRecord.setOnClickListener {
                    showAddVoteDialog(player, record)
                }
            } else {
                showAddVoteDialog(player, null)
            }
        }
        
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showAddSpeechDialog(player: Player, existingRecord: SpeechRecord?) {
        val dialogBinding = DialogAddRecordBinding.inflate(layoutInflater)
        dialogBinding.addRecordTitle.text = if (existingRecord == null) "添加发言总结" else "编辑发言总结"
        
        if (existingRecord != null) {
            dialogBinding.editRound.setText(existingRecord.round.toString())
            dialogBinding.editContent.setText(existingRecord.summary)
        } else {
            dialogBinding.editRound.setText(viewModel.currentRound.value.toString())
        }
        
        // Add duration input
        val durationInput = androidx.appcompat.widget.AppCompatEditText(this)
        durationInput.hint = "发言时长（秒）"
        durationInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        if (existingRecord != null) {
            durationInput.setText(existingRecord.duration.toString())
        }
        
        // Add speech type spinner
        val typeSpinner = androidx.appcompat.widget.AppCompatSpinner(this)
        val types = arrayOf("常规发言", "遗言", "反驳")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter
        if (existingRecord != null) {
            typeSpinner.setSelection(when (existingRecord.type) {
                SpeechRecord.SpeechType.NORMAL -> 0
                SpeechRecord.SpeechType.LAST_WORD -> 1
                SpeechRecord.SpeechType.REPLY -> 2
            })
        }
        
        // Create a linear layout to hold the new inputs
        val layout = androidx.appcompat.widget.LinearLayoutCompat(this)
        layout.orientation = androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
        layout.addView(dialogBinding.root)
        layout.addView(durationInput)
        layout.addView(typeSpinner)
        
        AlertDialog.Builder(this)
            .setTitle(if (existingRecord == null) "添加发言总结" else "编辑发言总结")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val round = dialogBinding.editRound.text.toString().toIntOrNull() ?: viewModel.currentRound.value ?: 1
                val summary = dialogBinding.editContent.text.toString()
                val duration = durationInput.text.toString().toIntOrNull() ?: 0
                val type = when (typeSpinner.selectedItemPosition) {
                    0 -> SpeechRecord.SpeechType.NORMAL
                    1 -> SpeechRecord.SpeechType.LAST_WORD
                    2 -> SpeechRecord.SpeechType.REPLY
                    else -> SpeechRecord.SpeechType.NORMAL
                }
                viewModel.addSpeechRecord(player.id, round, summary, duration, type)
                Toast.makeText(this, "发言总结已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddVoteDialog(player: Player, existingRecord: VoteRecord?) {
        val dialogBinding = DialogAddRecordBinding.inflate(layoutInflater)
        dialogBinding.addRecordTitle.text = if (existingRecord == null) "添加投票记录" else "编辑投票记录"
        dialogBinding.editContent.hint = "投票给几号玩家（输入数字）"
        
        if (existingRecord != null) {
            dialogBinding.editRound.setText(existingRecord.round.toString())
            dialogBinding.editContent.setText(existingRecord.targetId.toString())
        } else {
            dialogBinding.editRound.setText(viewModel.currentRound.value.toString())
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (existingRecord == null) "添加投票记录" else "编辑投票记录")
            .setView(dialogBinding.root)
            .setPositiveButton("保存") { _, _ ->
                val round = dialogBinding.editRound.text.toString().toIntOrNull() ?: viewModel.currentRound.value ?: 1
                val targetId = dialogBinding.editContent.text.toString().toIntOrNull()
                
                if (targetId != null && targetId in 1..12) {
                    viewModel.recordVote(player.id, targetId)
                    Toast.makeText(this, "投票记录已保存", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "请输入有效的玩家编号 (1-12)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSelectPlayerDialog() {
        if (!(viewModel.isGameStarted.value ?: false)) {
            Toast.makeText(this, "请先开始游戏", Toast.LENGTH_SHORT).show()
            return
        }

        val alivePlayers = viewModel.getAlivePlayers()
        val playerNames = alivePlayers.map { "${it.id}号 - ${it.getRoleDisplayName()}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("选择玩家")
            .setItems(playerNames) { _, which ->
                val player = alivePlayers[which]
                showPlayerActionDialog(player)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSettingsDialog() {
        val items = arrayOf("标记玩家出局", "记录投票", "设置发言顺序", "查看投票统计")
        
        AlertDialog.Builder(this)
            .setTitle("游戏设置")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showSelectPlayerForDeathDialog()
                    1 -> showSelectPlayerForVoteDialog()
                    2 -> Toast.makeText(this, "设置发言顺序", Toast.LENGTH_SHORT).show()
                    3 -> showVoteStatisticsDialog()
                }
            }
            .setNegativeButton("关闭", null)
            .show()
    }
    
    private fun showVoteStatisticsDialog() {
        val dialogBinding = DialogRoundReviewBinding.inflate(layoutInflater)
        dialogBinding.reviewTitle.text = "投票统计"
        
        val voteStats = viewModel.getVoteStatistics()
        val voteHistory = viewModel.getVoteHistory()
        
        val statsText = StringBuilder()
        
        // Current round statistics
        statsText.append("当前轮次投票统计：\n\n")
        if (voteStats.isEmpty()) {
            statsText.append("本轮暂无投票记录\n\n")
        } else {
            voteStats.forEach { (playerId, count) ->
                statsText.append("${playerId}号：${count}票\n")
            }
            statsText.append("\n")
        }
        
        // Most voted player
        val mostVoted = viewModel.getMostVotedPlayer()
        if (mostVoted != null) {
            statsText.append("得票最多：${mostVoted.first}号（${mostVoted.second}票）\n\n")
        }
        
        // Vote history
        statsText.append("历史投票记录：\n\n")
        if (voteHistory.isEmpty()) {
            statsText.append("暂无历史投票记录\n")
        } else {
            voteHistory.forEach { (round, votes) ->
                statsText.append("第${round}轮：\n")
                votes.forEach { (playerId, count) ->
                    statsText.append("  ${playerId}号：${count}票\n")
                }
                statsText.append("\n")
            }
        }
        
        dialogBinding.reviewContent.text = statsText.toString()
        dialogBinding.btnAddRecord.visibility = View.GONE
        
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showSelectPlayerForDeathDialog() {
        val alivePlayers = viewModel.getAlivePlayers()
        val playerNames = alivePlayers.map { "${it.id}号 - ${it.getRoleDisplayName()}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("选择出局玩家")
            .setItems(playerNames) { _, which ->
                val player = alivePlayers[which]
                viewModel.markPlayerDead(player.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSelectPlayerForVoteDialog() {
        val alivePlayers = viewModel.getAlivePlayers()
        val playerNames = alivePlayers.map { "${it.id}号 - ${it.getRoleDisplayName()}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("选择投票玩家")
            .setItems(playerNames) { _, which ->
                val voter = alivePlayers[which]
                showVoteTargetDialog(voter)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showVoteTargetDialog(voter: Player) {
        val targetPlayers = viewModel.getAlivePlayers().filter { it.id != voter.id }
        val playerNames = targetPlayers.map { "${it.id}号 - ${it.getRoleDisplayName()}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("${voter.id}号投票给...")
            .setItems(playerNames) { _, which ->
                val target = targetPlayers[which]
                viewModel.recordVote(voter.id, target.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updatePlayerView(player: Player) {
        val playerView = playerViews[player.id]
        if (playerView != null) {
            val statusIndicator = playerView.findViewById<View>(R.id.statusIndicator)
            val roleBadge = playerView.findViewById<View>(R.id.roleBadge)
            val roleText = playerView.findViewById<TextView>(R.id.roleText)
            
            statusIndicator.setBackgroundResource(
                if (player.isAlive) R.drawable.status_indicator_alive
                else R.drawable.status_indicator_dead
            )
            
            // Update role badge and text
            when (player.role) {
                Player.Role.UNKNOWN -> {
                    roleBadge.visibility = View.GONE
                    if (roleText != null) roleText.text = ""
                }
                // Good roles
                Player.Role.VILLAGER, Player.Role.SEER, Player.Role.WITCH, Player.Role.HUNTER, Player.Role.GUARD, Player.Role.KNIGHT, Player.Role.MEDIUM -> {
                    roleBadge.setBackgroundResource(R.drawable.badge_good)
                    roleBadge.visibility = View.VISIBLE
                    if (roleText != null) roleText.text = player.getRoleDisplayName()
                }
                // Evil roles
                Player.Role.WEREWOLF, Player.Role.WEREWOLF_BEAUTY, Player.Role.MECHANICAL_WOLF -> {
                    roleBadge.setBackgroundResource(R.drawable.badge_werewolf)
                    roleBadge.visibility = View.VISIBLE
                    if (roleText != null) roleText.text = player.getRoleDisplayName()
                }
            }
        }
    }
}
