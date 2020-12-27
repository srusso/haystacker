package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.*
import org.jparsec.*
import org.jparsec.Scanners.isChar
import org.jparsec.Scanners.stringCaseInsensitive
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
        whitespaces,
        symbolParser.followedBy(whitespaces),
        operatorParser.followedBy(whitespaces),
        valueParser.followedBy(whitespaces)
    ) { _: Void?, symbol: Symbol, operator: Operator, value: String ->
        buildHslNodeClause(symbol, operator, value) }

    private fun parser(): Parser<HslClause> {
        val ref = Parser.newReference<HslClause>()
        val term = ref.lazy().between(isChar('('), isChar(')')).or(nodeClauseParser)

        val parser = OperatorTable<HslClause>()
            .infixl(stringCaseInsensitive("AND").retn(BiFunction(::HslAndClause)), 20)
            .infixl(stringCaseInsensitive("OR").retn(BiFunction(::HslOrClause)), 10)
            .build(term)

        ref.set(parser)
        return parser
    }

    fun parse(queryString: String): HslQuery {
        return parser().parse(queryString)
    }
}