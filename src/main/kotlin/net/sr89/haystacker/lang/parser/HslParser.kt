package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.HslAndClause
import net.sr89.haystacker.lang.ast.HslClause
import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.HslOrClause
import net.sr89.haystacker.lang.ast.HslQuery
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol
import org.jparsec.OperatorTable
import org.jparsec.Parser
import org.jparsec.Parsers
import org.jparsec.Scanners
import org.jparsec.Scanners.isChar
import org.jparsec.Scanners.stringCaseInsensitive
import org.jparsec.Terminals
import java.util.function.BiFunction

class HslParser {
    private val symbolParser: Parser<Symbol> =
        stringCaseInsensitive("name").map { Symbol.NAME }
            .or(stringCaseInsensitive("last_modified").map { Symbol.LAST_MODIFIED })
            .or(stringCaseInsensitive("size").map { Symbol.SIZE })
            .or(stringCaseInsensitive("created").map { Symbol.CREATED })

    private val operatorParser: Parser<Operator> =
        isChar('<').followedBy(isChar('=')).map { Operator.LESS_OR_EQUAL }
            .or(isChar('>').followedBy(isChar('=')).map { Operator.GREATER_OR_EQUAL })
            .or(isChar('=').map { Operator.EQUALS })
            .or(isChar('>').map { Operator.GREATER })
            .or(isChar('<').map { Operator.LESS })

    private val dataSizeParser: Parser<String> = Parsers.sequence(
        Terminals.IntegerLiteral.TOKENIZER,
        stringCaseInsensitive("kb").map { "KB" }
            .or(stringCaseInsensitive("mb").map { "MB" })
            .or(stringCaseInsensitive("gb").map { "GB" })
            .or(isChar('b').map { "" })
            .asOptional()
            .map { o -> o.orElse("") }
    ) { a, b -> a.text() + b }

    private val valueParser: Parser<String> =
        Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER.or(dataSizeParser)

    private val whitespaces: Parser<Void> = Scanners.WHITESPACES.skipMany()

    private val nodeClauseParser: Parser<HslNodeClause> = Parsers.sequence(
        symbolParser.followedBy(whitespaces),
        operatorParser.followedBy(whitespaces),
        valueParser.followedBy(whitespaces),
        ::buildHslNodeClause)

    private fun parser(): Parser<HslClause> {
        val ref = Parser.newReference<HslClause>()
        val term = ref.lazy().between(isChar('('), isChar(')')).or(nodeClauseParser)

        val parser = OperatorTable<HslClause>()
            .infixl(stringCaseInsensitive("AND").followedBy(whitespaces).retn(BiFunction(::HslAndClause)), 20)
            .infixl(stringCaseInsensitive("OR").followedBy(whitespaces).retn(BiFunction(::HslOrClause)), 10)
            .build(term)

        ref.set(parser)
        return parser
    }

    fun parse(queryString: String): HslQuery {
        return parser().parse(queryString)
    }
}