package net.sr89.haystacker.server

import com.sun.jna.Library
import com.sun.jna.Native

/** Simple example of JNA interface mapping and usage.  */
object HelloWorld {
    @JvmStatic
    fun main(args: Array<String>) {
        println(CLibrary.INSTANCE.testString())
    }

    // This is the standard, stable way of mapping, which supports extensive
    // customization and mapping of Java to native types.
    interface CLibrary : Library {
        fun testString(): String

        companion object {
            val INSTANCE = Native.load("haystacker-fs-watcher-windows-lib", CLibrary::class.java) as CLibrary
        }
    }
}