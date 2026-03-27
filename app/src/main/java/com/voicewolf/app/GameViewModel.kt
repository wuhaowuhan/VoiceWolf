package com.voicewolf.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel for Werewolf game state management
 * Simplified for player perspective - focuses on speech and vote recording
 */
class GameViewModel : ViewModel() {

    // Current day
    private val _currentDay = MutableLiveData(1)
    val currentDay: LiveData<Int> = _currentDay

    // Players
    private val _players = MutableLiveData<List<Player>>(emptyList())
    val players: LiveData<List<Player>> = _players

    // Speech records
    private val _speechRecords = MutableLiveData<MutableList<SpeechRecord>>(mutableListOf())
    val speechRecords: LiveData<MutableList<SpeechRecord>> = _speechRecords

    // Vote records
    private val _voteRecords = MutableLiveData<MutableList<VoteRecord>>(mutableListOf())
    val voteRecords: LiveData<MutableList<VoteRecord>> = _voteRecords

    // Display info - all records formatted
    private val _allRecordsText = MutableLiveData("")
    val allRecordsText: LiveData<String> = _allRecordsText

    init {
        initializePlayers()
        updateDisplayInfo()
    }

    private fun initializePlayers() {
        val playerList = (1..12).map { Player(id = it) }
        _players.value = playerList
    }

    // Day management
    fun nextDay() {
        _currentDay.value = (_currentDay.value ?: 1) + 1
    }

    fun prevDay() {
        val current = _currentDay.value ?: 1
        if (current > 1) {
            _currentDay.value = current - 1
        }
    }

    fun setDay(day: Int) {
        if (day >= 1) {
            _currentDay.value = day
        }
    }

    // Reset game
    fun resetGame() {
        _currentDay.value = 1
        _speechRecords.value = mutableListOf()
        _voteRecords.value = mutableListOf()
        _allRecordsText.value = ""

        // Reset all players
        val resetPlayers = _players.value?.map { player ->
            player.copy(
                isAlive = true,
                role = Player.Role.UNKNOWN,
                markedRole = Player.MarkedRole.NONE
            )
        } ?: emptyList()
        _players.value = resetPlayers
        updateDisplayInfo()
    }

    // Speech record methods
    fun addSpeechRecord(playerId: Int, day: Int, summary: String) {
        val currentRecords = _speechRecords.value ?: mutableListOf()

        // Check if record exists for this player and day
        val existingIndex = currentRecords.indexOfFirst {
            it.playerId == playerId && it.day == day
        }

        if (existingIndex >= 0) {
            // Update existing
            currentRecords[existingIndex] = currentRecords[existingIndex].copy(
                summary = summary
            )
        } else {
            // Add new
            currentRecords.add(SpeechRecord(
                day = day,
                playerId = playerId,
                summary = summary
            ))
        }

        _speechRecords.value = currentRecords
        updateDisplayInfo()
    }

    fun deleteSpeechRecord(record: SpeechRecord) {
        val currentRecords = _speechRecords.value ?: mutableListOf()
        currentRecords.removeAll { it.playerId == record.playerId && it.day == record.day }
        _speechRecords.value = currentRecords
        updateDisplayInfo()
    }

    // Vote record methods
    fun recordVote(voterId: Int, targetId: Int, day: Int) {
        val currentRecords = _voteRecords.value ?: mutableListOf()

        // Remove existing vote for this voter in this day
        currentRecords.removeAll { it.voterId == voterId && it.day == day }

        // Add new vote
        if (targetId > 0) {
            currentRecords.add(VoteRecord(
                day = day,
                voterId = voterId,
                targetId = targetId
            ))
        }

        _voteRecords.value = currentRecords
        updateDisplayInfo()
    }

    fun deleteVoteRecord(record: VoteRecord) {
        val currentRecords = _voteRecords.value ?: mutableListOf()
        currentRecords.removeAll { it.voterId == record.voterId && it.day == record.day }
        _voteRecords.value = currentRecords
        updateDisplayInfo()
    }

    // Update display info - all days
    private fun updateDisplayInfo() {
        val allDays = getAllDays()

        if (allDays.isEmpty()) {
            _allRecordsText.value = "暂无记录"
            return
        }

        val sb = StringBuilder()
        allDays.forEach { day ->
            sb.append("【第${day}天】\n")

            // Speech records
            val speeches = getSpeechRecordsForDay(day)
            sb.append("📝 发言: ")
            if (speeches.isEmpty()) {
                sb.append("无\n")
            } else {
                sb.append("\n")
                speeches.forEach { record ->
                    sb.append("  ${record.playerId}号: ${record.summary}\n")
                }
            }

            // Vote records
            val votes = getVoteRecordsForDay(day)
            sb.append("🗳️ 投票: ")
            if (votes.isEmpty()) {
                sb.append("无\n")
            } else {
                sb.append("\n")
                // Vote info
                sb.append("  ")
                votes.forEach { record ->
                    sb.append("${record.voterId}→${record.targetId}  ")
                }
                sb.append("\n")
                // Vote stats
                val voteCounts = mutableMapOf<Int, Int>()
                votes.forEach { voteCounts[it.targetId] = voteCounts.getOrDefault(it.targetId, 0) + 1 }
                if (voteCounts.isNotEmpty()) {
                    sb.append("  统计: ")
                    sb.append(voteCounts.entries.sortedByDescending { it.value }.joinToString("  ") {
                        "${it.key}号(${it.value}票)"
                    })
                    sb.append("\n")
                }
            }

            sb.append("\n")
        }

        _allRecordsText.value = sb.toString().trim()
    }

    // Helper methods
    fun getPlayerById(playerId: Int): Player? {
        return _players.value?.find { it.id == playerId }
    }

    fun getSpeechRecordsForDay(day: Int): List<SpeechRecord> {
        return _speechRecords.value?.filter { it.day == day }?.sortedBy { it.playerId } ?: emptyList()
    }

    fun getVoteRecordsForDay(day: Int): List<VoteRecord> {
        return _voteRecords.value?.filter { it.day == day }?.sortedBy { it.voterId } ?: emptyList()
    }

    fun getSpeechRecordForPlayer(playerId: Int, day: Int): SpeechRecord? {
        return _speechRecords.value?.find { it.playerId == playerId && it.day == day }
    }

    fun getVoteRecordForPlayer(playerId: Int, day: Int): VoteRecord? {
        return _voteRecords.value?.find { it.voterId == playerId && it.day == day }
    }

    fun getAllDays(): List<Int> {
        val speechDays = _speechRecords.value?.map { it.day } ?: emptyList()
        val voteDays = _voteRecords.value?.map { it.day } ?: emptyList()
        return (speechDays + voteDays).distinct().sorted()
    }

    // Player status
    fun setPlayerAlive(playerId: Int, isAlive: Boolean) {
        val playerList = _players.value?.toMutableList() ?: return
        val index = playerList.indexOfFirst { it.id == playerId }
        if (index >= 0) {
            playerList[index] = playerList[index].copy(isAlive = isAlive)
            _players.value = playerList
        }
    }

    // Player marked role
    fun setPlayerMarkedRole(playerId: Int, markedRole: Player.MarkedRole) {
        val playerList = _players.value?.toMutableList() ?: return
        val index = playerList.indexOfFirst { it.id == playerId }
        if (index >= 0) {
            playerList[index] = playerList[index].copy(markedRole = markedRole)
            _players.value = playerList
        }
    }
}