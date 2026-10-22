package com.position.nightneko

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class NightTimeTest {
    private fun rewrite(text: String, hour: Int): String {
        val replacement = NightTime.replacement(text, hour) ?: return text
        return text.replaceRange(replacement.start, replacement.end, replacement.text)
    }

    @Test
    fun everyMinuteAcrossTheDay() {
        for (hour in 0..23) for (minute in 0..59) {
            val original = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
            val expected = String.format(Locale.ROOT, "%02d:%02d", if (hour < 4) hour + 24 else hour, minute)
            assertEquals(expected, rewrite(original, hour))
        }
    }

    @Test
    fun midnightAndFourOClockBoundaries() {
        assertEquals("23:59", rewrite("23:59", 23))
        assertEquals("24:00", rewrite("00:00", 0))
        assertEquals("27:59", rewrite("03:59", 3))
        assertEquals("04:00", rewrite("04:00", 4))
    }

    @Test
    fun splitHoursAndSeconds() {
        assertEquals("24", rewrite("00", 0))
        assertEquals("27", rewrite("3", 3))
        assertEquals("25:02:03", rewrite("01:02:03", 1))
        assertEquals("26：12", rewrite("02：12", 2))
    }

    @Test
    fun preservesDirectionMarksAndDigitFamily() {
        assertEquals("\u200f٢٥:٣٠", rewrite("\u200f٠١:٣٠", 1))
        assertEquals("２７:５９", rewrite("０３:５９", 3))
        assertEquals("\u200e24:00", rewrite("\u200e00:00", 0))
    }

    @Test
    fun leavesUnrelatedAndAlreadyConvertedTextAlone() {
        for (text in listOf("2026-10-22", "10月22日", "上午", "凌晨 01:30", "27:59", "24:00", "12:30", "", "0130")) {
            assertNull(text, NightTime.replacement(text, 1))
        }
        assertNull(NightTime.replacement("00:30", 2))
        assertEquals("25:30", rewrite(rewrite("01:30", 1), 1))
    }
}
