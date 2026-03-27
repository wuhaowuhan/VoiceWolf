package com.wolfshadow.app.model

/**
 * 投票记录
 */
data class VoteRecord(
    val day: Int,
    val voterId: Int,
    val targetId: Int,  // 0 = 弃票
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isAbstain(): Boolean = targetId == 0
}