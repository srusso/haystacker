package net.sr89.haystacker.test.common

import net.sr89.haystacker.index.IndexManager
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TopDocs
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

class ResultAssertions(val foundDocuments: List<Document>) {
    fun includingDocumentWith(term: Term): ResultAssertions {
        val elements = foundDocuments
            .mapNotNull { d -> d.get(term.field()) }
            .filter { binaryValue -> binaryValue == term.text() }
            .toList()

        assertTrue(elements.isNotEmpty(), "Expected term $term")
        return this
    }
}

fun hasHits(manager: IndexManager, docs: TopDocs, hits: Int): ResultAssertions {
    assertEquals(docs.totalHits.value, hits.toLong())

    return ResultAssertions(
        docs.scoreDocs.map(ScoreDoc::doc).mapNotNull(manager::fetchDocument).toList()
    )
}