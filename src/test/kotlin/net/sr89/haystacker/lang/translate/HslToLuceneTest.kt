package net.sr89.haystacker.lang.translate

import net.sr89.haystacker.lang.parser.HslParser
import org.apache.lucene.document.LongPoint
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause.Occur.MUST
import org.apache.lucene.search.BooleanClause.Occur.SHOULD
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.TermQuery
import org.junit.jupiter.api.Test
import org.springframework.util.unit.DataSize
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

internal class HslToLuceneTest {
    private val date = "2020-01-17"

    val hslToLucene: HslToLucene = HslToLucene(HslParser())

    @Test
    internal fun parseSimpleStringTermQuery() {
        val query = hslToLucene.toLuceneQuery("name = \"file.txt\"")

        assertEquals(query, TermQuery(Term("name", "file.txt")))
    }

    @Test
    internal fun longQueries() {
        assertEquals(
            LongPoint.newRangeQuery("size", DataSize.ofKilobytes(23).toBytes(), Long.MAX_VALUE),
            hslToLucene.toLuceneQuery("size > 23kb")
        )

        assertEquals(
            LongPoint.newRangeQuery("size", Long.MIN_VALUE, DataSize.ofKilobytes(23).toBytes()),
            hslToLucene.toLuceneQuery("size < 23kb")
        )

        assertEquals(
            LongPoint.newExactQuery("size", DataSize.ofKilobytes(23).toBytes()),
            hslToLucene.toLuceneQuery("size = 23kb")
        )
    }

    @Test
    internal fun dateQuery() {
        assertEquals(
            LongPoint.newRangeQuery("last_modified", Long.MIN_VALUE, dateToMillis(date)),
            hslToLucene.toLuceneQuery("last_modified <= '$date'"))
    }

    @Test
    internal fun dateTimeQuery() {
        assertEquals(
            LongPoint.newRangeQuery("last_modified", Long.MIN_VALUE, dateToMillis(date)),
            hslToLucene.toLuceneQuery("last_modified <= '$date'"))
    }

    @Test
    internal fun andQuery() {
        val createdClause = LongPoint.newRangeQuery("created", Long.MIN_VALUE, dateToMillis(date))
        val nameClause = TermQuery(Term("name", "file.txt"))

        assertEquals(
            BooleanQuery.Builder().add(createdClause, MUST).add(nameClause, MUST).build(),
            hslToLucene.toLuceneQuery("created < '$date' AND name = \"file.txt\"")
        )
    }

    @Test
    internal fun orQuery() {
        val createdClause = LongPoint.newRangeQuery("created", Long.MIN_VALUE, dateToMillis(date))
        val nameClause = TermQuery(Term("name", "file.txt"))

        assertEquals(
            BooleanQuery.Builder().add(createdClause, SHOULD).add(nameClause, SHOULD).build(),
            hslToLucene.toLuceneQuery("created < '$date' OR name = \"file.txt\"")
        )
    }

    @Test
    internal fun nestedQueries() {
        val createdClause = LongPoint.newRangeQuery("created", Long.MIN_VALUE, dateToMillis(date))
        val lastModifiedClause = LongPoint.newRangeQuery("last_modified", dateToMillis(date), Long.MAX_VALUE)
        val nameClause = TermQuery(Term("name", "file.txt"))

        val innerClause = BooleanQuery.Builder().add(lastModifiedClause, SHOULD).add(nameClause, SHOULD).build()

        assertEquals(
            BooleanQuery.Builder().add(createdClause, MUST).add(innerClause, MUST).build(),
            hslToLucene.toLuceneQuery("created < '$date' AND (last_modified > '$date' OR name = \"file.txt\")")
        )
    }

    private fun dateToMillis(date: String) = LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}