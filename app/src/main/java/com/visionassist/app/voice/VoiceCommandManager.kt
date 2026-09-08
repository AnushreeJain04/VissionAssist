package com.visionassist.app.voice

/**
 * Interprets recognized speech text for simple confirmation flows
 * and (later) full voice commands.
 */
object VoiceCommandManager {

    private val affirmativeWords = listOf("yes", "yeah", "yep", "confirm", "sure", "okay", "ok")
    private val negativeWords = listOf("no", "nope", "cancel", "stop")

    fun isAffirmative(text: String): Boolean {
        val lower = text.lowercase()
        return affirmativeWords.any { lower.contains(it) }
    }

    fun isNegative(text: String): Boolean {
        val lower = text.lowercase()
        return negativeWords.any { lower.contains(it) }
    }
}