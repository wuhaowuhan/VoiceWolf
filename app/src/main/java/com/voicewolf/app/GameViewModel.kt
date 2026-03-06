package com.voicewolf.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel for Werewolf game state management
 */
class GameViewModel : ViewModel() {

    // Game state
    private val _isGameStarted = MutableLiveData(false)
    val isGameStarted: LiveData<Boolean> = _isGameStarted

    private val _currentRound = MutableLiveData(1)
    val currentRound: LiveData<Int> = _currentRound

    private val _isDayPhase = MutableLiveData(false)
    val isDayPhase: LiveData<Boolean> = _isDayPhase

    private val _selectedGameMode = MutableLiveData<GameMode>(GameMode.MECHANIC_WOLF)
    val selectedGameMode: LiveData<GameMode> = _selectedGameMode

    // Players
    private val _players = MutableLiveData<List<Player>>(emptyList())
    val players: LiveData<List<Player>> = _players

    // Selected player for actions
    private val _selectedPlayer = MutableLiveData<Player?>(null)
    val selectedPlayer: LiveData<Player?> = _selectedPlayer

    // Game events
    private val _gameEvents = MutableLiveData<MutableList<GameEvent>>(mutableListOf())
    val gameEvents: LiveData<MutableList<GameEvent>> = _gameEvents

    // Speech records
    private val _speechRecords = MutableLiveData<MutableList<SpeechRecord>>(mutableListOf())
    val speechRecords: LiveData<MutableList<SpeechRecord>> = _speechRecords

    // Vote records
    private val _voteRecords = MutableLiveData<MutableList<VoteRecord>>(mutableListOf())
    val voteRecords: LiveData<MutableList<VoteRecord>> = _voteRecords

    // Display info
    private val _deathInfo = MutableLiveData("")
    val deathInfo: LiveData<String> = _deathInfo

    private val _voteInfo = MutableLiveData("")
    val voteInfo: LiveData<String> = _voteInfo

    private val _speakerText = MutableLiveData("")
    val speakerText: LiveData<String> = _speakerText

    private val _speakerTimer = MutableLiveData("")
    val speakerTimer: LiveData<String> = _speakerTimer

    private var speakerTimerTask: kotlinx.coroutines.Job? = null
    private val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    init {
        initializePlayers()
    }

    private fun initializePlayers() {
        val playerList = (1..12).map { Player(id = it) }
        _players.value = playerList
    }

    // Game control methods
    fun setGameMode(gameMode: GameMode) {
        _selectedGameMode.value = gameMode
    }

    fun startGame() {
        _isGameStarted.value = true
        _currentRound.value = 1
        _isDayPhase.value = false
        addGameEvent(GameEvent(
            type = GameEvent.EventType.CUSTOM,
            playerId = 0,
            description = "游戏开始！版型：${_selectedGameMode.value?.displayName}"
        ))
    }

    fun resetGame() {
        _isGameStarted.value = false
        _isDayPhase.value = false
        _currentRound.value = 1
        _deathInfo.value = ""
        _voteInfo.value = ""
        _speakerText.value = ""
        _speakerTimer.value = ""
        _gameEvents.value = mutableListOf()
        _speechRecords.value = mutableListOf()
        _voteRecords.value = mutableListOf()

        // Cancel speaker timer
        speakerTimerTask?.cancel()

        // Reset all players
        val resetPlayers = _players.value?.map { player ->
            player.copy(
                isAlive = true,
                role = Player.Role.UNKNOWN,
                votedFor = null,
                wasVotedBy = mutableListOf()
            )
        } ?: emptyList()
        _players.value = resetPlayers
    }

    fun toggleDayNight() {
        val wasDay = _isDayPhase.value ?: false
        _isDayPhase.value = !wasDay

        if (!wasDay) {
            // Now it's day
            addGameEvent(GameEvent(
                type = GameEvent.EventType.CUSTOM,
                playerId = 0,
                description = "第${_currentRound.value}天开始"
            ))
        } else {
            // Now it's night, increment round
            _currentRound.value = (_currentRound.value ?: 1) + 1
            addGameEvent(GameEvent(
                type = GameEvent.EventType.CUSTOM,
                playerId = 0,
                description = "第${_currentRound.value}夜开始"
            ))
        }
    }
    
