package net.sr89.haystacker.lang.ast

interface HslQuery {

}

interface HslClause: HslQuery {

}

class HslAndClause(val left: HslClause, val right: HslClause) : HslClause {
}

class HslOrClause(val left: HslClause, val right: HslClause): HslClause {

}

class HslNodeClause(val symbol: Symbol, val operator: Operator, val value: HslValue): HslClause {

}