package net.sr89.haystacker.lang.translate.visit

import net.sr89.haystacker.lang.ast.HslAndClause
import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.HslOrClause
import net.sr89.haystacker.lang.ast.HslQueryVisitor
import net.sr89.haystacker.lang.translate.and
import net.sr89.haystacker.lang.translate.or
import org.apache.lucene.search.Query

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
        return query.value.accept(ToLuceneTermVisitor(query.symbol, query.operator))
    }
}