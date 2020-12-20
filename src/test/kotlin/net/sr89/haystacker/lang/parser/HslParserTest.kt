package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol
import net.sr89.haystacker.test.common.having
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.springframework.util.unit.DataSize

class HslParserTest {

    private val parser = HslParser()

    @Test
    fun nameEqualToClause() {
        val query = parser.parse("name = \"file.txt\"")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                assertEquals(Symbol.NAME, it.symbol)
                assertEquals(Operator.EQUALS, it.operator)
                assertEquals("file.txt", it.value)
            }
    }

    @Test
    fun sizeGreaterClause() {
        val query = parser.parse("size > 23mb")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                assertEquals(Symbol.SIZE, it.symbol)
                assertEquals(Operator.GREATER, it.operator)
                assertEquals(DataSize.ofMegabytes(23), it.value)
            }
    }

    @Test
    fun dateGreaterClause() {
        val query = parser.parse("last_modified > \"2020-01-01\"")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                fail("Implement me")
            }
    }

    @Test
    fun dateTimeGreaterClause() {
        val query = parser.parse("last_modified > \"2020-01-01 10:10:50\"")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                fail("Implement me")
            }
    }
}