package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.HslQuery
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol
import org.jparsec.Parser
import org.jparsec.Parsers
import org.jparsec.Scanners
import org.jparsec.Terminals

class HslParser {
    private val symbolParser: Parser<Symbol> =
        Scanners.stringCaseInsensitive("name").map { Symbol.NAME }

    private val operatorParser: Parser<Operator> =
        Scanners.isChar('<').map { Operator.LESS }
            .or(Scanners.isChar('=').map { Operator.EQUALS })

    private val valueParser: Parser<Any> = Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER
        .map { s -> s.toString() }

    private val nodeClauseParser: Parser<HslNodeClause> = Parsers.sequence(
        symbolParser.followedBy(Scanners.WHITESPACES.skipMany()),
        operatorParser.followedBy(Scanners.WHITESPACES.skipMany()),
        valueParser.followedBy(Scanners.WHITESPACES.skipMany()),
        ::HslNodeClause)

    fun parse(queryString: String): HslQuery {
        return nodeClauseParser.parse(queryString)
    }
}