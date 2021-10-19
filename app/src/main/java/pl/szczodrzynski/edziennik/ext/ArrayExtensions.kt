/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannedString
import android.util.LongSparseArray
import android.util.SparseArray
import android.util.SparseIntArray
import androidx.core.util.forEach
import java.util.*

fun <T> Collection<T>?.isNotNullNorEmpty(): Boolean {
    return this != null && this.isNotEmpty()
}

fun List<String>.join(delimiter: String): String {
    return concat(delimiter).toString()
}

fun <T> LongSparseArray<T>.values(): List<T> {
    val result = mutableListOf<T>()
    forEach { _, value ->
        result += value
    }
    return result
}

fun SparseArray<*>.keys(): List<Int> {
    val result = mutableListOf<Int>()
    forEach { key, _ ->
        result += key
    }
    return result
}
fun <T> SparseArray<T>.values(): List<T> {
    val result = mutableListOf<T>()
    forEach { _, value ->
        result += value
    }
    return result
}

fun SparseIntArray.keys(): List<Int> {
    val result = mutableListOf<Int>()
    forEach { key, _ ->
        result += key
    }
    return result
}
fun SparseIntArray.values(): List<Int> {
    val result = mutableListOf<Int>()
    forEach { _, value ->
        result += value
    }
    return result
}

fun <K, V> List<Pair<K, V>>.keys(): List<K> {
    val result = mutableListOf<K>()
    forEach { pair ->
        result += pair.first
    }
    return result
}
fun <K, V> List<Pair<K, V>>.values(): List<V> {
    val result = mutableListOf<V>()
    forEach { pair ->
        result += pair.second
    }
    return result
}

fun <T> List<T>.toSparseArray(destination: SparseArray<T>, key: (T) -> Int) {
    forEach {
        destination.put(key(it), it)
    }
}
fun <T> List<T>.toSparseArray(destination: LongSparseArray<T>, key: (T) -> Long) {
    forEach {
        destination.put(key(it), it)
    }
}

fun <T> List<T>.toSparseArray(key: (T) -> Int): SparseArray<T> {
    val result = SparseArray<T>()
    toSparseArray(result, key)
    return result
}
fun <T> List<T>.toSparseArray(key: (T) -> Long): LongSparseArray<T> {
    val result = LongSparseArray<T>()
    toSparseArray(result, key)
    return result
}

fun <T> SparseArray<T>.singleOrNull(predicate: (T) -> Boolean): T? {
    forEach { _, value ->
        if (predicate(value))
            return value
    }
    return null
}
fun <T> LongSparseArray<T>.singleOrNull(predicate: (T) -> Boolean): T? {
    forEach { _, value ->
        if (predicate(value))
            return value
    }
    return null
}

/**
 * Returns a new read-only list only of those given elements, that are not empty.
 * Applies for CharSequence and descendants.
 */
fun <T : CharSequence> listOfNotEmpty(vararg elements: T): List<T> = elements.filterNot { it.isEmpty() }

fun List<CharSequence?>.concat(delimiter: CharSequence? = null): CharSequence {
    if (this.isEmpty()) {
        return ""
    }

    if (this.size == 1) {
        return this[0] ?: ""
    }

    var spanned = delimiter is Spanned
    if (!spanned) {
        for (piece in this) {
            if (piece is Spanned) {
                spanned = true
                break
            }
        }
    }

    var first = true
    if (spanned) {
        val ssb = SpannableStringBuilder()
        for (piece in this) {
            if (piece == null)
                continue
            if (!first && delimiter != null)
                ssb.append(delimiter)
            first = false
            ssb.append(piece)
        }
        return SpannedString(ssb)
    } else {
        val sb = StringBuilder()
        for (piece in this) {
            if (piece == null)
                continue
            if (!first && delimiter != null)
                sb.append(delimiter)
            first = false
            sb.append(piece)
        }
        return sb.toString()
    }
}

inline fun <T> List<T>.ifNotEmpty(block: (List<T>) -> Unit) {
    if (!isEmpty())
        block(this)
}

inline fun <T> LongSparseArray<T>.filter(predicate: (T) -> Boolean): List<T> {
    val destination = ArrayList<T>()
    this.forEach { _, element -> if (predicate(element)) destination.add(element) }
    return destination
}

fun CharSequence.containsAll(list: List<CharSequence>, ignoreCase: Boolean = false): Boolean {
    for (i in list) {
        if (!contains(i, ignoreCase))
            return false
    }
    return true
}

fun <E> MutableList<E>.after(what: E, insert: E) {
    val index = indexOf(what)
    if (index != -1)
        add(index + 1, insert)
}

fun <E> MutableList<E>.before(what: E, insert: E) {
    val index = indexOf(what)
    if (index != -1)
        add(index, insert)
}

fun <E> MutableList<E>.after(what: E, insert: Collection<E>) {
    val index = indexOf(what)
    if (index != -1)
        addAll(index + 1, insert)
}

fun <E> MutableList<E>.before(what: E, insert: Collection<E>) {
    val index = indexOf(what)
    if (index != -1)
        addAll(index, insert)
}

operator fun <K, V> Iterable<Pair<K, V>>.get(key: K): V? {
    return firstOrNull { it.first == key }?.second
}

@kotlin.jvm.JvmName("averageOrNullOfInt")
fun Iterable<Int>.averageOrNull() = this.average().let { if (it.isNaN()) null else it }
@kotlin.jvm.JvmName("averageOrNullOfFloat")
fun Iterable<Float>.averageOrNull() = this.average().let { if (it.isNaN()) null else it }
