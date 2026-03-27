package com.wolfshadow.app.model

/**
 * 游戏状态
 */
data class GameState(
    val currentDay: Int = 1,
    val players: List<Player> = (1..12).map { Player(id = it) },
    val speechRecords: List<SpeechRecord> = emptyList(),
    val voteRecords: List<VoteRecord> = emptyList()
) {
    fun getAllDays(): List<Int> {
        val speechDays = speechRecords.map { it.day }
        val voteDays = voteRecords.map { it.day }
        return (speechDays + voteDays).distinct().sorted()
    }
}