package com.voicewolf.app

/**
 * Represents a player in the Werewolf game
 */
data class Player(
    val id: Int,
    val name: String = "Player $id",
    val isAlive: Boolean = true,
    val role: Role = Role.UNKNOWN,
    val markedRole: MarkedRole = MarkedRole.NONE
) {
    /**
     * Marked role for recording player analysis
     */
    enum class MarkedRole {
        NONE,
        SEER,        // 预言家
        GOOD,        // 好人
        WEREWOLF,    // 狼人
        VILLAGER,    // 平民
        WITCH,       // 女巫
        HUNTER,      // 猎人
        GUARD,       // 守卫
        MECHANICAL_WOLF  // 机械狼
    }

    enum class Role {
        UNKNOWN,
        // Good roles
        VILLAGER,
        SEER,
        WITCH,
        HUNTER,
        GUARD,
        KNIGHT,
        MEDIUM,
        // Evil roles
        WEREWOLF,
        WEREWOLF_BEAUTY,
        MECHANICAL_WOLF
    }

    fun getRoleDisplayName(): String {
        return when (role) {
            Role.UNKNOWN -> "未知"
            Role.VILLAGER -> "村民"
            Role.SEER -> "预言家"
            Role.WITCH -> "女巫"
            Role.HUNTER -> "猎人"
            Role.GUARD -> "守卫"
            Role.KNIGHT -> "骑士"
            Role.MEDIUM -> "通灵师"
            Role.WEREWOLF -> "狼人"
            Role.WEREWOLF_BEAUTY -> "狼美人"
            Role.MECHANICAL_WOLF -> "机械狼"
        }
    }

    fun getMarkedRoleDisplayName(): String {
        return when (markedRole) {
            MarkedRole.NONE -> ""
            MarkedRole.SEER -> "预言"
            MarkedRole.GOOD -> "好人"
            MarkedRole.WEREWOLF -> "狼人"
            MarkedRole.VILLAGER -> "平民"
            MarkedRole.WITCH -> "女巫"
            MarkedRole.HUNTER -> "猎人"
            MarkedRole.GUARD -> "守卫"
            MarkedRole.MECHANICAL_WOLF -> "机械狼"
        }
    }

    fun isMarkedEvil(): Boolean {
        return markedRole in listOf(MarkedRole.WEREWOLF, MarkedRole.MECHANICAL_WOLF)
    }

    fun isEvil(): Boolean {
        return role in listOf(Role.WEREWOLF, Role.WEREWOLF_BEAUTY, Role.MECHANICAL_WOLF)
    }

    fun isGood(): Boolean {
        return role in listOf(Role.VILLAGER, Role.SEER, Role.WITCH, Role.HUNTER, Role.GUARD, Role.KNIGHT, Role.MEDIUM)
    }
}

/**
 * Represents a speech record for a player
 */
data class SpeechRecord(
    val day: Int,
    val playerId: Int,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents a vote record
 */
data class VoteRecord(
    val day: Int,
    val voterId: Int,
    val targetId: Int,  // 0 = abstain
    val timestamp: Long = System.currentTimeMillis()
)