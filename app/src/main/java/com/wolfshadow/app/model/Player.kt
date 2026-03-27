package com.wolfshadow.app.model

/**
 * 玩家模型
 */
data class Player(
    val id: Int,
    val name: String = "玩家$id",
    val isAlive: Boolean = true,
    val markedRole: MarkedRole = MarkedRole.NONE
)