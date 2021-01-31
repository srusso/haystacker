package net.sr89.haystacker.lang.ast

enum class Symbol(val luceneQueryName: String) {
    NAME("path"),
    SIZE("size"),
    CREATED("created"),
    LAST_MODIFIED("last_modified")
}