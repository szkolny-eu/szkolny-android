/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.config.utils

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import pl.szczodrzynski.edziennik.config.AbstractConfig
import pl.szczodrzynski.edziennik.config.db.ConfigEntry
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

private val gson = Gson()

fun AbstractConfig.set(key: String, value: Int) {
    set(key, value.toString())
}
fun AbstractConfig.set(key: String, value: Boolean) {
    set(key, value.toString())
}
fun AbstractConfig.set(key: String, value: Long) {
    set(key, value.toString())
}
fun AbstractConfig.set(key: String, value: Float) {
    set(key, value.toString())
}
fun AbstractConfig.set(key: String, value: Date?) {
    set(key, value?.stringY_m_d)
}
fun AbstractConfig.set(key: String, value: Time?) {
    set(key, value?.stringValue)
}
fun AbstractConfig.set(key: String, value: JsonElement?) {
    set(key, value?.toString())
}
fun AbstractConfig.set(key: String, value: List<Any>?) {
    set(key, value?.let { gson.toJson(it) })
}
fun AbstractConfig.set(key: String, value: Any?) {
    set(key, value?.let { gson.toJson(it) })
}
fun AbstractConfig.setStringList(key: String, value: List<String>?) {
    set(key, value?.let { gson.toJson(it) })
}
fun AbstractConfig.setIntList(key: String, value: List<Int>?) {
    set(key, value?.let { gson.toJson(it) })
}
fun AbstractConfig.setLongList(key: String, value: List<Long>?) {
    set(key, value?.let { gson.toJson(it) })
}
fun <K, V> AbstractConfig.setMap(key: String, value: Map<K, V>?) {
    set(key, value?.let { gson.toJson(it) })
}

fun HashMap<String, String?>.get(key: String, default: String?): String? {
    return this[key] ?: default
}
fun HashMap<String, String?>.get(key: String, default: Boolean): Boolean {
    return this[key]?.toBoolean() ?: default
}
fun HashMap<String, String?>.get(key: String, default: Int): Int {
    return this[key]?.toIntOrNull() ?: default
}
fun HashMap<String, String?>.get(key: String, default: Long): Long {
    return this[key]?.toLongOrNull() ?: default
}
fun HashMap<String, String?>.get(key: String, default: Float): Float {
    return this[key]?.toFloatOrNull() ?: default
}
fun HashMap<String, String?>.get(key: String, default: Date?): Date? {
    return this[key]?.let { Date.fromY_m_d(it) } ?: default
}
fun HashMap<String, String?>.get(key: String, default: Time?): Time? {
    return this[key]?.let { Time.fromHms(it) } ?: default
}
fun HashMap<String, String?>.get(key: String, default: JsonObject?): JsonObject? {
    return this[key]?.let { JsonParser().parse(it)?.asJsonObject } ?: default
}
fun HashMap<String, String?>.get(key: String, default: JsonArray?): JsonArray? {
    return this[key]?.let { JsonParser().parse(it)?.asJsonArray } ?: default
}
inline fun <reified T> HashMap<String, String?>.get(key: String, default: T?): T? {
    return this[key]?.let { Gson().fromJson(it, T::class.java) } ?: default
}
/* !!! cannot use mutable list here - modifying it will not update the DB */
fun <T> HashMap<String, String?>.get(key: String, default: List<T>?, classOfT: Class<T>): List<T>? {
    return this[key]?.let { ConfigGsonUtils().deserializeList<T>(gson, it, classOfT) } ?: default
}
fun HashMap<String, String?>.getStringList(key: String, default: List<String>?): List<String>? {
    return this[key]?.let { gson.fromJson<List<String>>(it, object: TypeToken<List<String>>(){}.type) } ?: default
}
fun HashMap<String, String?>.getIntList(key: String, default: List<Int>?): List<Int>? {
    return this[key]?.let { gson.fromJson<List<Int>>(it, object: TypeToken<List<Int>>(){}.type) } ?: default
}
fun HashMap<String, String?>.getLongList(key: String, default: List<Long>?): List<Long>? {
    return this[key]?.let { gson.fromJson<List<Long>>(it, object: TypeToken<List<Long>>(){}.type) } ?: default
}

fun HashMap<String, String?>.getFloat(key: String): Float? {
    return this[key]?.toFloatOrNull()
}

fun List<ConfigEntry>.toHashMap(profileId: Int, map: HashMap<String, String?>) {
    map.clear()
    forEach {
        if (it.profileId == profileId)
            map[it.key] = it.value
    }
}
