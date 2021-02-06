package net.sr89.haystacker.lang.exception

class InvalidSemanticException(message: String) : RuntimeException(message)

class InvalidHslGrammarException(val hslQuery: String) : RuntimeException()

open class HslParseException: RuntimeException()

class InvalidHslDateException(val date: Any) : HslParseException()