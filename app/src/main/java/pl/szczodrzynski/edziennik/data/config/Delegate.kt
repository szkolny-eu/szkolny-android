/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-21.
 */

package pl.szczodrzynski.edziennik.data.config

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import timber.log.Timber
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.reflect.KProperty

private val gson = Gson()

inline fun <reified T> BaseConfig<*>.config(noinline default: () -> T) = ConfigDelegate(
    config = this,
    type = T::class.java,
    nullable = null is T,
    typeToken = object : TypeToken<T>() {},
    defaultFunc = default,
    defaultValue = null,
    fieldName = null,
)

inline fun <reified T> BaseConfig<*>.config(name: String? = null, noinline default: () -> T) = ConfigDelegate(
    config = this,
    type = T::class.java,
    nullable = null is T,
    typeToken = object : TypeToken<T>() {},
    defaultFunc = default,
    defaultValue = null,
    fieldName = name,
)

inline fun <reified T> BaseConfig<*>.config(default: T) = ConfigDelegate(
    config = this,
    type = T::class.java,
    nullable = null is T,
    typeToken = object : TypeToken<T>() {},
    defaultFunc = null,
    defaultValue = default,
    fieldName = null,
)

inline fun <reified T> BaseConfig<*>.config(name: String? = null, default: T) = ConfigDelegate(
    config = this,
    type = T::class.java,
    nullable = null is T,
    typeToken = object : TypeToken<T>() {},
    defaultFunc = null,
    defaultValue = default,
    fieldName = name,
)

@Suppress("UNCHECKED_CAST")
class ConfigDelegate<T>(
    private val config: BaseConfig<*>,
    private val type: Class<T>,
    private val nullable: Boolean,
    private val typeToken: TypeToken<T>,
    private val defaultFunc: (() -> T)?,
    private val defaultValue: T?,
    private val fieldName: String?,
) {
    companion object {
        private const val TAG = "ConfigDelegate"
    }

    private var value: T? = null
    private var isInitialized = false

    private fun getDefault(): T = when {
        defaultFunc != null -> defaultFunc.invoke()
        else -> defaultValue as T
    }

    private fun getGenericType(index: Int = 0): Class<*> {
        val parameterizedType = typeToken.type as ParameterizedType
        val typeArgument = parameterizedType.actualTypeArguments[index] as WildcardType
        return typeArgument.upperBounds[0] as Class<*>
    }

    operator fun setValue(_thisRef: Any, property: KProperty<*>, newValue: T) {
        value = newValue
        isInitialized = true
        config[fieldName ?: property.name] = serialize(newValue)?.toString()
    }

    operator fun getValue(_thisRef: Any, property: KProperty<*>): T {
        if (isInitialized)
            return value as T
        val key = fieldName ?: property.name

        if (key !in config) {
            value = getDefault()
            isInitialized = true
            return value as T
        }

        try {
            value = config[key]?.let(::deserialize)
        } catch (e: Exception) {
            value = getDefault()
            Timber.e(e, "Couldn't deserialize '$key'")
        }
        if (value == null && !nullable)
            value = getDefault()

        isInitialized = true
        return value as T
    }

    private fun <I> serialize(value: I?, serializeObjects: Boolean = true): Any? {
        if (value == null)
            return null

        return when (value) {
            is String -> value
            is Date -> value.stringY_m_d
            is Time -> value.stringValue
            is JsonObject -> value
            is JsonArray -> value
            // primitives
            is Number -> value
            is Boolean -> value
            // enums, maps & collections
            is Enum<*> -> value.toString()
            is Collection<*> -> value.map {
                if (it is Number || it is Boolean) it else serialize(it, serializeObjects = false)
            }.toJsonElement()
            is Map<*, *> -> gson.toJson(value.mapValues { (_, it) ->
                if (it is Number || it is Boolean) it else serialize(it, serializeObjects = false)
            })
            // objects or else
            else -> if (serializeObjects) gson.toJson(value) else value
        }
    }

    private fun <I> deserialize(value: String?, type: Class<*> = this.type): I? {
        if (value == null)
            return null

        @Suppress("USELESS_CAST")
        return when (type) {
            String::class.java -> value
            Date::class.java -> Date.fromY_m_d(value)
            Time::class.java -> Time.fromHms(value)
            JsonObject::class.java -> value.toJsonObject()
            JsonArray::class.java -> value.toJsonArray()
            // primitives
            java.lang.Integer::class.java -> value.toIntOrNull()
            java.lang.Boolean::class.java -> value.toBooleanStrictOrNull()
            java.lang.Long::class.java -> value.toLongOrNull()
            java.lang.Float::class.java -> value.toFloatOrNull()
            // enums, maps & collections
            else -> when {
                Enum::class.java.isAssignableFrom(type) -> value.toEnumOrNull(type) as Enum?
                Collection::class.java.isAssignableFrom(type) -> {
                    val array = value.toJsonArray()
                    val genericType = getGenericType()
                    val list = array?.map {
                        val str = if (it.isJsonPrimitive) it.asString else it.toString()
                        deserialize<Any>(str, genericType)
                    }
                    when {
                        List::class.java.isAssignableFrom(type) -> list
                        Set::class.java.isAssignableFrom(type) -> list?.toSet()
                        else -> list?.toTypedArray()
                    }
                }
                Map::class.java.isAssignableFrom(type) -> {
                    val obj = value.toJsonObject()
                    val genericType = getGenericType(index = 1)
                    val map = obj?.entrySet()?.associate { (key, it) ->
                        val str = if (it.isJsonPrimitive) it.asString else it.toString()
                        key to deserialize<Any>(str, genericType)
                    }
                    map
                }
                // objects or else
                else -> gson.fromJson(value, type)
            }
        } as? I
    }
}
