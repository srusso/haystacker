package net.sr89.haystacker.lang.translate.visit

import net.sr89.haystacker.lang.ast.HslAndClause
import net.sr89.haystacker.lang.ast.HslDate
import net.sr89.haystacker.lang.ast.HslInstant
import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.HslOrClause
import net.sr89.haystacker.lang.ast.HslQueryVisitor
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol
import net.sr89.haystacker.lang.exception.InvalidSemanticException
import net.sr89.haystacker.lang.parser.parseHslDateTime
import net.sr89.haystacker.lang.translate.and
import net.sr89.haystacker.lang.translate.or
import org.apache.lucene.document.LongPoint
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.springframework.util.unit.DataSize
import java.time.ZoneOffset

private fun longQuery(operator: Operator, fieldName: String, bytes: Long): Query {
    return when (operator) {
        Operator.EQUALS -> LongPoint.newExactQuery(fieldName, bytes)
        Operator.GREATER_OR_EQUAL -> LongPoint.newRangeQuery(fieldName, bytes, Long.MAX_VALUE)
        Operator.LESS_OR_EQUAL -> LongPoint.newRangeQuery(fieldName, Long.MIN_VALUE, bytes)
        Operator.GREATER -> LongPoint.newRangeQuery(fieldName, bytes + 1, Long.MAX_VALUE)
        Operator.LESS -> LongPoint.newRangeQuery(fieldName, Long.MIN_VALUE, bytes - 1)
    }
}

private fun toFileNameQuery(clause: HslNodeClause): Query {
    return when (clause.operator) {
        Operator.EQUALS -> TermQuery(Term(clause.symbol.luceneQueryName, clause.value.str))
        else -> throw InvalidSemanticException("Invalid operator (${clause.operator}) for filename value '${clause.value.str}'")
    }
}

private fun toDataSizeQuery(clause: HslNodeClause): Query {
    return try {
        val dataSize = DataSize.parse(clause.value.str.toUpperCase())
        longQuery(clause.operator, clause.symbol.luceneQueryName, dataSize.toBytes())
    } catch (e: IllegalArgumentException) {
        throw InvalidSemanticException("Expected data-size value for symbol (${clause.symbol.name.toLowerCase()}), but was '${clause.value.str}'")
    }
}

private fun toDateQuery(clause: HslNodeClause): Query {
    return when (val dateTime = parseHslDateTime(clause.symbol, clause.value.str)) {
        is HslDate -> longQuery(clause.operator, clause.symbol.luceneQueryName, dateTime.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        is HslInstant -> longQuery(clause.operator, clause.symbol.luceneQueryName, dateTime.instant.toEpochMilli())
    }
}

private fun HslNodeClause.toLuceneTerm(): Query {
    return when (symbol) {
        Symbol.NAME -> toFileNameQuery(this)
        Symbol.CREATED, Symbol.LAST_MODIFIED -> toDateQuery(this)
        Symbol.SIZE -> toDataSizeQuery(this)
    }
}

class ToLuceneQueryVisitor : HslQueryVisitor<Query> {
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
        return query.toLuceneTerm()
    }
}