package net.sr89.haystacker.lang.ast

import org.springframework.util.unit.DataSize
import java.time.Instant
import java.time.LocalDate

sealed class HslValue

data class HslString(val str: String): HslValue()

data class HslDataSize(val size: DataSize): HslValue()

open class HslDateTime: HslValue()

data class HslDate(val date: LocalDate): HslDateTime()

data class HslInstant(val instant: Instant): HslDateTime()