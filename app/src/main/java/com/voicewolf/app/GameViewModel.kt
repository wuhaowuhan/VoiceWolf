package com.voicewolf.app

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel for Werewolf game state management
 * Simplified for player perspective - focuses on speech and vote recording
 */
class GameViewModel : ViewModel() {

    // Current day (0 = 警上, 1 = 第1天, 2 = 第2天...)
    private val _currentDay = MutableLiveData(0)
    val currentDay: LiveData<Int> = _currentDay

    // Players - now supports dynamic count (8-15)
    private val _players = MutableLiveData<List<Player>>(emptyList())
    val players: LiveData<List<Player>> = _players

    // Active player IDs for quick access
    private val _activePlayerIds = MutableLiveData<Set<Int>>(emptySet())
    val activePlayerIds: LiveData<Set<Int>> = _activePlayerIds

    // Speech records
    private val _speechRecords = MutableLiveData<MutableList<SpeechRecord>>(mutableListOf())
    val speechRecords: LiveData<MutableList<SpeechRecord>> = _speechRecords

    // Vote records
    private val _voteRecords = MutableLiveData<MutableList<VoteRecord>>(mutableListOf())
    val voteRecords: LiveData<MutableList<VoteRecord>> = _voteRecords

    // Display info - all records formatted as rich text
    private val _allRecordsText = MutableLiveData<CharSequence>("")
    val allRecordsText: LiveData<CharSequence> = _allRecordsText

    // Current game setup (nullable - may not be set yet)
    private val _currentSetup = MutableLiveData<GameSetup?>()
    val currentSetup: LiveData<GameSetup?> = _currentSetup

    // Constants
    companion object {
        const val MIN_PLAYERS = 12
        const val DEFAULT_PLAYERS = 12
        const val MAX_PLAYERS = 15
    }

    init {
        initializePlayers(DEFAULT_PLAYERS)
        updateDisplayInfo()
    }

    fun initializePlayers(count: Int) {
        // Create players with specified count (12-15)
        val validCount = count.coerceIn(MIN_PLAYERS, MAX_PLAYERS)
        val playerList = (1..validCount).map { Player(id = it) }
        _players.value = playerList
        updateActivePlayerIds()
    }

    private fun initializePlayers() {
        initializePlayers(DEFAULT_PLAYERS)
    }

    private fun updateActivePlayerIds() {
        _activePlayerIds.value = _players.value?.filter { it.isActive }?.map { it.id }?.toSet() ?: emptySet()
    }

    // Check if game has started (has any records)
    fun hasGameStarted(): Boolean {
        return (_speechRecords.value?.isNotEmpty() == true) ||
               (_voteRecords.value?.isNotEmpty() == true)
    }

    // Get active player count
    fun getActivePlayerCount(): Int {
        return _activePlayerIds.value?.size ?: DEFAULT_PLAYERS
    }

    // Add a new player (only if game not started and count < MAX)
    fun addPlayer(): Boolean {
        if (hasGameStarted()) return false

        val currentCount = getActivePlayerCount()
        if (currentCount >= MAX_PLAYERS) return false

        val currentPlayers = _players.value?.toMutableList() ?: return false

        // Find the next available ID (might be a removed player's ID)
        val allIds = currentPlayers.map { it.id }
        val nextId = (currentCount + 1..MAX_PLAYERS).firstOrNull { it !in allIds }
            ?: ((allIds.maxOrNull() ?: DEFAULT_PLAYERS) + 1)

        if (nextId > MAX_PLAYERS) return false

        // Add new player
        currentPlayers.add(Player(id = nextId, isActive = true))
        _players.value = currentPlayers.sortedBy { it.id }
        updateActivePlayerIds()
        return true
    }

    // Remove a player (only if game not started and ID >= 13)
    fun removePlayer(playerId: Int): Boolean {
        if (hasGameStarted()) return false
        if (playerId < 13) return false  // Only 13-15 can be removed

        val currentPlayers = _players.value?.toMutableList() ?: return false
        val index = currentPlayers.indexOfFirst { it.id == playerId }
        if (index < 0) return false

        // Mark player as inactive instead of removing (keep ID)
        currentPlayers[index] = currentPlayers[index].copy(isActive = false)
        _players.value = currentPlayers
        updateActivePlayerIds()
        return true
    }

    // Get list of active players for UI display
    fun getActivePlayers(): List<Player> {
        return _players.value?.filter { it.isActive } ?: emptyList()
    }

