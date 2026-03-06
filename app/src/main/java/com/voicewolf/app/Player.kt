package com.voicewolf.app

/**
 * Represents a player in the Werewolf game
 */
data class Player(
    val id: Int,
    val name: String = "Player $id",
    val isAlive: Boolean = true,
    val role: Role = Role.UNKNOWN,
    val votedFor: Int? = null,
    val wasVotedBy: List<Int> = emptyList()
) {
    enum class Role {
        UNKNOWN,
        GOOD,
        WEREWOLF
    }
}

/**
 * Represents a speech record for a player
 */
data class SpeechRecord(
    val round: Int,
    val playerId: Int,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents a vote record
 */
data class VoteRecord(
    val round: Int,
    val voterId: Int,
    val targetId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Game mode definitions
 */
enum class GameMode(val displayName: String, val description: String) {
    MECHANIC_WOLF("机械狼通灵师", "12 人局：3 小狼 + 机械狼 vs 通灵师 + 女巫 + 猎人 + 守卫 + 4 村民"),
    WOLF_BEAUTY_KNIGHT("狼美骑士", "12 人局：3 小狼 + 狼美 vs 预言家 + 女巫 + 骑士 + 守卫 + 4 村民")
}
