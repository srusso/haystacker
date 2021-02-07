package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.HslAndClause
import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.HslOrClause
import net.sr89.haystacker.lang.ast.HslValue
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol
import net.sr89.haystacker.lang.exception.InvalidHslGrammarException
import net.sr89.haystacker.test.common.having
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class HslParserTest {
    private val date = "2020-01-01"
    private val dateTime = "2020-01-17T10:15:50Z"
    private val dateTimeWithOffset = "2020-01-17T10:15:30+04:00"

    private val parser = HslParser()

    private fun <T> HslNodeClause.isNodeClause(symbol: Symbol, operator: Operator, value: T) {
        assertEquals(symbol, this.symbol)
        assertEquals(operator, this.operator)
        assertEquals(value, this.value)
    }

    @Test
    fun nameEqualToClauseWithSpacesInFilename() {
        val query = parser.parse("name = \"name with spaces.txt\"")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.NAME, Operator.EQUALS, HslValue("name with spaces.txt"))
            }
    }

    @Test
    fun nameEqualToClause() {
        val query = parser.parse("name = \"file.txt\"")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.NAME, Operator.EQUALS, HslValue("file.txt"))
            }
    }

    @Test
    fun nameEqualToClauseNoQuotes() {
        val query = parser.parse("name = file.txt")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.NAME, Operator.EQUALS, HslValue("file.txt"))
            }
    }

    @Test
    fun sizeAssumesBytesIfNoUnitSpecified() {
        val query = parser.parse("size > 23")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.SIZE, Operator.GREATER, HslValue("23"))
            }
    }

    @Test
    fun sizeGreaterThanBClause() {
        val query = parser.parse("size > 23b")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.SIZE, Operator.GREATER, HslValue("23b"))
            }
    }

    @Test
    fun sizeGreaterThanKBClause() {
        val query = parser.parse("size > 23kb")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.SIZE, Operator.GREATER, HslValue("23kb"))
            }
    }

    @Test
    fun lastModifiedGreaterThanDate() {
        val query = parser.parse("last_modified > '$date'")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.LAST_MODIFIED, Operator.GREATER, HslValue(date))
            }
    }

    @Test
    fun lastModifiedGreaterThanDateTime() {
        val query = parser.parse("last_modified > '$dateTime'")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.LAST_MODIFIED, Operator.GREATER, HslValue(dateTime))
            }
    }

    @Test
    fun lastModifiedGreaterThanDateTimeWithOffset() {
        val query = parser.parse("last_modified > '$dateTimeWithOffset'")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.LAST_MODIFIED, Operator.GREATER, HslValue(dateTimeWithOffset))
            }
    }

    @Test
    fun lastModifiedGreaterOrEqualThanDate() {
        val query = parser.parse("last_modified >= '$date'")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.LAST_MODIFIED, Operator.GREATER_OR_EQUAL, HslValue(date))
            }
    }

    @Test
    fun createdLessThanDate() {
        val query = parser.parse("created < '$date'")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.CREATED, Operator.LESS, HslValue(date))
            }
    }

    @Test
    fun andClause() {
        val query = parser.parse("created < '$date' AND name = \"file.txt\"")

        having(query)
            .ofType(HslAndClause::class)
            .then {
                having(it.left)
                    .ofType(HslNodeClause::class)
                    .then {left ->
                        left.isNodeClause(Symbol.CREATED, Operator.LESS, HslValue(date))
                    }

                having(it.right)
                    .ofType(HslNodeClause::class)
                    .then {right ->
                        right.isNodeClause(Symbol.NAME, Operator.EQUALS, HslValue("file.txt"))
                    }
            }
    }

    @Test
    fun orClause() {
        val query = parser.parse("created < '$date' OR name = \"file.txt\"")

        having(query)
            .ofType(HslOrClause::class)
            .then {
                having(it.left)
                    .ofType(HslNodeClause::class)
                    .then {left ->
                        left.isNodeClause(Symbol.CREATED, Operator.LESS, HslValue(date))
                    }

                having(it.right)
                    .ofType(HslNodeClause::class)
                    .then {right ->
                        right.isNodeClause(Symbol.NAME, Operator.EQUALS, HslValue("file.txt"))
                    }
            }
    }

    @Test
    fun andHasPrecedenceOverOr() {
        val query = parser.parse("created < '$date' AND created < '$date' OR name = \"file.txt\"")

        having(query)
            .ofType(HslOrClause::class)
            .then {
                having(it.left)
                    .ofType(HslAndClause::class)
                    .then {
                    }

                having(it.right)
                    .ofType(HslNodeClause::class)
                    .then {right ->
                        right.isNodeClause(Symbol.NAME, Operator.EQUALS, HslValue("file.txt"))
                    }
            }
    }

    @Test
    fun parensForcePrecedence() {
        val query = parser.parse("created < '$date' AND (created < '$date' OR name = \"file.txt\")")

        having(query)
            .ofType(HslAndClause::class)
            .then {
                having(it.left)
                    .ofType(HslNodeClause::class)
                    .then {left ->
                        left.isNodeClause(Symbol.CREATED, Operator.LESS, HslValue(date))
                    }

                having(it.right)
                    .ofType(HslOrClause::class)
                    .then {
                    }
            }
    }

    @Test
    fun spacesAreIgnored() {
        val query = parser.parse("  created  <   '$date'     AND    (  created   <   '$date'   OR  name   =   \"file.txt\"  ) ")

        having(query)
            .ofType(HslAndClause::class)
            .then {
                having(it.left)
                    .ofType(HslNodeClause::class)
                    .then {left ->
                        left.isNodeClause(Symbol.CREATED, Operator.LESS, HslValue(date))
                    }

                having(it.right)
                    .ofType(HslOrClause::class)
                    .then {
                    }
            }
    }

    @Test
    internal fun brokenGrammarThrowsSensibleException() {
        try {
            parser.parse("this is a broken query")
            fail<String>("Expected parsing to fail for a broken HSL query")
        } catch (e: InvalidHslGrammarException) {

        }
    }
}