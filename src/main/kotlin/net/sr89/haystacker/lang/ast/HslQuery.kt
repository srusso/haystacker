package net.sr89.haystacker.lang.ast

interface HslQuery {

}

interface HslClause: HslQuery {

}

class AndClause(val left: HslClause, val right: HslClause) : HslClause {
}

class OrClause(val left: HslClause, val right: HslClause): HslClause {

}

class NotClause(val underlyingClause: HslClause): HslClause {

}

class HslNodeClause<T>(val symbol: Symbol, val operator: Operator, val value: T): HslClause {

}