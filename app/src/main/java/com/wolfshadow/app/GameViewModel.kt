package com.wolfshadow.app

import androidx.lifecycle.ViewModel
import com.wolfshadow.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel : ViewModel() {

    // 单一状态源
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // 便捷访问
    val currentDay: Int get() = _gameState.value.currentDay
    val players: List<Player> get() = _gameState.value.players
    val speechRecords: List<SpeechRecord> get() = _gameState.value.speechRecords
    val voteRecords: List<VoteRecord> get() = _gameState.value.voteRecords

    // ========== 玩家操作 ==========

    fun setPlayerMarkedRole(playerId: Int, role: MarkedRole) {
        _gameState.update { state ->
            state.copy(
                players = state.players.map { player ->
                    if (player.id == playerId) player.copy(markedRole = role) else player
                }
            )
        }
    }

    fun setPlayerAlive(playerId: Int, isAlive: Boolean) {
        _gameState.update { state ->
            state.copy(
                players = state.players.map { player ->
                    if (player.id == playerId) player.copy(isAlive = isAlive) else player
                }
            )
        }
    }

    // ========== 发言操作 ==========

    fun addSpeechRecord(record: SpeechRecord) {
        _gameState.update { state ->
            // 先删除同一天同一玩家的记录
            val filtered = state.speechRecords.filterNot {
                it.day == record.day && it.playerId == record.playerId
            }
            state.copy(speechRecords = filtered + record)
        }
    }

    fun deleteSpeechRecord(day: Int, playerId: Int) {
        _gameState.update { state ->
            state.copy(
                speechRecords = state.speechRecords.filterNot {
                    it.day == day && it.playerId == playerId
                }
            )
        }
    }

    // ========== 投票操作 ==========

    fun recordVote(voterId: Int, targetId: Int, day: Int) {
        _gameState.update { state ->
            // 先删除该投票者当天的记录
            val filtered = state.voteRecords.filterNot {
                it.voterId == voterId && it.day == day
            }
            // 如果不是弃票，添加新记录
            val newRecords = if (targetId > 0) {
                filtered + VoteRecord(day = day, voterId = voterId, targetId = targetId)
            } else {
                filtered
            }
            state.copy(voteRecords = newRecords)
        }
    }

    fun deleteVoteRecord(day: Int, voterId: Int) {
        _gameState.update { state ->
            state.copy(
                voteRecords = state.voteRecords.filterNot {
                    it.day == day && it.voterId == voterId
                }
            )
        }
    }

    // ========== 天数操作 ==========

    fun nextDay() {
        _gameState.update { state ->
            state.copy(currentDay = state.currentDay + 1)
        }
    }

    fun prevDay() {
        _gameState.update { state ->
            if (state.currentDay > 1) {
                state.copy(currentDay = state.currentDay - 1)
            } else {
                state
            }
        }
    }

    fun setDay(day: Int) {
        if (day >= 1) {
            _gameState.update { state ->
                state.copy(currentDay = day)
            }
        }
    }

    // ========== 游戏操作 ==========

    fun resetGame() {
        _gameState.value = GameState()
    }

    // ========== 查询方法 ==========

    fun getPlayerById(id: Int): Player? {
        return _gameState.value.players.find { it.id == id }
    }

    fun getSpeechRecordsForPlayer(playerId: Int): List<SpeechRecord> {
        return _gameState.value.speechRecords
            .filter { it.playerId == playerId }
            .sortedBy { it.day }
    }

    fun getSpeechRecordsForDay(day: Int): List<SpeechRecord> {
        return _gameState.value.speechRecords
            .filter { it.day == day }
            .sortedBy { it.playerId }
    }

    fun getSpeechRecord(day: Int, playerId: Int): SpeechRecord? {
        return _gameState.value.speechRecords.find {
            it.day == day && it.playerId == playerId
        }
    }

    fun getVoteRecordsForPlayer(playerId: Int): List<VoteRecord> {
        return _gameState.value.voteRecords
            .filter { it.voterId == playerId }
            .sortedBy { it.day }
    }

    fun getVoteRecordsForDay(day: Int): List<VoteRecord> {
        return _gameState.value.voteRecords
            .filter { it.day == day }
            .sortedBy { it.voterId }
    }

    fun getVoteCountsForDay(day: Int): Map<Int, Int> {
        return getVoteRecordsForDay(day)
            .filter { !it.isAbstain() }
            .groupingBy { it.targetId }
            .eachCount()
    }

    fun getAbstainVotersForDay(day: Int): List<Int> {
        return getVoteRecordsForDay(day)
            .filter { it.isAbstain() }
            .map { it.voterId }
    }
}