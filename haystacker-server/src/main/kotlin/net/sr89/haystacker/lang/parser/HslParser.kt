package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.HslAndClause
import net.sr89.haystacker.lang.ast.HslClause
import net.sr89.haystacker.lang.ast.HslDataSize
import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.HslOrClause
import net.sr89.haystacker.lang.ast.HslQuery
import net.sr89.haystacker.lang.ast.HslString
import net.sr89.haystacker.lang.ast.HslValue
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol
import net.sr89.haystacker.lang.exception.HslParseException
import net.sr89.haystacker.lang.exception.InvalidHslGrammarException
import org.jparsec.OperatorTable
import org.jparsec.Parser
import org.jparsec.Parsers
import org.jparsec.Scanners
import org.jparsec.Scanners.isChar
import org.jparsec.Scanners.string
import org.jparsec.Scanners.stringCaseInsensitive
import org.jparsec.Terminals
import org.jparsec.Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER
import org.jparsec.Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER
import org.jparsec.error.ParserException
import org.jparsec.pattern.CharPredicates.IS_ALPHA_NUMERIC
import org.jparsec.pattern.Patterns
import org.springframework.util.unit.DataSize
import java.util.function.BiFunction

class HslParser {
    private val stringPattern =
        Patterns.isChar(IS_ALPHA_NUMERIC)
            .or(Patterns.isChar('.'))
            .or(Patterns.among("-_"))
            .many()

    private val filenameScanner: Parser<HslValue> = DOUBLE_QUOTE_TOKENIZER
        .or(stringPattern.toScanner("filename").source())
        .map(::HslString)

    private val symbolParser: Parser<Symbol> =
        stringCaseInsensitive("name").map { Symbol.NAME }
            .or(stringCaseInsensitive("last_modified").map { Symbol.LAST_MODIFIED })
            .or(stringCaseInsensitive("size").map { Symbol.SIZE })
            .or(stringCaseInsensitive("created").map { Symbol.CREATED })

    private val operatorParser: Parser<Operator> =
        string("<=").map { Operator.LESS_OR_EQUAL }
            .or(string(">=").map { Operator.GREATER_OR_EQUAL })
            .or(isChar('=').map { Operator.EQUALS })
            .or(isChar('>').map { Operator.GREATER })
            .or(isChar('<').map { Operator.LESS })

    private val dataSizeParser: Parser<HslValue> = Parsers.sequence(
        Terminals.IntegerLiteral.TOKENIZER,
        listOf("tb", "gb", "mb", "kb", "b")
            .map(::stringCaseInsensitive)
            .reduce(Parser<Void>::or)
            .source()
            .asOptional()
            .map { o -> o.orElse("b") }
    ) { a, b -> a.text() + b }
        .map(String::toUpperCase)
        .map(DataSize::parse)
        .map(::HslDataSize)

    private val dateParser: Parser<HslValue> = SINGLE_QUOTE_TOKENIZER
        .map(::parseHslDateTime)

    private val valueParser: Parser<HslValue> = dataSizeParser
        .or(dateParser)
        .or(filenameScanner)

    private val whitespaces: Parser<Void> = Scanners.WHITESPACES.skipMany()

    private val nodeClauseParser: Parser<HslNodeClause> = Parsers.sequence(
        symbolParser.followedBy(whitespaces),
        operatorParser.followedBy(whitespaces),
        valueParser.followedBy(whitespaces),
        ::HslNodeClause
    )

    private fun parser(): Parser<HslClause> {
        val ref = Parser.newReference<HslClause>()
        val term = ref.lazy().between(isChar('(').followedBy(whitespaces), isChar(')')).or(nodeClauseParser)

        val parser = OperatorTable<HslClause>()
            .infixl(stringCaseInsensitive("AND").followedBy(whitespaces).retn(BiFunction(::HslAndClause)), 20)
            .infixl(stringCaseInsensitive("OR").followedBy(whitespaces).retn(BiFunction(::HslOrClause)), 10)
            .build(term)

        ref.set(parser)
        return parser
    }

    fun parse(queryString: String): HslQuery {
        try {
            return parser().parse(queryString.trim())
        } catch (e: ParserException) {
            val cause = e.cause
            if (cause is HslParseException) {
                throw cause
            } else {
                throw InvalidHslGrammarException(queryString, e.line, e.column)
            }
        }
    }
}