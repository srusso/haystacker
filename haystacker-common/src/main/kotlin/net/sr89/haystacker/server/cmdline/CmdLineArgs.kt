package net.sr89.haystacker.server.cmdline

class CmdLineArgs(commandLineArgs: Array<String>) {
    private val argList = commandLineArgs.toMutableList()

    private fun String.isDesiredArgumentName(argNames: Array<out String>) = argNames.any { "--$it" == this || "-$it" == this }

    private fun MutableList<String>.getIntArgument(defaultValue: Int, vararg argNames: String): Int {
        for (i in 0 until size - 1) {
            if (this[i].isDesiredArgumentName(argNames)) {
                val returnValue = this[i + 1].toInt()

                this.removeAt(i + 1)
                this.removeAt(i)

                return returnValue
            }
        }

        return defaultValue
    }

    private fun MutableList<String>.getStringArgument(defaultValue: String, vararg argNames: String): String {
        for (i in 0 until size - 1) {
            if (this[i].isDesiredArgumentName(argNames)) {
                val returnValue = this[i + 1]

                this.removeAt(i + 1)
                this.removeAt(i)

                return returnValue
            }
        }

        return defaultValue
    }

    val port: Int = argList.getIntArgument(9000, "port", "p")
    val host: String = argList.getStringArgument("localhost", "host", "h")
    val settingsDirectory: String = argList.getStringArgument(".", "settings", "s")

    fun getArgsForShell() = argList.toTypedArray()
}