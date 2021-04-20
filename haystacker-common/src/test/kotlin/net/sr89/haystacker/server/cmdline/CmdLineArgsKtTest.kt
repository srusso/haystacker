package net.sr89.haystacker.server.cmdline

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class CmdLineArgsKtTest {
    @Test
    internal fun portAndHostSpecified() {
        val args = CmdLineArgs(argsOf("--port", "9191", "--host", "myhost"))

        assertEquals(9191, args.port)
        assertEquals("myhost", args.host)
    }

    @Test
    internal fun notParametersArePassedToBashStyleShell() {
        val args = CmdLineArgs(argsOf("--unknown", "--host", "myhost", "extra"))

        assertEquals("myhost", args.host)
        assertEquals(listOf("--unknown", "extra"), args.getArgsForShell().toList())
    }

    @Test
    internal fun defaultValues() {
        val args = CmdLineArgs(argsOf())

        assertNotNull(args.port)
        assertNotNull(args.host)
    }

    private fun argsOf(vararg args: String) = args.toList().toTypedArray()
}