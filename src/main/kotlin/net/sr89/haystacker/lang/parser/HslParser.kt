package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.HslQuery
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol
import org.jparsec.Parser
import org.jparsec.Parsers
import org.jparsec.Scanners
import org.jparsec.Scanners.isChar
import org.jparsec.Terminals
import org.jparsec.functors.Map3

class HslParser {
    private val symbolParser: Parser<Symbol> =
        Scanners.stringCaseInsensitive("name").map { Symbol.NAME }
            .or(Scanners.stringCaseInsensitive("last_modified").map { Symbol.LAST_MODIFIED })
            .or(Scanners.stringCaseInsensitive("size").map { Symbol.SIZE })
            .or(Scanners.stringCaseInsensitive("created").map { Symbol.CREATED })

    private val operatorParser: Parser<Operator> =
        isChar('<').followedBy(isChar('=')).map { Operator.LESS_OR_EQUAL }
            .or(isChar('>').followedBy(isChar('=')).map { Operator.GREATER_OR_EQUAL })
            .or(isChar('=').map { Operator.EQUALS })
            .or(isChar('>').map { Operator.GREATER })
            .or(isChar('<').map { Operator.LESS })

    private val valueParser: Parser<Any> = Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER
        .map { s -> s.toString() }

    private val nodeClauseParser: Parser<HslNodeClause> = Parsers.sequence(
        symbolParser.followedBy(Scanners.WHITESPACES.skipMany()),
        operatorParser.followedBy(Scanners.WHITESPACES.skipMany()),
        valueParser.followedBy(Scanners.WHITESPACES.skipMany()),
        ::buildHslNodeClause
    )

    fun parse(queryString: String): HslQuery {
        return nodeClauseParser.parse(queryString)
    }
}