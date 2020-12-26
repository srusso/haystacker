package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.*
import org.springframework.util.unit.DataSize

fun buildHslNodeClause(symbol: Symbol, operator: Operator, value: Any): HslNodeClause {
    val parsedValue: HslValue = when (symbol) {
        Symbol.CREATED -> parseHslDateTime(value)
        Symbol.LAST_MODIFIED -> parseHslDateTime(value)
        Symbol.SIZE -> HslDataSize(DataSize.parse(value.toString()))
        Symbol.NAME -> HslString(value.toString())
    }

    return HslNodeClause(symbol, operator, parsedValue)
}