    // Get current game state description
    fun getGameStateDescription(): String {
        val round = _currentRound.value ?: 1
        val isDay = _isDayPhase.value ?: false
        return "第${round}${if (isDay) "天" else "夜"}"
    }
    
    // Get current game state color
    fun getGameStateColor(): Int {
        return if (_isDayPhase.value ?: false) {
            android.R.color.holo_blue_light
        } else {
            android.R.color.holo_purple
        }
    }

    fun nextRound() {
        _currentRound.value = (_currentRound.value ?: 1) + 1
        addGameEvent(GameEvent(
            type = GameEvent.EventType.CUSTOM,
            playerId = 0,
            description = "进入第${_currentRound.value}轮"
        ))
    }

    // Player methods
    fun selectPlayer(player: Player) {
        _selectedPlayer.value = player
    }

    fun clearSelectedPlayer() {
        _selectedPlayer.value = null
    }

    fun setPlayerRole(playerId: Int, role: Player.Role) {
        val playerList = _players.value?.toMutableList() ?: return
        val index = playerList.indexOfFirst { it.id == playerId }
        if (index >= 0) {
            playerList[index] = playerList[index].copy(role = role)
            _players.value = playerList
        }
    }

    fun markPlayerDead(playerId: Int) {
        val playerList = _players.value?.toMutableList() ?: return
        val index = playerList.indexOfFirst { it.id == playerId }
        if (index >= 0 && playerList[index].isAlive) {
            playerList[index] = playerList[index].copy(isAlive = false)
            _players.value = playerList

            addGameEvent(GameEvent(
                type = GameEvent.EventType.DEATH,
                playerId = playerId,
                description = "${playerId}号玩家出局"
            ))

            updateDeathInfo()
        }
    }

    fun recordVote(voterId: Int, targetId: Int) {
        val playerList = _players.value?.toMutableList() ?: return

        // Update voter
        val voterIndex = playerList.indexOfFirst { it.id == voterId }
        if (voterIndex >= 0) {
            playerList[voterIndex] = playerList[voterIndex].copy(
                votedFor = targetId
            )
        }

        // Update target's wasVotedBy
        val targetIndex = playerList.indexOfFirst { it.id == targetId }
        if (targetIndex >= 0) {
            val updatedWasVotedBy = playerList[targetIndex].wasVotedBy.toMutableList()
            updatedWasVotedBy.add(voterId)
            playerList[targetIndex] = playerList[targetIndex].copy(
                wasVotedBy = updatedWasVotedBy
            )
        }

        _players.value = playerList

        // Add to vote records
        val currentRecords = _voteRecords.value ?: mutableListOf()
        currentRecords.add(VoteRecord(
            round = _currentRound.value ?: 1,
            voterId = voterId,
            targetId = targetId
        ))
        _voteRecords.value = currentRecords

        // Add game event
        addGameEvent(GameEvent(
            type = GameEvent.EventType.VOTE,
            playerId = voterId,
            targetId = targetId,
            description = "${voterId}号投票给${targetId}号"
        ))

        updateVoteInfo()
    }

    // Speech record methods
    fun addSpeechRecord(playerId: Int, round: Int, summary: String, duration: Int = 0, type: SpeechRecord.SpeechType = SpeechRecord.SpeechType.NORMAL) {
        val currentRecords = _speechRecords.value ?: mutableListOf()

        // Check if record exists for this player and round
        val existingIndex = currentRecords.indexOfFirst {
            it.playerId == playerId && it.round == round
        }

        if (existingIndex >= 0) {
            // Update existing
            currentRecords[existingIndex] = currentRecords[existingIndex].copy(
                summary = summary,
                duration = duration,
                type = type
            )
        } else {
            // Add new
            currentRecords.add(SpeechRecord(
                round = round,
                playerId = playerId,
                summary = summary,
                duration = duration,
                type = type
            ))
        }

        _speechRecords.value = currentRecords
    }

