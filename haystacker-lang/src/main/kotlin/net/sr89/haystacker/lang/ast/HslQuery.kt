package net.sr89.haystacker.lang.ast

interface HslClauseVisitor<T> {
    fun visit(query: HslAndClause): T
    fun visit(query: HslOrClause): T
    fun visit(query: HslNodeClause): T
}

enum class SortOrder { ASCENDING, DESCENDING }

data class HslSortField(val field: Symbol, val sortOrder: SortOrder)

data class HslSort(val sortFields: List<HslSortField>)

data class HslQuery(val clause: HslClause, val sort: HslSort)

interface HslClause {
    fun <T> accept(visitor: HslClauseVisitor<T>): T
}

class HslAndClause(val left: HslClause, val right: HslClause) : HslClause {
    override fun <T> accept(visitor: HslClauseVisitor<T>) = visitor.visit(this)
}

class HslOrClause(val left: HslClause, val right: HslClause) : HslClause {
    override fun <T> accept(visitor: HslClauseVisitor<T>) = visitor.visit(this)
}

class HslNodeClause(val symbol: Symbol, val operator: Operator, val value: HslValue) : HslClause {
    override fun <T> accept(visitor: HslClauseVisitor<T>) = visitor.visit(this)
}