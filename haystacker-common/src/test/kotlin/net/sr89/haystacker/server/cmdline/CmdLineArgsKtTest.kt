package net.sr89.haystacker.server.cmdline

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class CmdLineArgsKtTest {
    @Test
    internal fun portAndHostSpecified() {
        val args = argsOf("--port", "9191", "--host", "myhost")

        assertEquals(9191, args.getPortOrDefault())
        assertEquals("myhost", args.getHostOrDefault())
    }

    @Test
    internal fun unknownArgumentsIgnored() {
        val args = argsOf("--unknown", "--host", "myhost", "extra")

        assertEquals("myhost", args.getHostOrDefault())
    }

    @Test
    internal fun defaultValues() {
        val args = argsOf()

        assertNotNull(args.getPortOrDefault())
        assertNotNull(args.getHostOrDefault())
    }

    private fun argsOf(vararg args: String) = args.toList().toTypedArray()
}