    // Info update methods
    private fun updateDeathInfo() {
        val deadPlayers = _players.value?.filter { !it.isAlive } ?: emptyList()
        _deathInfo.value = if (deadPlayers.isNotEmpty()) {
            "出局玩家：${deadPlayers.joinToString { "${it.id}号" }}"
        } else {
            ""
        }
    }

    private fun updateVoteInfo() {
        val votes = _players.value?.filter { it.votedFor != null } ?: emptyList()
        _voteInfo.value = if (votes.isNotEmpty()) {
            "投票记录：${votes.joinToString { "${it.id}→${it.votedFor}号" }}"
        } else {
            ""
        }
    }
    
    // Get vote statistics for current round
    fun getVoteStatistics(): Map<Int, Int> {
        val currentRound = _currentRound.value ?: 1
        val roundVotes = _voteRecords.value?.filter { it.round == currentRound } ?: emptyList()
        
        val voteCounts = mutableMapOf<Int, Int>()
        roundVotes.forEach {
            voteCounts[it.targetId] = voteCounts.getOrDefault(it.targetId, 0) + 1
        }
        
        return voteCounts
    }
    
    // Get player with most votes in current round
    fun getMostVotedPlayer(): Pair<Int, Int>? {
        val voteCounts = getVoteStatistics()
        return voteCounts.maxByOrNull { it.value }?.toPair()
    }
    
    // Get vote history for all rounds
    fun getVoteHistory(): Map<Int, Map<Int, Int>> {
        val allVotes = _voteRecords.value ?: emptyList()
        val roundVotes = mutableMapOf<Int, MutableMap<Int, Int>>()
        
        allVotes.forEach {
            if (!roundVotes.containsKey(it.round)) {
                roundVotes[it.round] = mutableMapOf()
            }
            roundVotes[it.round]?.let {roundMap ->
                roundMap[it.targetId] = roundMap.getOrDefault(it.targetId, 0) + 1
            }
        }
        
        return roundVotes
    }

    fun setSpeaker(playerId: Int, duration: Int = 180) {
        _speakerText.value = "【${playerId}号】发言中..."
        
        // Cancel any existing timer
        speakerTimerTask?.cancel()
        
        // Start new timer
        var timeLeft = duration
        _speakerTimer.value = "${timeLeft} 秒"
        
        speakerTimerTask = coroutineScope.launch {
            while (timeLeft > 0) {
                kotlinx.coroutines.delay(1000)
                timeLeft--
                _speakerTimer.value = "${timeLeft} 秒"
            }
            _speakerText.value = ""
            _speakerTimer.value = ""
        }
    }
    
    fun stopSpeakerTimer() {
        speakerTimerTask?.cancel()
        _speakerText.value = ""
        _speakerTimer.value = ""
    }

    fun addGameEvent(event: GameEvent) {
        val currentEvents = _gameEvents.value ?: mutableListOf()
        currentEvents.add(0, event) // Add to top
        _gameEvents.value = currentEvents
    }

    // Helper methods for UI
    fun getPlayerById(playerId: Int): Player? {
        return _players.value?.find { it.id == playerId }
    }

    fun getSpeechRecordsForPlayer(playerId: Int): List<SpeechRecord> {
        return _speechRecords.value?.filter { it.playerId == playerId }?.sortedBy { it.round } ?: emptyList()
    }

    fun getVoteRecordsForPlayer(playerId: Int): List<VoteRecord> {
        return _voteRecords.value?.filter { it.voterId == playerId }?.sortedBy { it.round } ?: emptyList()
    }

    fun getAlivePlayers(): List<Player> {
        return _players.value?.filter { it.isAlive } ?: emptyList()
    }
}
