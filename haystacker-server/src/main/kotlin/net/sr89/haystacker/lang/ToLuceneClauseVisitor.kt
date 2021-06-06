package net.sr89.haystacker.lang

import net.sr89.haystacker.lang.ast.HslAndClause
import net.sr89.haystacker.lang.ast.HslClauseVisitor
import net.sr89.haystacker.lang.ast.HslDate
import net.sr89.haystacker.lang.ast.HslInstant
import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.HslOrClause
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol
import net.sr89.haystacker.lang.exception.InvalidHslDataSizeException
import net.sr89.haystacker.lang.exception.InvalidHslOperatorException
import net.sr89.haystacker.lang.parser.parseHslDateTime
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.LongPoint
import org.apache.lucene.index.Term
import org.apache.lucene.search.PrefixQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.springframework.util.unit.DataSize
import java.time.ZoneOffset

private fun normalizeValueToLuceneRepresentation(clause: HslNodeClause, analyzer: Analyzer): String {
    return analyzer.normalize(clause.symbol.luceneQueryName, clause.value.str).utf8ToString()
}

private fun normalizeValueToLuceneRepresentation(
    clauseSymbol: Symbol,
    queryPart: String,
    analyzer: Analyzer
): String {
    return analyzer.normalize(clauseSymbol.luceneQueryName, queryPart).utf8ToString()
}

private fun longQuery(operator: Operator, fieldName: String, bytes: Long): Query {
    return when (operator) {
        Operator.EQUALS -> LongPoint.newExactQuery(fieldName, bytes)
        Operator.GREATER_OR_EQUAL -> LongPoint.newRangeQuery(fieldName, bytes, Long.MAX_VALUE)
        Operator.LESS_OR_EQUAL -> LongPoint.newRangeQuery(fieldName, Long.MIN_VALUE, bytes)
        Operator.GREATER -> LongPoint.newRangeQuery(fieldName, bytes + 1, Long.MAX_VALUE)
        Operator.LESS -> LongPoint.newRangeQuery(fieldName, Long.MIN_VALUE, bytes - 1)
    }
}

private fun toFileNameQuery(clause: HslNodeClause, analyzer: Analyzer): Query {
    return when (clause.operator) {
        Operator.EQUALS -> {

            if (!clause.value.str.contains(' ')) {
                val term = Term(clause.symbol.luceneQueryName, normalizeValueToLuceneRepresentation(clause, analyzer))
                TermQuery(term).or(PrefixQuery(term))
            } else {
                clause.value.str.split(" ")
                    .map {
                        val term = Term(
                            clause.symbol.luceneQueryName,
                            normalizeValueToLuceneRepresentation(clause.symbol, it, analyzer)
                        )
                        TermQuery(term).or(PrefixQuery(term))
                    }.reduceRight(Query::and)
            }
        }
        else -> throw InvalidHslOperatorException(clause.symbol, clause.operator, clause.value.str)
    }
}

private fun toDataSizeQuery(clause: HslNodeClause): Query {
    return try {
        val dataSize = DataSize.parse(clause.value.str.toUpperCase())
        longQuery(clause.operator, clause.symbol.luceneQueryName, dataSize.toBytes())
    } catch (e: IllegalArgumentException) {
        throw InvalidHslDataSizeException(clause.symbol, clause.value.str)
    }
}

private fun toDateQuery(clause: HslNodeClause): Query {
    return when (val dateTime = parseHslDateTime(clause.symbol, clause.value.str)) {
        is HslDate -> longQuery(
            clause.operator,
            clause.symbol.luceneQueryName,
            dateTime.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        is HslInstant -> longQuery(clause.operator, clause.symbol.luceneQueryName, dateTime.instant.toEpochMilli())
    }
}

private fun HslNodeClause.toLuceneTerm(analyzer: Analyzer): Query {
    return when (symbol) {
        Symbol.NAME -> toFileNameQuery(this, analyzer)
        Symbol.CREATED, Symbol.LAST_MODIFIED -> toDateQuery(this)
        Symbol.SIZE -> toDataSizeQuery(this)
    }
}

class ToLuceneClauseVisitor(val analyzer: Analyzer) : HslClauseVisitor<Query> {
    override fun visit(query: HslAndClause): Query {
        val left = query.left.accept(this)
        val right = query.right.accept(this)

        return left.and(right)
    }

    override fun visit(query: HslOrClause): Query {
        val left = query.left.accept(this)
        val right = query.right.accept(this)

        return left.or(right)
    }

    override fun visit(query: HslNodeClause): Query {
        return query.toLuceneTerm(analyzer)
    }
}