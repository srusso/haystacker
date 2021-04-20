package net.sr89.haystacker.server.cmdline

class CmdLineArgs(commandLineArgs: Array<String>) {
    private val argList = commandLineArgs.toMutableList()

    private fun String.isDesiredArgumentName(argNames: Array<out String>) =
        argNames.any { "--$it" == this || "-$it" == this }

    private fun <T> MutableList<String>.getArgument(
        convert: (String) -> T,
        defaultValue: T,
        vararg argNames: String
    ): T {
        for (i in 0 until size - 1) {
            if (this[i].isDesiredArgumentName(argNames)) {
                val returnValue = convert(this[i + 1])

                this.removeAt(i + 1)
                this.removeAt(i)

                return returnValue
            }
        }

        return defaultValue
    }

    private fun MutableList<String>.getIntArgument(defaultValue: Int, vararg argNames: String): Int {
        return getArgument({ s -> s.toInt() }, defaultValue, *argNames)
    }

    private fun MutableList<String>.getStringArgument(defaultValue: String, vararg argNames: String): String {
        return getArgument({ s -> s }, defaultValue, *argNames)
    }

    val port: Int = argList.getIntArgument(9000, "port", "p")
    val host: String = argList.getStringArgument("localhost", "host", "h")
    val settingsDirectory: String = argList.getStringArgument(".", "settings", "s")
    val index: String = argList.getStringArgument("", "index", "i")

    fun getArgsForShell() = argList.toTypedArray()
}