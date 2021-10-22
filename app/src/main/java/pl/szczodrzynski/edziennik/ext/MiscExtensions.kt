/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.database.Cursor
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T?) {
            observer.onChanged(t)
            removeObserver(this)
        }
    })
}

fun CoroutineScope.startCoroutineTimer(delayMillis: Long = 0, repeatMillis: Long = 0, action: suspend CoroutineScope.() -> Unit) = launch {
    delay(delayMillis)
    if (repeatMillis > 0) {
        while (true) {
            action()
            delay(repeatMillis)
        }
    } else {
        action()
    }
}

inline fun <reified T> Any?.instanceOfOrNull(): T? {
    return when (this) {
        is T -> this
        else -> null
    }
}

val Throwable.stackTraceString: String
    get() {
        val sw = StringWriter()
        printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

fun Cursor?.getString(columnName: String) = this?.getStringOrNull(getColumnIndex(columnName))
fun Cursor?.getInt(columnName: String) = this?.getIntOrNull(getColumnIndex(columnName))
fun Cursor?.getLong(columnName: String) = this?.getLongOrNull(getColumnIndex(columnName))

inline fun <A, B, R> ifNotNull(a: A?, b: B?, code: (A, B) -> R): R? {
    if (a != null && b != null) {
        return code(a, b)
    }
    return null
}

infix fun Int.hasSet(what: Int) = this and what == what
