/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-18.
 */

@file:Suppress("UNCHECKED_CAST")

package pl.szczodrzynski.edziennik.utils

internal object UNINITIALIZED_VALUE
class MutableLazyImpl<T>(initializer: () -> T, lock: Any? = null) {
    private var initializer: (() -> T)? = initializer
    @Volatile private var _value: Any? = UNINITIALIZED_VALUE
    private val lock = lock ?: this

    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
        if (_value !== UNINITIALIZED_VALUE)
            return _value as T;

        return synchronized(lock) {
            val typedValue = initializer!!()
            _value = typedValue
            initializer = null
            typedValue
        }
    }
    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
        _value = value
    }

    fun isInitialized() = _value !== UNINITIALIZED_VALUE

    override fun toString() = if (isInitialized()) _value.toString() else "ChangeableLazy value not initialized yet."
}

fun <T> mutableLazy(initializer: () -> T): MutableLazyImpl<T> = MutableLazyImpl(initializer)