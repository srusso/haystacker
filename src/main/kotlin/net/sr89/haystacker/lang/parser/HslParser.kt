package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.HslQuery

class HslParser {
    fun parse(queryString: String): HslQuery {
        return HslNodeClause()
    }
}