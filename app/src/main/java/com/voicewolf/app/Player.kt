package com.voicewolf.app

/**
 * Represents a player in the Werewolf game
 */
data class Player(
    val id: Int,
    val name: String = "Player $id",
    val isAlive: Boolean = true,
    val role: Role = Role.UNKNOWN,
    val markedRole: MarkedRole = MarkedRole.NONE,
    val isActive: Boolean = true  // 玩家是否在当前游戏中（用于动态人数）
) {
    /**
     * Marked role for recording player analysis
     */
    enum class MarkedRole {
        NONE,
        // 好人阵营
        SEER,           // 预言家
        SEER_MIRROR,    // 通灵师
        WITCH,          // 女巫
        HUNTER,         // 猎人
        GUARD,          // 守卫
        KNIGHT,         // 骑士
        IDIOT,          // 白痴
        VILLAGER,       // 平民
        GOOD,           // 好人（泛指）
        // 狼人阵营
        WEREWOLF,       // 狼人
        WOLF_KING,      // 狼王
        WOLF_BEAUTY,    // 狼美人
        MECHANICAL_WOLF // 机械狼
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
            MarkedRole.SEER_MIRROR -> "通灵"
            MarkedRole.GOOD -> "好人"
            MarkedRole.WEREWOLF -> "狼人"
            MarkedRole.WOLF_KING -> "狼王"
            MarkedRole.WOLF_BEAUTY -> "狼美"
            MarkedRole.VILLAGER -> "平民"
            MarkedRole.WITCH -> "女巫"
            MarkedRole.HUNTER -> "猎人"
            MarkedRole.GUARD -> "守卫"
            MarkedRole.KNIGHT -> "骑士"
            MarkedRole.IDIOT -> "白痴"
            MarkedRole.MECHANICAL_WOLF -> "机械狼"
        }
    }

    fun isMarkedEvil(): Boolean {
        return markedRole in listOf(MarkedRole.WEREWOLF, MarkedRole.WOLF_KING, MarkedRole.WOLF_BEAUTY, MarkedRole.MECHANICAL_WOLF)
    }

    fun isEvil(): Boolean {
        return role in listOf(Role.WEREWOLF, Role.WEREWOLF_BEAUTY, Role.MECHANICAL_WOLF)
    }

    fun isGood(): Boolean {
        return role in listOf(Role.VILLAGER, Role.SEER, Role.WITCH, Role.HUNTER, Role.GUARD, Role.KNIGHT, Role.MEDIUM)
    }
}

// Extension function for MarkedRole enum
fun Player.MarkedRole.getMarkedRoleDisplayName(): String {
    return when (this) {
        Player.MarkedRole.NONE -> ""
        Player.MarkedRole.SEER -> "预言"
        Player.MarkedRole.SEER_MIRROR -> "通灵"
        Player.MarkedRole.GOOD -> "好人"
        Player.MarkedRole.WEREWOLF -> "狼人"
        Player.MarkedRole.WOLF_KING -> "狼王"
        Player.MarkedRole.WOLF_BEAUTY -> "狼美"
        Player.MarkedRole.VILLAGER -> "平民"
        Player.MarkedRole.WITCH -> "女巫"
        Player.MarkedRole.HUNTER -> "猎人"
        Player.MarkedRole.GUARD -> "守卫"
        Player.MarkedRole.KNIGHT -> "骑士"
        Player.MarkedRole.IDIOT -> "白痴"
        Player.MarkedRole.MECHANICAL_WOLF -> "机械狼"
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