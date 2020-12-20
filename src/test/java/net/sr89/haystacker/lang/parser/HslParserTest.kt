package net.sr89.haystacker.lang.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HslParserTest {

    val parser = HslParser()

    @Test
    fun baseQuery() {
        val query = parser.parse("name = \"file.txt\"")
    }
}