package net.sr89.haystacker.lang.exception

import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol

class InvalidHslGrammarException(val hslQuery: String, val line: Int, val column: Int) : RuntimeException("Invalid grammar in query '$hslQuery' at line $line, column $column")

abstract class InvalidSemanticException : RuntimeException()

class InvalidHslOrderByClause(val symbol: Symbol) : InvalidSemanticException()

class InvalidHslDateException(val symbol: Symbol, val date: String) : InvalidSemanticException()

class InvalidHslDataSizeException(val symbol: Symbol, val dataSize: String) : InvalidSemanticException()

class InvalidHslOperatorException(val symbol: Symbol, val operator: Operator, val value: String) : InvalidSemanticException()

class SettingsUpdateException(message: String): RuntimeException(message)