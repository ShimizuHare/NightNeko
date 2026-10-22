package com.position.nightneko

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class NightDateTest {
    private fun check(actual: String, expected: String, zone: String = "Asia/Shanghai") {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm EEE", Locale.US).apply {
            timeZone = TimeZone.getTimeZone(zone)
            isLenient = false
        }
        val source = formatter.parse(actual)!!
        val original = source.time
        val result = NightTime.displayDate(source, formatter.timeZone)
        assertEquals(expected, formatter.format(result))
        assertEquals("Input date must remain unchanged", original, source.time)
    }

    @Test fun midnightAndFourOClockRestore() {
        check("2026-10-22 00:00 Thu", "2026-10-21 00:00 Wed")
        check("2026-10-22 03:59 Thu", "2026-10-21 03:59 Wed")
        check("2026-10-22 04:00 Thu", "2026-10-22 04:00 Thu")
        check("2026-10-22 23:59 Thu", "2026-10-22 23:59 Thu")
    }

    @Test fun monthYearLeapDayAndWeekBoundary() {
        check("2026-01-01 01:00 Thu", "2025-12-31 01:00 Wed")
        check("2024-03-01 01:00 Fri", "2024-02-29 01:00 Thu")
        check("2025-03-01 01:00 Sat", "2025-02-28 01:00 Fri")
        check("2026-10-19 01:00 Mon", "2026-10-18 01:00 Sun")
    }

    @Test fun daylightSavingUsesCalendarDay() {
        check("2026-03-09 00:30 Mon", "2026-03-08 00:30 Sun", "America/New_York")
        check("2026-11-02 00:30 Mon", "2026-11-01 00:30 Sun", "America/New_York")
    }

    @Test fun usesProvidedTimeZone() {
        val actual = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse("2026-10-21 18:00")!!
        val formatter = SimpleDateFormat("yyyy-MM-dd EEE", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Tokyo")
        }
        assertEquals("2026-10-21 Wed", formatter.format(NightTime.displayDate(actual, formatter.timeZone)))
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        assertEquals("2026-10-21 Wed", formatter.format(NightTime.displayDate(actual, formatter.timeZone)))
    }
}
