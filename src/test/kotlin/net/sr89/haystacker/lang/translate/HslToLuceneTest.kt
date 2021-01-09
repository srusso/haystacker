package net.sr89.haystacker.lang.translate

import net.sr89.haystacker.lang.parser.HslParser
import org.junit.jupiter.api.Test

internal class HslToLuceneTest {
    val hslToLucene: HslToLucene = HslToLucene(HslParser())

    @Test
    internal fun parseSimpleQuery() {
        val query = hslToLucene.toLuceneQuery("name = \"file.txt\"")

        TODO("Implement asserts and more tests")
    }
}