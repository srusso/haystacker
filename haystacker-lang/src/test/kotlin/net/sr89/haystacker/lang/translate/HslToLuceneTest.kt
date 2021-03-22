package net.sr89.haystacker.lang.translate

import net.sr89.haystacker.lang.ast.Symbol
import net.sr89.haystacker.lang.ast.Symbol.CREATED
import net.sr89.haystacker.lang.ast.Symbol.LAST_MODIFIED
import net.sr89.haystacker.lang.ast.Symbol.SIZE
import net.sr89.haystacker.lang.parser.HslParser
import net.sr89.haystacker.lang.translate.visit.ToLuceneClauseVisitor
import org.apache.lucene.document.LongPoint
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause.Occur.MUST
import org.apache.lucene.search.BooleanClause.Occur.SHOULD
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.junit.jupiter.api.Test
import org.springframework.util.unit.DataSize
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

internal class HslToLuceneTest {
    private val date = "2020-01-17"
    private val dateTime = "2020-01-17T10:15:30Z"

    val hslParser: HslParser = HslParser()

    private fun toLuceneQuery(hsl: String): Query {
        return hslParser.parse(hsl).clause.accept(ToLuceneClauseVisitor())
    }

    @Test
    internal fun parseSimpleStringTermQuery() {
        val query = toLuceneQuery("name = \"file.txt\"")

        assertEquals(TermQuery(Term(Symbol.NAME.luceneQueryName, "file.txt")), query)
    }

    @Test
    internal fun longQueries() {
        assertEquals(
            LongPoint.newRangeQuery(SIZE.luceneQueryName, DataSize.ofKilobytes(23).toBytes() + 1, Long.MAX_VALUE),
            toLuceneQuery("size > 23kb")
        )

        assertEquals(
            LongPoint.newRangeQuery(SIZE.luceneQueryName, DataSize.ofKilobytes(23).toBytes(), Long.MAX_VALUE),
            toLuceneQuery("size >= 23kb")
        )

        assertEquals(
            LongPoint.newRangeQuery(SIZE.luceneQueryName, Long.MIN_VALUE, DataSize.ofKilobytes(23).toBytes() - 1),
            toLuceneQuery("size < 23kb")
        )

        assertEquals(
            LongPoint.newRangeQuery(SIZE.luceneQueryName, Long.MIN_VALUE, DataSize.ofKilobytes(23).toBytes()),
            toLuceneQuery("size <= 23kb")
        )

        assertEquals(
            LongPoint.newExactQuery(SIZE.luceneQueryName, DataSize.ofKilobytes(23).toBytes()),
            toLuceneQuery("size = 23kb")
        )
    }

    @Test
    internal fun dateQuery() {
        assertEquals(
            LongPoint.newRangeQuery(LAST_MODIFIED.luceneQueryName, Long.MIN_VALUE, dateToMillis(date)),
            toLuceneQuery("last_modified <= '$date'"))
    }

    @Test
    internal fun dateTimeQuery() {
        assertEquals(
            LongPoint.newRangeQuery(LAST_MODIFIED.luceneQueryName, Long.MIN_VALUE, 1579256130000),
            toLuceneQuery("last_modified <= '$dateTime'"))
    }

    @Test
    internal fun andQuery() {
        val createdClause = LongPoint.newRangeQuery(CREATED.luceneQueryName, Long.MIN_VALUE, dateToMillis(date))
        val nameClause = TermQuery(Term("path", "file.txt"))

        assertEquals(
            BooleanQuery.Builder().add(createdClause, MUST).add(nameClause, MUST).build(),
            toLuceneQuery("created <= '$date' AND name = \"file.txt\"")
        )
    }

    @Test
    internal fun orQuery() {
        val createdClause = LongPoint.newRangeQuery(CREATED.luceneQueryName, Long.MIN_VALUE, dateToMillis(date))
        val nameClause = TermQuery(Term("path", "file.txt"))

        assertEquals(
            BooleanQuery.Builder().add(createdClause, SHOULD).add(nameClause, SHOULD).build(),
            toLuceneQuery("created <= '$date' OR name = \"file.txt\"")
        )
    }

    @Test
    internal fun nestedQueries() {
        val createdClause = LongPoint.newRangeQuery(CREATED.luceneQueryName, Long.MIN_VALUE, dateToMillis(date))
        val lastModifiedClause = LongPoint.newRangeQuery(LAST_MODIFIED.luceneQueryName, dateToMillis(date), Long.MAX_VALUE)
        val nameClause = TermQuery(Term("path", "file.txt"))

        val innerClause = BooleanQuery.Builder().add(lastModifiedClause, SHOULD).add(nameClause, SHOULD).build()

        assertEquals(
            BooleanQuery.Builder().add(createdClause, MUST).add(innerClause, MUST).build(),
            toLuceneQuery("created <= '$date' AND (last_modified >= '$date' OR name = \"file.txt\")")
        )
    }

    private fun dateToMillis(date: String) = LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}