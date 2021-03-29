package net.sr89.haystacker.server.cmdline

private fun String.isDesiredArgumentName(argNames: Array<out String>) = argNames.any { "--$it" == this || "-$it" == this }

private fun Array<String>.getIntArgument(defaultValue: Int, vararg argNames: String): Int {
    for (i in 0 until size - 1) {
        if (this[i].isDesiredArgumentName(argNames)) {
            return this[i + 1].toInt()
        }
    }

    return defaultValue
}

private fun Array<String>.getStringArgument(defaultValue: String, vararg argNames: String): String {
    for (i in 0 until size - 1) {
        if (this[i].isDesiredArgumentName(argNames)) {
            return this[i + 1]
        }
    }

    return defaultValue
}

fun Array<String>.getPortOrDefault() = getIntArgument(9000, "port", "p")

fun Array<String>.getHostOrDefault() = getStringArgument("localhost", "host", "h")

fun Array<String>.getSettingsDirectory() = getStringArgument(".", "settings", "s")