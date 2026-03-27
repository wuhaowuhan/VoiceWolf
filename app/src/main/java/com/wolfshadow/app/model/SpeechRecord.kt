package com.wolfshadow.app.model

/**
 * 发言记录
 */
data class SpeechRecord(
    val day: Int,
    val playerId: Int,
    val tags: List<SpeechTag>,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formatDisplay(): String {
        val tagsText = if (tags.isNotEmpty()) {
            tags.joinToString("、") { it.displayName }
        } else {
            ""
        }
        return if (tagsText.isNotEmpty() && summary.isNotEmpty()) {
            "$tagsText | $summary"
        } else if (tagsText.isNotEmpty()) {
            tagsText
        } else {
            summary
        }
    }
}