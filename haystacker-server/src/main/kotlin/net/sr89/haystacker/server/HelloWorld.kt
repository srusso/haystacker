package net.sr89.haystacker.server

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Platform

/** Simple example of JNA interface mapping and usage.  */
object HelloWorld {
    @JvmStatic
    fun main(args: Array<String>) {
        CLibrary.INSTANCE.printf("Hello, World\n")
        for (i in args.indices) {
            CLibrary.INSTANCE.printf("Argument %d: %s\n", i, args[i])
        }
    }

    // This is the standard, stable way of mapping, which supports extensive
    // customization and mapping of Java to native types.
    interface CLibrary : Library {
        fun printf(format: String?, vararg args: Any?)

        companion object {
            val INSTANCE = Native.load(if (Platform.isWindows()) "msvcrt" else "c",
                CLibrary::class.java) as CLibrary
        }
    }
}