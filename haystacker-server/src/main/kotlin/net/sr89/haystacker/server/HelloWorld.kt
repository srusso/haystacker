package net.sr89.haystacker.server

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder("a", "b")
sealed class FSChangeNotification : Structure() {
    @JvmField var a: Int = 0
    @JvmField var b: Int = 0

    class ByValue : FSChangeNotification(), Structure.ByValue
    class ByReference : FSChangeNotification(), Structure.ByReference
}

/** Simple example of JNA interface mapping and usage.  */
object HelloWorld {
    @JvmStatic
    fun main(args: Array<String>) {
        println(CLibrary.INSTANCE.testString())

        val notif = CLibrary.INSTANCE.pollChangeNotification()

        println("${notif.a}, ${notif.b}")
    }

    // This is the standard, stable way of mapping, which supports extensive
    // customization and mapping of Java to native types.
    interface CLibrary : Library {
        fun testString(): String
        fun pollChangeNotification(): FSChangeNotification.ByValue

        companion object {
            val INSTANCE = Native.load("haystacker-fs-watcher-windows-lib", CLibrary::class.java) as CLibrary
        }
    }
}