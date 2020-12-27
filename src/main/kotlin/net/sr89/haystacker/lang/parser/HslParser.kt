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
import org.jparsec.Scanners.string
import org.jparsec.Scanners.stringCaseInsensitive
import org.jparsec.Terminals
import org.jparsec.Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER
import org.jparsec.pattern.CharPredicates.IS_ALPHA
import org.jparsec.pattern.CharPredicates.IS_ALPHA_NUMERIC
import org.jparsec.pattern.Patterns
import java.util.function.BiFunction

class HslParser {

    // starts with either an alphabetic character or a dit (think .gitignore)
    private val filenamePattern = (Patterns.isChar(IS_ALPHA)
        .or(Patterns.isChar('.')))
        .next(
            // and continues with either alphanumeric characters or dots or other selected special characters
            (Patterns.isChar(IS_ALPHA_NUMERIC)
                .or(Patterns.isChar('.')))
                .or(Patterns.among("-_"))
                .many()
        )

    private val filenameScanner: Parser<String> = DOUBLE_QUOTE_TOKENIZER
        .or(filenamePattern.toScanner("filename").source())

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
        filenameScanner
            .or(dataSizeParser)

    private val whitespaces: Parser<Void> = Scanners.WHITESPACES.skipMany()

    private val nodeClauseParser: Parser<HslNodeClause> = Parsers.sequence(
        symbolParser.followedBy(whitespaces),
        operatorParser.followedBy(whitespaces),
        valueParser.followedBy(whitespaces),
        ::buildHslNodeClause
    )

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