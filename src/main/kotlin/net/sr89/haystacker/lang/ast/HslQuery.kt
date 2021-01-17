package net.sr89.haystacker.lang.ast

interface HslQueryVisitor<T> {
    fun visit(query: HslAndClause): T
    fun visit(query: HslOrClause): T
    fun visit(query: HslNodeClause): T
}

interface HslQuery {
    fun <T> accept(visitor: HslQueryVisitor<T>): T
}

interface HslClause: HslQuery

class HslAndClause(val left: HslClause, val right: HslClause) : HslClause {
    override fun <T> accept(visitor: HslQueryVisitor<T>): T {
        return visitor.visit(this)
    }
}

class HslOrClause(val left: HslClause, val right: HslClause): HslClause {
    override fun <T> accept(visitor: HslQueryVisitor<T>): T {
        return visitor.visit(this)
    }

}

class HslNodeClause(val symbol: Symbol, val operator: Operator, val value: HslValue): HslClause {
    override fun <T> accept(visitor: HslQueryVisitor<T>): T {
        return visitor.visit(this)
    }

}