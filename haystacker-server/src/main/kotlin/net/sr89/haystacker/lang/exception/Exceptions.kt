package net.sr89.haystacker.lang.exception

import net.sr89.haystacker.lang.ast.Symbol


class InvalidSemanticException(message: String) : RuntimeException(message)

class InvalidHslGrammarException(val hslQuery: String, val line: Int, val column: Int) : RuntimeException()

open class HslParseException : RuntimeException()

class InvalidHslDateException(val symbol: Symbol, val date: String) : HslParseException()