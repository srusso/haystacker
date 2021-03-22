package net.sr89.haystacker.lang.ast

// TODO refactor this: the luceneQueryName absolutely does not belong in this module
enum class Symbol(val luceneQueryName: String) {
    NAME("path"),
    SIZE("size"),
    CREATED("created"),
    LAST_MODIFIED("modified")
}