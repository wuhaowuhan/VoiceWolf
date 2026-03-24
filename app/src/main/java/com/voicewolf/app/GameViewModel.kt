package com.voicewolf.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel for Werewolf game state management
 * Simplified for player perspective - focuses on speech and vote recording
 */
class GameViewModel : ViewModel() {

    // Current round
    private val _currentRound = MutableLiveData(1)
    val currentRound: LiveData<Int> = _currentRound

    // Players
    private val _players = MutableLiveData<List<Player>>(emptyList())
    val players: LiveData<List<Player>> = _players

    // Speech records
    private val _speechRecords = MutableLiveData<MutableList<SpeechRecord>>(mutableListOf())
    val speechRecords: LiveData<MutableList<SpeechRecord>> = _speechRecords

    // Vote records
    private val _voteRecords = MutableLiveData<MutableList<VoteRecord>>(mutableListOf())
    val voteRecords: LiveData<MutableList<VoteRecord>> = _voteRecords

    // Display info
    private val _speechInfo = MutableLiveData("")
    val speechInfo: LiveData<String> = _speechInfo

    private val _voteInfo = MutableLiveData("")
    val voteInfo: LiveData<String> = _voteInfo

    private val _voteStats = MutableLiveData("")
    val voteStats: LiveData<String> = _voteStats

    init {
        initializePlayers()
    }

    private fun initializePlayers() {
        val playerList = (1..12).map { Player(id = it) }
        _players.value = playerList
    }

    // Round management
    fun nextRound() {
        _currentRound.value = (_currentRound.value ?: 1) + 1
        updateDisplayInfo()
    }

    fun prevRound() {
        val current = _currentRound.value ?: 1
        if (current > 1) {
            _currentRound.value = current - 1
            updateDisplayInfo()
        }
    }

    fun setRound(round: Int) {
        if (round >= 1) {
            _currentRound.value = round
            updateDisplayInfo()
        }
    }

    // Reset game
    fun resetGame() {
        _currentRound.value = 1
        _speechRecords.value = mutableListOf()
        _voteRecords.value = mutableListOf()
        _speechInfo.value = ""
        _voteInfo.value = ""
        _voteStats.value = ""

        // Reset all players
        val resetPlayers = _players.value?.map { player ->
            player.copy(
                isAlive = true,
                role = Player.Role.UNKNOWN,
                votedFor = null
            )
        } ?: emptyList()
        _players.value = resetPlayers
        updateDisplayInfo()
    }

    // Speech record methods
    fun addSpeechRecord(playerId: Int, round: Int, summary: String) {
        val currentRecords = _speechRecords.value ?: mutableListOf()

        // Check if record exists for this player and round
        val existingIndex = currentRecords.indexOfFirst {
            it.playerId == playerId && it.round == round
        }

        if (existingIndex >= 0) {
            // Update existing
            currentRecords[existingIndex] = currentRecords[existingIndex].copy(
                summary = summary
            )
        } else {
            // Add new
            currentRecords.add(SpeechRecord(
                round = round,
                playerId = playerId,
                summary = summary
            ))
        }

        _speechRecords.value = currentRecords
        updateDisplayInfo()
    }

    fun deleteSpeechRecord(record: SpeechRecord) {
        val currentRecords = _speechRecords.value ?: mutableListOf()
        currentRecords.removeAll { it.playerId == record.playerId && it.round == record.round }
        _speechRecords.value = currentRecords
        updateDisplayInfo()
    }

    // Vote record methods
    fun recordVote(voterId: Int, targetId: Int, round: Int) {
        val currentRecords = _voteRecords.value ?: mutableListOf()

        // Remove existing vote for this voter in this round
        currentRecords.removeAll { it.voterId == voterId && it.round == round }

        // Add new vote
        if (targetId > 0) {
            currentRecords.add(VoteRecord(
                round = round,
                voterId = voterId,
                targetId = targetId
            ))
        }

        _voteRecords.value = currentRecords
        updateDisplayInfo()
    }

    fun deleteVoteRecord(record: VoteRecord) {
        val currentRecords = _voteRecords.value ?: mutableListOf()
        currentRecords.removeAll { it.voterId == record.voterId && it.round == record.round }
        _voteRecords.value = currentRecords
        updateDisplayInfo()
    }

    // Update display info
    private fun updateDisplayInfo() {
        val currentRound = _currentRound.value ?: 1

        // Update speech info
        val speechRecords = _speechRecords.value?.filter { it.round == currentRound } ?: emptyList()
        _speechInfo.value = if (speechRecords.isEmpty()) {
            "暂无发言记录"
        } else {
            speechRecords.sortedBy { it.playerId }.joinToString("\n\n") {
                "${it.playerId}号: ${it.summary}"
            }
        }

        // Update vote info
        val voteRecords = _voteRecords.value?.filter { it.round == currentRound } ?: emptyList()
        _voteInfo.value = if (voteRecords.isEmpty()) {
            "暂无投票记录"
        } else {
            voteRecords.sortedBy { it.voterId }.joinToString("  ") {
                "${it.voterId}→${it.targetId}"
            }
        }

        // Update vote stats
        val voteCounts = mutableMapOf<Int, Int>()
        voteRecords.forEach { voteCounts[it.targetId] = voteCounts.getOrDefault(it.targetId, 0) + 1 }
        _voteStats.value = if (voteCounts.isEmpty()) {
            ""
        } else {
            voteCounts.entries.sortedByDescending { it.value }.joinToString("  ") {
                "${it.key}号: ${it.value}票"
            }
        }
    }

    // Helper methods
    fun getPlayerById(playerId: Int): Player? {
        return _players.value?.find { it.id == playerId }
    }

    fun getSpeechRecordsForRound(round: Int): List<SpeechRecord> {
        return _speechRecords.value?.filter { it.round == round }?.sortedBy { it.playerId } ?: emptyList()
    }

    fun getVoteRecordsForRound(round: Int): List<VoteRecord> {
        return _voteRecords.value?.filter { it.round == round }?.sortedBy { it.voterId } ?: emptyList()
    }

    fun getSpeechRecordForPlayer(playerId: Int, round: Int): SpeechRecord? {
        return _speechRecords.value?.find { it.playerId == playerId && it.round == round }
    }

    fun getVoteRecordForPlayer(playerId: Int, round: Int): VoteRecord? {
        return _voteRecords.value?.find { it.voterId == playerId && it.round == round }
    }

    fun getAllRounds(): List<Int> {
        val speechRounds = _speechRecords.value?.map { it.round } ?: emptyList()
        val voteRounds = _voteRecords.value?.map { it.round } ?: emptyList()
        return (speechRounds + voteRounds).distinct().sorted()
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
}