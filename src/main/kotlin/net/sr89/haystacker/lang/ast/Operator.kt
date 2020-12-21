package net.sr89.haystacker.lang.ast

enum class Operator(val symbol: String) {
    EQUALS("="), GREATER_OR_EQUAL(">="), LESS_OR_EQUAL("<="),
    GREATER(">"), LESS("<")
}