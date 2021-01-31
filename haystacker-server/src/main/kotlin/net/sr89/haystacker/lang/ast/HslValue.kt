package net.sr89.haystacker.lang.ast

import org.springframework.util.unit.DataSize
import java.time.Instant
import java.time.LocalDate

interface HslValueVisitor<T> {
    fun accept(value: HslString): T
    fun accept(value: HslDataSize): T
    fun accept(value: HslDate): T
    fun accept(value: HslInstant): T
}

sealed class HslValue {
    abstract fun <T>accept(visitor: HslValueVisitor<T>): T
}

data class HslString(val str: String): HslValue() {
    override fun <T> accept(visitor: HslValueVisitor<T>): T {
        return visitor.accept(this)
    }
}

data class HslDataSize(val size: DataSize): HslValue() {
    override fun <T> accept(visitor: HslValueVisitor<T>): T {
        return visitor.accept(this)
    }
}

sealed class HslDateTime: HslValue()

data class HslDate(val date: LocalDate): HslDateTime() {
    override fun <T> accept(visitor: HslValueVisitor<T>): T {
        return visitor.accept(this)
    }
}

data class HslInstant(val instant: Instant): HslDateTime() {
    override fun <T> accept(visitor: HslValueVisitor<T>): T {
        return visitor.accept(this)
    }
}