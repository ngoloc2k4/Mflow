package com.lobie.mflow.data.parser

import java.util.regex.Pattern

data class LyricLine(
    val timeMs: Long,
    val text: String
)

object LyricsParser {
    private val LRC_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{2,3}))?\\](.*)")

    fun parse(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        lrcContent.lineSequence().forEach { line ->
            val matcher = LRC_PATTERN.matcher(line.trim())
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val fracStr = matcher.group(3) ?: "0"
                val frac = if (fracStr.length == 2) fracStr.toLong() * 10 else fracStr.toLong()
                val timeMs = (min * 60 + sec) * 1000 + frac
                val text = matcher.group(4)?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    lines.add(LyricLine(timeMs, text))
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}
