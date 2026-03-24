package com.voicewolf.app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
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

        binding.btnHistory.setOnClickListener {
            showHistoryDialog()
        }

        binding.btnMenu.setOnClickListener {
            showMenuDialog()
        }
    }

    private fun observeViewModel() {
        // Observe current round
        viewModel.currentRound.observe(this) { round ->
            binding.roundText.text = "轮次: $round"
        }

        // Observe speech info
        viewModel.speechInfo.observe(this) { info ->
            binding.speechInfo.text = info
        }

        // Observe vote info
        viewModel.voteInfo.observe(this) { info ->
            binding.voteInfo.text = info
        }

        // Observe vote stats
        viewModel.voteStats.observe(this) { stats ->
            binding.voteStats.text = stats
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

        val statusIndicator = playerView.findViewById<android.view.View>(R.id.statusIndicator)
        statusIndicator?.setBackgroundResource(
            if (player.isAlive) R.drawable.status_indicator_alive
            else R.drawable.status_indicator_dead
        )
    }

    private fun showPlayerOptionsDialog(playerId: Int) {
        val options = arrayOf("记录发言", "记录投票")
        val currentRound = viewModel.currentRound.value ?: 1

        AlertDialog.Builder(this)
            .setTitle("${playerId}号玩家")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddSpeechDialog(playerId, currentRound)
                    1 -> showAddVoteDialog(playerId, currentRound)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddSpeechDialog(playerId: Int? = null, round: Int? = null) {
        val dialogBinding = DialogAddSpeechBinding.inflate(layoutInflater)
        val currentRound = round ?: viewModel.currentRound.value ?: 1

        // Set round
        dialogBinding.editRound.setText(currentRound.toString())

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
        val existingRecord = playerId?.let { viewModel.getSpeechRecordForPlayer(it, currentRound) }
        if (existingRecord != null) {
            dialogBinding.editContent.setText(existingRecord.summary)
        }

        AlertDialog.Builder(this)
            .setTitle("记录发言")
            .setView(dialogBinding.root)
            .setPositiveButton("保存") { _, _ ->
                val selectedPlayerId = dialogBinding.spinnerPlayer.selectedItemPosition + 1
                val inputRound = dialogBinding.editRound.text.toString().toIntOrNull() ?: currentRound
                val content = dialogBinding.editContent.text.toString()

                if (content.isNotBlank()) {
                    viewModel.addSpeechRecord(selectedPlayerId, inputRound, content)
                    Toast.makeText(this, "发言已记录", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddVoteDialog(playerId: Int? = null, round: Int? = null) {
        val dialogBinding = DialogAddVoteBinding.inflate(layoutInflater)
        val currentRound = round ?: viewModel.currentRound.value ?: 1

        // Set round
        dialogBinding.editRound.setText(currentRound.toString())

        // Set voter spinner
        val playerNames = (1..12).map { "${it}号" }.toTypedArray()
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, playerNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerVoter.adapter = adapter

        // Set target spinner (add abstain option)
        val targetNames = arrayOf("弃票") + (1..12).map { "${it}号" }.toTypedArray()
        val targetAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, targetNames)
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerTarget.adapter = targetAdapter

        // Set selected voter if provided
        if (playerId != null) {
            dialogBinding.spinnerVoter.setSelection(playerId - 1)
        }

        // Load existing record if any
        val existingRecord = playerId?.let { viewModel.getVoteRecordForPlayer(it, currentRound) }
        if (existingRecord != null) {
            dialogBinding.spinnerTarget.setSelection(existingRecord.targetId) // 0 = abstain, 1-12 = players
        }

        AlertDialog.Builder(this)
            .setTitle("记录投票")
            .setView(dialogBinding.root)
            .setPositiveButton("保存") { _, _ ->
                val voterId = dialogBinding.spinnerVoter.selectedItemPosition + 1
                val inputRound = dialogBinding.editRound.text.toString().toIntOrNull() ?: currentRound
                val targetPosition = dialogBinding.spinnerTarget.selectedItemPosition
                val targetId = if (targetPosition == 0) 0 else targetPosition

                viewModel.recordVote(voterId, targetId, inputRound)
                Toast.makeText(this, "投票已记录", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showHistoryDialog() {
        val dialogBinding = DialogHistoryBinding.inflate(layoutInflater)
        val allRounds = viewModel.getAllRounds()

        if (allRounds.isEmpty()) {
            dialogBinding.historyContent.text = "暂无历史记录"
        } else {
            val sb = StringBuilder()
            allRounds.forEach { round ->
                sb.append("═══ 第${round}轮 ═══\n\n")

                // Speech records
                val speeches = viewModel.getSpeechRecordsForRound(round)
                if (speeches.isNotEmpty()) {
                    sb.append("【发言】\n")
                    speeches.forEach { record ->
                        sb.append("${record.playerId}号: ${record.summary}\n\n")
                    }
                }

                // Vote records
                val votes = viewModel.getVoteRecordsForRound(round)
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

                    // Vote stats for this round
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
        val currentRound = viewModel.currentRound.value ?: 1
        val options = arrayOf(
            "下一轮 (当前: $currentRound)",
            "上一轮",
            "跳转到指定轮次",
            "重置游戏"
        )

        AlertDialog.Builder(this)
            .setTitle("菜单")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.nextRound()
                    1 -> viewModel.prevRound()
                    2 -> showJumpRoundDialog()
                    3 -> showResetConfirmDialog()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showJumpRoundDialog() {
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(viewModel.currentRound.value.toString())

        AlertDialog.Builder(this)
            .setTitle("跳转到轮次")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val round = input.text.toString().toIntOrNull()
                if (round != null && round >= 1) {
                    viewModel.setRound(round)
                } else {
                    Toast.makeText(this, "请输入有效的轮次", Toast.LENGTH_SHORT).show()
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