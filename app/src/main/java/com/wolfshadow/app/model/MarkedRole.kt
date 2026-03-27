package com.wolfshadow.app.model

/**
 * 玩家身份标记
 */
enum class MarkedRole(val displayName: String, val colorKey: String) {
    NONE("", "gray"),
    SEER("预言", "seer"),
    GOOD("好人", "good"),
    WEREWOLF("狼人", "werewolf"),
    VILLAGER("平民", "villager"),
    WITCH("女巫", "witch"),
    HUNTER("猎人", "hunter"),
    GUARD("守卫", "guard"),
    MECHANICAL_WOLF("机械狼", "mech_wolf");

    fun isEvil(): Boolean = this in listOf(WEREWOLF, MECHANICAL_WOLF)
}