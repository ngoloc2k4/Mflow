package com.lobie.mflow

import com.lobie.mflow.data.parser.LyricsParser
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsParserTest {

    @Test
    fun parse_validLrcString_returnsOrderedLines() {
        val lrc = """
            [00:01.00]First line
            [00:05.50]Second line with decimal
            [01:10.200]Third line after one minute
        """.trimIndent()

        val parsed = LyricsParser.parse(lrc)

        assertEquals(3, parsed.size)
        assertEquals(1000L, parsed[0].timeMs)
        assertEquals("First line", parsed[0].text)

        assertEquals(5500L, parsed[1].timeMs)
        assertEquals("Second line with decimal", parsed[1].text)

        assertEquals(70200L, parsed[2].timeMs)
        assertEquals("Third line after one minute", parsed[2].text)
    }

    @Test
    fun parse_emptyString_returnsEmptyList() {
        val parsed = LyricsParser.parse("")
        assertEquals(0, parsed.size)
    }
}
