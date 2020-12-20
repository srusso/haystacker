package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class HslParserTest {

    private val parser = HslParser()

    @Test
    fun baseQuery() {
        val query = parser.parse("name = \"file.txt\"")

        if (query is HslNodeClause) {
            assertEquals(Symbol.NAME, query.symbol)
            assertEquals(Operator.EQUALS, query.operator)
            assertEquals("file.txt", query.value)
        } else {
            fail("Query was expected to be HslNodeClause")
        }
    }
}