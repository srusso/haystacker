package net.sr89.haystacker.server.calc

fun Double.boundValue(min: Double, max: Double): Double {
    return when {
        this < min -> min
        this > max -> max
        else -> this
    }
}