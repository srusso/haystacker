package net.sr89.haystacker.lang.translate.visit

import net.sr89.haystacker.lang.ast.HslDataSize
import net.sr89.haystacker.lang.ast.HslDate
import net.sr89.haystacker.lang.ast.HslInstant
import net.sr89.haystacker.lang.ast.HslString
import net.sr89.haystacker.lang.ast.HslValueVisitor
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery

class ToLuceneTermVisitor(val symbol: Symbol, val operator: Operator): HslValueVisitor<Query> {
    override fun accept(value: HslString): Query {
        return TermQuery(Term(symbol.name.toLowerCase(), value.str))
    }

    override fun accept(value: HslDataSize): Query {
        TODO("Not yet implemented")
    }

    override fun accept(value: HslDate): Query {
        TODO("Not yet implemented")
    }

    override fun accept(value: HslInstant): Query {
        TODO("Not yet implemented")
    }

}