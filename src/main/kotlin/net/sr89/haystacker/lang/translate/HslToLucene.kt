package net.sr89.haystacker.lang.translate

import net.sr89.haystacker.lang.parser.HslParser
import org.apache.lucene.search.Query

class HslToLucene (val hslParser: HslParser) {

    fun toLuceneQuery(hslQuery: String): Query {
        TODO("Implement me")
    }

}