    // Day management
    fun nextDay() {
        _currentDay.value = (_currentDay.value ?: 0) + 1
    }

    fun prevDay() {
        val current = _currentDay.value ?: 0
        if (current > 0) {
            _currentDay.value = current - 1
        }
    }

    fun setDay(day: Int) {
        if (day >= 0) {
            _currentDay.value = day
        }
    }

    // Get display text for current day
    fun getDayDisplayText(): String {
        val day = _currentDay.value ?: 0
        return if (day == 0) "警上" else "第${day}天"
    }

    // Reset game
    fun resetGame(keepSetup: Boolean = true) {
        _currentDay.value = 0  // Reset to 警上
        _speechRecords.value = mutableListOf()
        _voteRecords.value = mutableListOf()
        _allRecordsText.value = ""

        // Reset to default 12 players
        val resetPlayers = (1..DEFAULT_PLAYERS).map { Player(id = it) }
        _players.value = resetPlayers
        updateActivePlayerIds()
        updateDisplayInfo()

        // Clear setup if not keeping
        if (!keepSetup) {
            _currentSetup.value = null
        }
    }

    // Set game setup
    fun setSetup(setup: GameSetup) {
        _currentSetup.value = setup
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

    // Update display info - all days with rich text formatting
    private fun updateDisplayInfo() {
        val allDays = getAllDays()

        if (allDays.isEmpty()) {
            _allRecordsText.value = "暂无记录"
            return
        }

        val sb = SpannableStringBuilder()
        val colorRed    = Color.parseColor("#FF6B6B")  // 被投号码 - 亮红
        val colorWhite  = Color.WHITE
        val colorBlue   = Color.parseColor("#60A5FA")  // 区块标签 - 蓝
        val colorGray   = Color.parseColor("#B0B0B0")  // 发言内容 - 浅灰
        val colorDimGray = Color.parseColor("#6B7280") // 投票人号、箭头 - 暗灰

        fun SpannableStringBuilder.appendColored(text: String, color: Int): SpannableStringBuilder {
            val start = length
            append(text)
            setSpan(ForegroundColorSpan(color), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return this
        }

        fun SpannableStringBuilder.appendBold(text: String, color: Int): SpannableStringBuilder {
            val start = length
            append(text)
            setSpan(ForegroundColorSpan(color), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return this
        }

        allDays.forEachIndexed { index, day ->
            if (index > 0) sb.append("\n")

            // Day header: bold white
            sb.appendBold("【${getDayDisplayText(day)}】", colorWhite)
            sb.append("\n")

            val speeches = getSpeechRecordsForDay(day)
            val votes = getVoteRecordsForDay(day)

            // Speech section
            if (speeches.isNotEmpty()) {
                sb.appendColored("📝 发言\n", colorBlue)
                speeches.forEach { record ->
                    sb.append("  ")
                    sb.appendColored("${record.playerId}号", colorWhite)
                    sb.appendColored(": ${record.summary}\n", colorGray)
                }
            }

            // Vote section
            if (votes.isNotEmpty()) {
                sb.appendColored("🗳️ 投票\n", colorBlue)

                // Vote arrows line: "  N→M  N→M"
                sb.append("  ")
                votes.forEach { record ->
                    sb.appendColored("${record.voterId}", colorDimGray)
                    sb.appendColored("→", colorDimGray)
                    sb.appendColored("${record.targetId}", colorRed)
                    sb.append("  ")
                }
                sb.append("\n")

                // Vote stats: "  3号×2  5号×1"
                val voteCounts = mutableMapOf<Int, Int>()
                votes.forEach { voteCounts[it.targetId] = voteCounts.getOrDefault(it.targetId, 0) + 1 }
                if (voteCounts.isNotEmpty()) {
                    sb.append("  ")
                    voteCounts.entries.sortedByDescending { it.value }.forEach { (targetId, count) ->
                        sb.appendColored("${targetId}号", colorRed)
                        sb.appendColored("×${count}  ", colorGray)
                    }
                    sb.append("\n")
                }
            }

            // Both empty
            if (speeches.isEmpty() && votes.isEmpty()) {
                sb.appendColored("  （暂无）\n", colorDimGray)
            }
        }

        _allRecordsText.value = sb
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

    // Get display text for a specific day
    fun getDayDisplayText(day: Int): String {
        return if (day == 0) "警上" else "第${day}天"
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