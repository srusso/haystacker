package net.sr89.haystacker.lang.parser

import java.time.Instant
import java.time.LocalDate

open class HslDateTime

data class HslDate(val date: LocalDate): HslDateTime()

data class HslInstant(val instant: Instant): HslDateTime()