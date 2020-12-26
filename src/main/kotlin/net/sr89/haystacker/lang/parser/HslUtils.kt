package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol

fun buildHslNodeClause(symbol: Symbol, operator: Operator, value: Any): HslNodeClause {
    val parsedValue: Any = when (symbol) {
        Symbol.CREATED -> parseHslDateTime(value)
        Symbol.LAST_MODIFIED -> parseHslDateTime(value)
        Symbol.SIZE -> value
        Symbol.NAME -> value
    }

    return HslNodeClause(symbol, operator, parsedValue)
}