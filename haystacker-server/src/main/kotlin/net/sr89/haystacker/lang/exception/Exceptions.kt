package net.sr89.haystacker.lang.exception

import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol
import java.lang.IllegalArgumentException

class InvalidHslGrammarException(val hslQuery: String, val line: Int, val column: Int) : RuntimeException()

class HslParseException : RuntimeException()

abstract class InvalidSemanticException : RuntimeException()

class InvalidHslDateException(val symbol: Symbol, val date: String) : InvalidSemanticException()

class InvalidHslDataSizeException(val symbol: Symbol, val dataSize: String) : InvalidSemanticException()

class InvalidHslOperatorException(val symbol: Symbol, val operator: Operator, val value: String) : InvalidSemanticException()

class InvalidTaskIdException(val taskId: String) : IllegalArgumentException()
