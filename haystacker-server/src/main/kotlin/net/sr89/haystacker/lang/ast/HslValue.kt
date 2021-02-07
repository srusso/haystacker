package net.sr89.haystacker.lang.ast

import java.time.Instant
import java.time.LocalDate

data class HslValue(val str: String)


sealed class HslDateTime

data class HslDate(val date: LocalDate): HslDateTime()

data class HslInstant(val instant: Instant): HslDateTime()