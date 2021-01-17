package net.sr89.haystacker.lang.translate.visit

import net.sr89.haystacker.lang.ast.HslDataSize
import net.sr89.haystacker.lang.ast.HslDate
import net.sr89.haystacker.lang.ast.HslInstant
import net.sr89.haystacker.lang.ast.HslString
import net.sr89.haystacker.lang.ast.HslValueVisitor
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Operator.EQUALS
import net.sr89.haystacker.lang.ast.Operator.GREATER
import net.sr89.haystacker.lang.ast.Operator.GREATER_OR_EQUAL
import net.sr89.haystacker.lang.ast.Operator.LESS
import net.sr89.haystacker.lang.ast.Operator.LESS_OR_EQUAL
import net.sr89.haystacker.lang.ast.Symbol
import net.sr89.haystacker.lang.ast.Symbol.CREATED
import net.sr89.haystacker.lang.ast.Symbol.LAST_MODIFIED
import net.sr89.haystacker.lang.ast.Symbol.SIZE
import net.sr89.haystacker.lang.exception.InvalidSemanticException
import org.apache.lucene.document.LongPoint
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

class ToLuceneTermVisitor(val symbol: Symbol, val operator: Operator): HslValueVisitor<Query> {
    override fun accept(value: HslString): Query {
        if (symbol == Symbol.NAME && operator == EQUALS) {
            return TermQuery(Term(symbol.name.toLowerCase(), value.str))
        } else {
            throw InvalidSemanticException("Invalid symbol (${symbol.name.toLowerCase()}) or operator ($operator) for string value '${value.str}'")
        }
    }

    override fun accept(value: HslDataSize): Query {
        if (symbol != SIZE) {
            throw InvalidSemanticException("Symbol ${symbol.name.toLowerCase()} does not accept a data-size value like ${value.size}")
        }

        return longQuery(symbol.name.toLowerCase(), value.size.toBytes())
    }

    override fun accept(value: HslDate): Query {
        if (symbol !in setOf(LAST_MODIFIED, CREATED)) {
            throw InvalidSemanticException("Symbol ${symbol.name.toLowerCase()} does not accept a date value like ${value.date}")
        }

        return longQuery(symbol.name.toLowerCase(), value.date.atStartOfDay(UTC).toInstant().toEpochMilli())
    }

    override fun accept(value: HslInstant): Query {
        if (symbol !in setOf(LAST_MODIFIED, CREATED)) {
            throw InvalidSemanticException("Symbol ${symbol.name.toLowerCase()} does not accept a datetime value like ${LocalDateTime.ofInstant(value.instant, UTC)}")
        }

        return longQuery(symbol.name.toLowerCase(), value.instant.toEpochMilli())
    }

    private fun longQuery(fieldName: String, bytes: Long): Query {
        return when (operator) {
            EQUALS -> LongPoint.newExactQuery(fieldName, bytes)
            GREATER_OR_EQUAL -> LongPoint.newRangeQuery(fieldName, bytes, Long.MAX_VALUE)
            LESS_OR_EQUAL -> LongPoint.newRangeQuery(fieldName, Long.MIN_VALUE, bytes)
            GREATER -> LongPoint.newRangeQuery(fieldName, bytes + 1, Long.MAX_VALUE)
            LESS -> LongPoint.newRangeQuery(fieldName, Long.MIN_VALUE, bytes - 1)
        }
    }
}