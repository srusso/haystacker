package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.HslQuery
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol

class HslParser {
    fun parse(queryString: String): HslQuery {
        return HslNodeClause(Symbol.NAME, Operator.EQUALS, "")
    }
}