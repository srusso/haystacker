package net.sr89.haystacker.test.common

import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.cast

class CastableObject(private val obj: Any) {
    fun <T : Any> ofType(clazz: KClass<T>): CastObject<T> {
        if (clazz.isInstance(obj)) {
            return CastObject(clazz.cast(obj))
        } else {
            throw ClassCastException("Item was expected to be of type " + clazz::class)
        }
    }
}

class CastObject<T>(private val obj: T) {
    fun then(block: (T) -> Unit) = block.invoke(obj)
}

fun having(obj: Any) = CastableObject(obj)

fun timeAction(action: () -> Unit, actionName: String) {
    val start = System.currentTimeMillis()
    action.invoke()
    val end = System.currentTimeMillis()
    println("$actionName took ${Duration.ofMillis(end - start).toMillis()} ms")
}