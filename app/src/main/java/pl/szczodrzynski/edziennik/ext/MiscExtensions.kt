/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.app.PendingIntent
import android.database.Cursor
import android.os.Build
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.PrintWriter
import java.io.StringWriter

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(value: T) {
            observer.onChanged(value)
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

fun pendingIntentFlag(): Int {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        return PendingIntent.FLAG_IMMUTABLE
    return 0
}

fun pendingIntentMutable(): Int {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        return PendingIntent.FLAG_MUTABLE
    return 0
}

fun Int?.takeValue() = if (this == -1) null else this
fun Int?.takePositive() = if (this == -1 || this == 0) null else this

fun Long?.takeValue() = if (this == -1L) null else this
fun Long?.takePositive() = if (this == -1L || this == 0L) null else this

fun String?.takeValue() = if (this.isNullOrBlank()) null else this

fun Any?.ignore() = Unit

fun EventBus.registerSafe(subscriber: Any) = try {
    EventBus.getDefault().register(subscriber)
} catch (_: Exception) {
}

fun EventBus.unregisterSafe(subscriber: Any) = try {
    EventBus.getDefault().unregister(subscriber)
} catch (_: Exception) {
}
