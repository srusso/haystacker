package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.HslDate
import net.sr89.haystacker.lang.ast.HslDateTime
import net.sr89.haystacker.lang.ast.HslInstant
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField

private fun isDateTime(date: Any): HslInstant? {
    val parsers = listOf(
        DateTimeFormatter.ISO_INSTANT,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME
    )

    for (parser in parsers) {
        try {
            val temporalAccessor = parser.parse(date.toString())
            val seconds = temporalAccessor.get(ChronoField.INSTANT_SECONDS)
            val nanos = temporalAccessor.get(ChronoField.NANO_OF_SECOND)
            return HslInstant(Instant.ofEpochSecond(seconds.toLong(), nanos.toLong()))
        } catch (e: DateTimeParseException) {

        }
    }

    return null
}

private fun isDate(date: Any): HslDate? {
    return try {
        HslDate(LocalDate.parse(date.toString()))
    } catch (e: DateTimeParseException) {
        null
    }
}

fun parseHslDateTime(date: Any): HslDateTime {
    val dateTimeValue = isDateTime(date)

    if (dateTimeValue != null) {
        return dateTimeValue
    }

    val dateValue = isDate(date)

    if (dateValue != null) {
        return dateValue
    }

    throw hslParseException("Value '$date' is neither a \"UTC date time\" (ex: 2011-12-03T10:15:30Z), a \"date time with offset\" (ex: 2011-12-03T10:15:30+01:00) nor a date (ex: 2011-12-03)")
}