package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.HslAndClause
import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.HslOrClause
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol
import net.sr89.haystacker.test.common.having
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.util.unit.DataSize
import java.time.LocalDate

class HslParserTest {
    private val date = "2020-01-01"

    private val parser = HslParser()

    private fun <T> HslNodeClause<*>.isNodeClause(symbol: Symbol, operator: Operator, value: T) {
        assertEquals(symbol, this.symbol)
        assertEquals(operator, this.operator)
        assertEquals(value, this.value)
    }

    @Test
    fun nameEqualToClause() {
        val query = parser.parse("name = \"file.txt\"")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.NAME, Operator.EQUALS, "file.txt")
            }
    }

    @Test
    fun sizeGreaterThanBClause() {
        val query = parser.parse("size > 23")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.SIZE, Operator.GREATER, DataSize.ofBytes(23))
            }
    }

    @Test
    fun sizeGreaterThanKBClause() {
        val query = parser.parse("size > 23kb")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.SIZE, Operator.GREATER, DataSize.ofKilobytes(23))
            }
    }

    @Test
    fun sizeGreaterThanMBClause() {
        val query = parser.parse("size > 23mb")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.SIZE, Operator.GREATER, DataSize.ofMegabytes(23))
            }
    }

    @Test
    fun sizeGreaterThanGBClause() {
        val query = parser.parse("size > 23gb")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.SIZE, Operator.GREATER, DataSize.ofGigabytes(23))
            }
    }

    @Test
    fun lastModifiedGreaterThanDate() {
        val query = parser.parse("last_modified > \"$date\"")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.LAST_MODIFIED, Operator.GREATER, LocalDate.parse(date))
            }
    }

    @Test
    fun lastModifiedGreaterOrEqualThanDate() {
        val query = parser.parse("last_modified >= \"$date\"")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.LAST_MODIFIED, Operator.GREATER_OR_EQUAL, LocalDate.parse(date))
            }
    }

    @Test
    fun lastModifiedLessOrEqualThanDate() {
        val query = parser.parse("last_modified <= \"$date\"")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.LAST_MODIFIED, Operator.LESS_OR_EQUAL, LocalDate.parse(date))
            }
    }

    @Test
    fun createdLessThanDate() {
        val query = parser.parse("created < \"$date\"")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.CREATED, Operator.LESS, LocalDate.parse(date))
            }
    }

    @Test
    fun andClause() {
        val query = parser.parse("created < \"$date\" AND name = \"file.txt\"")

        having(query)
            .ofType(HslAndClause::class)
            .then {
                having(it.left)
                    .ofType(HslNodeClause::class)
                    .then {left ->
                        left.isNodeClause(Symbol.CREATED, Operator.LESS, LocalDate.parse(date))
                    }

                having(it.right)
                    .ofType(HslNodeClause::class)
                    .then {right ->
                        right.isNodeClause(Symbol.NAME, Operator.EQUALS, "\"file.txt\"")
                    }
            }
    }

    @Test
    fun orClause() {
        val query = parser.parse("created < \"$date\" OR name = \"file.txt\"")

        having(query)
            .ofType(HslOrClause::class)
            .then {
                having(it.left)
                    .ofType(HslNodeClause::class)
                    .then {left ->
                        left.isNodeClause(Symbol.CREATED, Operator.LESS, LocalDate.parse(date))
                    }

                having(it.right)
                    .ofType(HslNodeClause::class)
                    .then {right ->
                        right.isNodeClause(Symbol.NAME, Operator.EQUALS, "\"file.txt\"")
                    }
            }
    }

    @Test
    fun andHasPrecedenceOverOr() {
        val query = parser.parse("created < \"$date\" AND created < \"$date\" OR name = \"file.txt\"")

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
                        right.isNodeClause(Symbol.NAME, Operator.EQUALS, "\"file.txt\"")
                    }
            }
    }

    @Test
    fun parensForcePrecedence() {
        val query = parser.parse("created < \"$date\" AND (created < \"$date\" OR name = \"file.txt\")")

        having(query)
            .ofType(HslAndClause::class)
            .then {
                having(it.left)
                    .ofType(HslNodeClause::class)
                    .then {left ->
                        left.isNodeClause(Symbol.CREATED, Operator.LESS, LocalDate.parse(date))
                    }

                having(it.right)
                    .ofType(HslOrClause::class)
                    .then {
                    }
            }
    }
}