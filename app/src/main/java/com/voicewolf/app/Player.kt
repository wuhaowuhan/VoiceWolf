package com.voicewolf.app

/**
 * Represents a player in the Werewolf game
 */
data class Player(
    var id: Int,
    var name: String = "Player $id",
    var isAlive: Boolean = true,
    var role: Role = Role.UNKNOWN,
    var votedFor: Int? = null,
    var wasVotedBy: List<Int> = emptyList()
) {
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
    
    // Get role display name
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
    
    // Check if role is evil
    fun isEvil(): Boolean {
        return role in listOf(Role.WEREWOLF, Role.WEREWOLF_BEAUTY, Role.MECHANICAL_WOLF)
    }
    
    // Check if role is good
    fun isGood(): Boolean {
        return role in listOf(Role.VILLAGER, Role.SEER, Role.WITCH, Role.HUNTER, Role.GUARD, Role.KNIGHT, Role.MEDIUM)
    }
}

/**
 * Represents a speech record for a player
 */
data class SpeechRecord(
    var round: Int,
    var playerId: Int,
    var summary: String,
    var duration: Int = 0, // 发言时长（秒）
    var type: SpeechType = SpeechType.NORMAL, // 发言类型
    var timestamp: Long = System.currentTimeMillis()
) {
    enum class SpeechType {
        NORMAL,     // 常规发言
        LAST_WORD,  // 遗言
        REPLY       // 反驳
    }
}

/**
 * Represents a vote record
 */
data class VoteRecord(
    var round: Int,
    var voterId: Int,
    var targetId: Int,
    var timestamp: Long = System.currentTimeMillis()
)

/**
 * Game mode definitions
 */
enum class GameMode(val displayName: String, val description: String) {
    STANDARD("标准场", "12 人局：3 小狼 vs 预言家 + 女巫 + 猎人 + 守卫 + 4 村民"),
    WOLF_BEAUTY_KNIGHT("狼美骑士", "12 人局：3 小狼 + 狼美 vs 预言家 + 女巫 + 骑士 + 守卫 + 4 村民"),
    MECHANIC_WOLF("机械狼通灵师", "12 人局：3 小狼 + 机械狼 vs 通灵师 + 女巫 + 猎人 + 守卫 + 4 村民"),
    WHITE_WOLF_KING("白狼王", "12 人局：2 小狼 + 白狼王 vs 预言家 + 女巫 + 猎人 + 守卫 + 4 村民"),
    GARGOYLE_GRAVEDIGGER("石像鬼守墓人", "12 人局：2 小狼 + 石像鬼 vs 预言家 + 女巫 + 猎人 + 守墓人 + 4 村民"),
    FORTUNE_TELLER("预言家丘比特", "12 人局：2 小狼 + 丘比特 vs 预言家 + 女巫 + 猎人 + 守卫 + 4 村民")
}

/**
 * Represents a game event (death, vote, custom events, etc.)
 */
data class GameEvent(
    var type: EventType,
    var playerId: Int,
    var targetId: Int? = null,
    var description: String,
    var timestamp: Long = System.currentTimeMillis()
) {
    enum class EventType {
        CUSTOM,     // 自定义事件（游戏开始、天黑、天亮等）
        DEATH,      // 玩家出局
        VOTE,       // 投票事件
        SKILL,      // 技能使用
        SPEECH      // 发言事件
    }
}
