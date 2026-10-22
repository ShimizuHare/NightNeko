package com.position.nightneko

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/** Display-only arithmetic. Never mutates a clock, calendar, date or time zone. */
object NightTime {
    /** Returns a new display date, using calendar days rather than a fixed 24-hour offset. */
    fun displayDate(actual: Date, timeZone: TimeZone): Date {
        val calendar = Calendar.getInstance(timeZone).apply { time = actual }
        if (calendar.get(Calendar.HOUR_OF_DAY) < 4) calendar.add(Calendar.DAY_OF_MONTH, -1)
        return calendar.time
    }

    fun displayHour(hour: Int): Int {
        require(hour in 0..23)
        return if (hour < 4) hour + 24 else hour
    }

    data class HourReplacement(val start: Int, val end: Int, val text: String)

    private val leadingHour = Regex("^[\\s\\u200e\\u200f\\u061c]*([\\p{Nd}]{1,2})(?=[:：.∶]|$)")

    /** Fail closed for dates, AM/PM labels and unrecognised clock formats. */
    fun replacement(text: CharSequence, actualHour: Int): HourReplacement? {
        if (actualHour !in 0..3) return null
        val group = leadingHour.find(text)?.groups?.get(1) ?: return null
        val digits = group.value
        val value = digits.fold(0) { total, char -> total * 10 + Character.digit(char, 10) }
        if (value != actualHour) return null
        val zero = digits.first().code - Character.digit(digits.first(), 10)
        val newDigits = displayHour(actualHour).toString().map { (zero + (it - '0')).toChar() }.joinToString("")
        return HourReplacement(group.range.first, group.range.last + 1, newDigits)
    }
}
