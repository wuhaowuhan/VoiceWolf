package com.wolfshadow.app.model

/**
 * 发言标签
 */
enum class SpeechTag(val displayName: String) {
    CLAIM_ROLE("跳身份"),
    TAKE_SIDE("站边"),
    ATTACK("攻击"),
    DEFEND("辩护"),
    FISHING("划水"),
    WOLF_TELL("爆狼"),
    SELF_DESTRUCT("自爆")
}