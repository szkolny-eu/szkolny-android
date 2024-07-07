/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-6.
 */

@file:Suppress("UNCHECKED_CAST")

package pl.szczodrzynski.edziennik.ext

fun <T> Any.getDeclaredField(name: String): T? = this::class.java.getDeclaredField(name).run {
    isAccessible = true
    get(this@getDeclaredField) as? T
}

fun Any.setDeclaredField(name: String, value: Any?) = this::class.java.getDeclaredField(name).run {
    isAccessible = true
    set(this@setDeclaredField, value)
}

fun Any.invokeDeclaredMethod(name: String, vararg args: Pair<Class<*>, Any>): Any? =
    this::class.java.getDeclaredMethod(name, *args.map { (k, _) -> k }.toTypedArray()).run {
        isAccessible = true
        invoke(this@invokeDeclaredMethod, *args.map { (_, v) -> v }.toTypedArray())
    }

fun <T> Class<T>.invokeDeclaredConstructor(vararg args: Pair<Class<*>, Any>): T =
    getDeclaredConstructor(*args.map { (k, _) -> k }.toTypedArray()).run {
        isAccessible = true
        newInstance(*args.map { (_, v) -> v }.toTypedArray())
    }

fun <T> Class<*>.getDeclaredClass(name: String): Class<T> =
    declaredClasses.first { it.simpleName == name } as Class<T>
