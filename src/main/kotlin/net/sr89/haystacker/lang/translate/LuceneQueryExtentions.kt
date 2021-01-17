package net.sr89.haystacker.lang.translate

import org.apache.lucene.search.BooleanClause.Occur.MUST
import org.apache.lucene.search.BooleanClause.Occur.SHOULD
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query

fun Query.or(other: Query): Query {
    return BooleanQuery.Builder()
        .add(this, SHOULD)
        .add(other, SHOULD)
        .build()
}

fun Query.and(other: Query): Query {
    return BooleanQuery.Builder()
        .add(this, MUST)
        .add(other, MUST)
        .build()
}