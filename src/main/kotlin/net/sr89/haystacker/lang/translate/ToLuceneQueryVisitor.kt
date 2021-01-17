package net.sr89.haystacker.lang.translate

import net.sr89.haystacker.lang.ast.HslAndClause
import net.sr89.haystacker.lang.ast.HslDataSize
import net.sr89.haystacker.lang.ast.HslDate
import net.sr89.haystacker.lang.ast.HslDateTime
import net.sr89.haystacker.lang.ast.HslInstant
import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.HslOrClause
import net.sr89.haystacker.lang.ast.HslQueryVisitor
import net.sr89.haystacker.lang.ast.HslString
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery

class ToLuceneQueryVisitor: HslQueryVisitor<Query> {
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
        return TermQuery(Term(query.symbol.name.toLowerCase(), nodeClauseValue(query)))
    }

    private fun nodeClauseValue(query: HslNodeClause): String {
        return when(query.value) {
            is HslString -> query.value.str
            is HslDataSize -> TODO()
            is HslDate -> TODO()
            is HslInstant -> TODO()
            is HslDateTime -> throw AssertionError("Should not get to this case")
        }
    }
}