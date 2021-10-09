package pl.szczodrzynski.edziennik

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.database.Cursor
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.*
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.util.*
import android.util.Base64
import android.util.Base64.NO_WRAP
import android.util.Base64.encodeToString
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.annotation.*
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.util.forEach
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import im.wangchao.mhttp.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import okio.Buffer
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApiException
import pl.szczodrzynski.edziennik.data.api.szkolny.response.ApiResponse
import pl.szczodrzynski.edziennik.data.db.entity.Notification
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.entity.Team
import pl.szczodrzynski.edziennik.utils.models.Time
import java.io.InterruptedIOException
import java.io.PrintWriter
import java.io.StringWriter
import java.math.BigInteger
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.SSLException
import kotlin.Pair


fun List<Teacher>.byId(id: Long) = firstOrNull { it.id == id }
fun List<Teacher>.byNameFirstLast(nameFirstLast: String) = firstOrNull { it.name + " " + it.surname == nameFirstLast }
fun List<Teacher>.byNameLastFirst(nameLastFirst: String) = firstOrNull { it.surname + " " + it.name == nameLastFirst }
fun List<Teacher>.byNameFDotLast(nameFDotLast: String) = firstOrNull { it.name + "." + it.surname == nameFDotLast }
fun List<Teacher>.byNameFDotSpaceLast(nameFDotSpaceLast: String) = firstOrNull { it.name + ". " + it.surname == nameFDotSpaceLast }

fun JsonObject?.get(key: String): JsonElement? = this?.get(key)

fun JsonObject?.getBoolean(key: String): Boolean? = get(key)?.let { if (!it.isJsonPrimitive) null else it.asBoolean }
fun JsonObject?.getString(key: String): String? = get(key)?.let { if (!it.isJsonPrimitive) null else it.asString }
fun JsonObject?.getInt(key: String): Int? = get(key)?.let { if (!it.isJsonPrimitive) null else it.asInt }
fun JsonObject?.getLong(key: String): Long? = get(key)?.let { if (!it.isJsonPrimitive) null else it.asLong }
fun JsonObject?.getFloat(key: String): Float? = get(key)?.let { if(!it.isJsonPrimitive) null else it.asFloat }
fun JsonObject?.getChar(key: String): Char? = get(key)?.let { if(!it.isJsonPrimitive) null else it.asCharacter }
fun JsonObject?.getJsonObject(key: String): JsonObject? = get(key)?.let { if (it.isJsonObject) it.asJsonObject else null }
fun JsonObject?.getJsonArray(key: String): JsonArray? = get(key)?.let { if (it.isJsonArray) it.asJsonArray else null }

fun JsonObject?.getBoolean(key: String, defaultValue: Boolean): Boolean = get(key)?.let { if (!it.isJsonPrimitive) defaultValue else it.asBoolean } ?: defaultValue
fun JsonObject?.getString(key: String, defaultValue: String): String = get(key)?.let { if (!it.isJsonPrimitive) defaultValue else it.asString } ?: defaultValue
fun JsonObject?.getInt(key: String, defaultValue: Int): Int = get(key)?.let { if (!it.isJsonPrimitive) defaultValue else it.asInt } ?: defaultValue
fun JsonObject?.getLong(key: String, defaultValue: Long): Long = get(key)?.let { if (!it.isJsonPrimitive) defaultValue else it.asLong } ?: defaultValue
fun JsonObject?.getFloat(key: String, defaultValue: Float): Float = get(key)?.let { if(!it.isJsonPrimitive) defaultValue else it.asFloat } ?: defaultValue
fun JsonObject?.getChar(key: String, defaultValue: Char): Char = get(key)?.let { if(!it.isJsonPrimitive) defaultValue else it.asCharacter } ?: defaultValue
fun JsonObject?.getJsonObject(key: String, defaultValue: JsonObject): JsonObject = get(key)?.let { if (it.isJsonObject) it.asJsonObject else defaultValue } ?: defaultValue
fun JsonObject?.getJsonArray(key: String, defaultValue: JsonArray): JsonArray = get(key)?.let { if (it.isJsonArray) it.asJsonArray else defaultValue } ?: defaultValue

fun JsonArray.getBoolean(key: Int): Boolean? = if (key >= size()) null else get(key)?.let { if (!it.isJsonPrimitive) null else it.asBoolean }
fun JsonArray.getString(key: Int): String? = if (key >= size()) null else get(key)?.let { if (!it.isJsonPrimitive) null else it.asString }
fun JsonArray.getInt(key: Int): Int? = if (key >= size()) null else get(key)?.let { if (!it.isJsonPrimitive) null else it.asInt }
fun JsonArray.getLong(key: Int): Long? = if (key >= size()) null else get(key)?.let { if (!it.isJsonPrimitive) null else it.asLong }
fun JsonArray.getFloat(key: Int): Float? = if (key >= size()) null else get(key)?.let { if(!it.isJsonPrimitive) null else it.asFloat }
fun JsonArray.getChar(key: Int): Char? = if (key >= size()) null else get(key)?.let { if(!it.isJsonPrimitive) null else it.asCharacter }
fun JsonArray.getJsonObject(key: Int): JsonObject? = if (key >= size()) null else get(key)?.let { if (it.isJsonObject) it.asJsonObject else null }
fun JsonArray.getJsonArray(key: Int): JsonArray? = if (key >= size()) null else get(key)?.let { if (it.isJsonArray) it.asJsonArray else null }

fun String.toJsonObject(): JsonObject? = try { JsonParser().parse(this).asJsonObject } catch (ignore: Exception) { null }

operator fun JsonObject.set(key: String, value: JsonElement) = this.add(key, value)
operator fun JsonObject.set(key: String, value: Boolean) = this.addProperty(key, value)
operator fun JsonObject.set(key: String, value: String?) = this.addProperty(key, value)
operator fun JsonObject.set(key: String, value: Number) = this.addProperty(key, value)
operator fun JsonObject.set(key: String, value: Char) = this.addProperty(key, value)

operator fun Profile.set(key: String, value: JsonElement) = this.studentData.add(key, value)
operator fun Profile.set(key: String, value: Boolean) = this.studentData.addProperty(key, value)
operator fun Profile.set(key: String, value: String?) = this.studentData.addProperty(key, value)
operator fun Profile.set(key: String, value: Number) = this.studentData.addProperty(key, value)
operator fun Profile.set(key: String, value: Char) = this.studentData.addProperty(key, value)

fun JsonArray.asJsonObjectList() = this.mapNotNull { it.asJsonObject }

fun CharSequence?.isNotNullNorEmpty(): Boolean {
    return this != null && this.isNotEmpty()
}

fun <T> Collection<T>?.isNotNullNorEmpty(): Boolean {
    return this != null && this.isNotEmpty()
}

fun CharSequence?.isNotNullNorBlank(): Boolean {
    return this != null && this.isNotBlank()
}

fun currentTimeUnix() = System.currentTimeMillis() / 1000

fun Bundle?.getInt(key: String, defaultValue: Int): Int {
    return this?.getInt(key, defaultValue) ?: defaultValue
}
fun Bundle?.getLong(key: String, defaultValue: Long): Long {
    return this?.getLong(key, defaultValue) ?: defaultValue
}
fun Bundle?.getFloat(key: String, defaultValue: Float): Float {
    return this?.getFloat(key, defaultValue) ?: defaultValue
}
fun Bundle?.getString(key: String, defaultValue: String): String {
    return this?.getString(key, defaultValue) ?: defaultValue
}

fun Bundle?.getIntOrNull(key: String): Int? {
    return this?.get(key) as? Int
}
fun <T : Any> Bundle?.get(key: String): T? {
    return this?.get(key) as? T?
}

/**
 * `   The quick BROWN_fox Jumps OveR THE       LAZy-DOG.   `
 *
 * converts to
 *
 * `The Quick Brown_fox Jumps Over The Lazy-Dog.`
 */
fun String?.fixName(): String {
    return this?.fixWhiteSpaces()?.toProperCase() ?: ""
}

/**
 * `The quick BROWN_fox Jumps OveR THE       LAZy-DOG.`
 *
 * converts to
 *
 * `The Quick Brown_fox Jumps Over The       Lazy-Dog.`
 */
fun String.toProperCase(): String = changeStringCase(this)

/**
 * `John Smith` -> `Smith John`
 *
 * `JOHN SMith` -> `SMith JOHN`
 */
fun String.swapFirstLastName(): String {
    return this.split(" ").let {
        if (it.size > 1)
            it[1]+" "+it[0]
        else
            it[0]
    }
}

fun String.splitName(): Pair<String, String>? {
    return this.split(" ").let {
        if (it.size >= 2) Pair(it[0], it[1])
        else null
    }
}

fun changeStringCase(s: String): String {
    val delimiters = " '-/"
    val sb = StringBuilder()
    var capNext = true
    for (ch in s.toCharArray()) {
        var c = ch
        c = if (capNext)
            Character.toUpperCase(c)
        else
            Character.toLowerCase(c)
        sb.append(c)
        capNext = delimiters.indexOf(c) >= 0
    }
    return sb.toString()
}

fun buildFullName(firstName: String?, lastName: String?): String {
    return "$firstName $lastName".fixName()
}

fun String.getShortName(): String {
    return split(" ").let {
        if (it.size > 1)
            "${it[0]} ${it[1][0]}."
        else
            it[0]
    }
}

/**
 * "John Smith"             -> "JS"
 *
 * "JOHN SMith"             -> "JS"
 *
 * "John"                   -> "J"
 *
 * "John "                  -> "J"
 *
 * "John     Smith      "   -> "JS"
 *
 * " "                      -> ""
 *
 * "  "                     -> ""
 */
fun String?.getNameInitials(): String {
    if (this.isNullOrBlank()) return ""
    return this.toUpperCase().fixWhiteSpaces().split(" ").take(2).map { it[0] }.joinToString("")
}

fun List<String>.join(delimiter: String): String {
    return concat(delimiter).toString()
}

fun colorFromName(name: String?): Int {
    val i = (name ?: "").crc32()
    return when ((i / 10 % 16 + 1).toInt()) {
        13 -> 0xffF44336
        4  -> 0xffF50057
        2  -> 0xffD500F9
        9  -> 0xff6200EA
        5  -> 0xffFFAB00
        1  -> 0xff304FFE
        6  -> 0xff40C4FF
        14 -> 0xff26A69A
        15 -> 0xff00C853
        7  -> 0xffFFD600
        3  -> 0xffFF3D00
        8  -> 0xffDD2C00
        10 -> 0xff795548
        12 -> 0xff2979FF
        11 -> 0xffFF6D00
        else -> 0xff64DD17
    }.toInt()
}

fun colorFromCssName(name: String): Int {
    return when (name) {
        "red" -> 0xffff0000
        "green" -> 0xff008000
        "blue" -> 0xff0000ff
        "violet" -> 0xffee82ee
        "brown" -> 0xffa52a2a
        "orange" -> 0xffffa500
        "black" -> 0xff000000
        "white" -> 0xffffffff
        else -> -1L
    }.toInt()
}

fun List<Profile>.filterOutArchived() = this.filter { !it.archived }

fun Response?.getUnixDate(): Long {
    val rfcDate = this?.headers()?.get("date") ?: return currentTimeUnix()
    val pattern = "EEE, dd MMM yyyy HH:mm:ss Z"
    val format = SimpleDateFormat(pattern, Locale.ENGLISH)
    return format.parse(rfcDate).time / 1000
}

const val MINUTE = 60L
const val HOUR = 60L*MINUTE
const val DAY = 24L*HOUR
const val WEEK = 7L*DAY
const val MONTH = 30L*DAY
const val YEAR = 365L*DAY
const val MS = 1000L

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

fun String.fixWhiteSpaces() = buildString(length) {
    var wasWhiteSpace = true
    for (c in this@fixWhiteSpaces) {
        if (c.isWhitespace()) {
            if (!wasWhiteSpace) {
                append(c)
                wasWhiteSpace = true
            }
        } else {
            append(c)
            wasWhiteSpace = false
        }
    }
}.trimEnd()

fun List<Team>.getById(id: Long): Team? {
    return singleOrNull { it.id == id }
}
fun LongSparseArray<Team>.getById(id: Long): Team? {
    forEach { _, value ->
        if (value.id == id)
            return value
    }
    return null
}

operator fun MatchResult.get(group: Int): String {
    if (group >= groupValues.size)
        return ""
    return groupValues[group]
}

fun Context.setLanguage(language: String) {
    val locale = Locale(language.toLowerCase(Locale.ROOT))
    val configuration = resources.configuration
    Locale.setDefault(locale)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        configuration.setLocale(locale)
    }
    configuration.locale = locale
    resources.updateConfiguration(configuration, resources.displayMetrics)
}

/*
  Code copied from android-28/java.util.Locale.initDefault()
 */
fun initDefaultLocale() {
    run {
        // user.locale gets priority
        /*val languageTag: String? = System.getProperty("user.locale", "")
        if (languageTag.isNotNullNorEmpty()) {
            return@run Locale(languageTag)
        }*/

        // user.locale is empty
        val language: String? = System.getProperty("user.language", "pl")
        val region: String? = System.getProperty("user.region")
        val country: String?
        val variant: String?
        // for compatibility, check for old user.region property
        if (region != null) {
            // region can be of form country, country_variant, or _variant
            val i = region.indexOf('_')
            if (i >= 0) {
                country = region.substring(0, i)
                variant = region.substring(i + 1)
            } else {
                country = region
                variant = ""
            }
        } else {
            country = System.getProperty("user.country", "")
            variant = System.getProperty("user.variant", "")
        }
        return@run Locale(language)
    }.let {
        Locale.setDefault(it)
    }
}

fun String.crc16(): Int {
    var crc = 0xFFFF
    for (aBuffer in this) {
        crc = crc.ushr(8) or (crc shl 8) and 0xffff
        crc = crc xor (aBuffer.toInt() and 0xff) // byte to int, trunc sign
        crc = crc xor (crc and 0xff shr 4)
        crc = crc xor (crc shl 12 and 0xffff)
        crc = crc xor (crc and 0xFF shl 5 and 0xffff)
    }
    crc = crc and 0xffff
    return crc + 32768
}

fun String.crc32(): Long {
    val crc = CRC32()
    crc.update(toByteArray())
    return crc.value
}

fun String.hmacSHA1(password: String): String {
    val key = SecretKeySpec(password.toByteArray(), "HmacSHA1")

    val mac = Mac.getInstance("HmacSHA1").apply {
        init(key)
        update(this@hmacSHA1.toByteArray())
    }

    return encodeToString(mac.doFinal(), NO_WRAP)
}

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

fun String.sha1Hex(): String {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(toByteArray())
    return md.digest().joinToString("") { "%02x".format(it) }
}

fun String.sha256(): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(toByteArray())
    return md.digest()
}

fun RequestBody.bodyToString(): String {
    val buffer = Buffer()
    writeTo(buffer)
    return buffer.readUtf8()
}

fun Long.formatDate(format: String = "yyyy-MM-dd HH:mm:ss"): String = SimpleDateFormat(format).format(this)

fun CharSequence?.asColoredSpannable(colorInt: Int): Spannable {
    val spannable = SpannableString(this)
    spannable.setSpan(ForegroundColorSpan(colorInt), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}
fun CharSequence?.asStrikethroughSpannable(): Spannable {
    val spannable = SpannableString(this)
    spannable.setSpan(StrikethroughSpan(), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}
fun CharSequence?.asItalicSpannable(): Spannable {
    val spannable = SpannableString(this)
    spannable.setSpan(StyleSpan(Typeface.ITALIC), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}
fun CharSequence?.asBoldSpannable(): Spannable {
    val spannable = SpannableString(this)
    spannable.setSpan(StyleSpan(Typeface.BOLD), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}
fun CharSequence.asSpannable(
    vararg spans: CharacterStyle,
    substring: CharSequence? = null,
    ignoreCase: Boolean = false,
    ignoreDiacritics: Boolean = false
): Spannable {
    val spannable = SpannableString(this)
    substring?.let { substr ->
        val string = if (ignoreDiacritics)
            this.cleanDiacritics()
        else
            this
        val search = if (ignoreDiacritics)
            substr.cleanDiacritics()
        else
            substr.toString()

        var index = 0
        do {
            index = string.indexOf(
                string = search,
                startIndex = index,
                ignoreCase = ignoreCase
            )

            if (index >= 0) {
                spans.forEach {
                    spannable.setSpan(
                        CharacterStyle.wrap(it),
                        index,
                        index + substring.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                index += substring.length.coerceAtLeast(1)
            }
        } while (index >= 0)

    } ?: spans.forEach {
        spannable.setSpan(it, 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannable
}

fun CharSequence.cleanDiacritics(): String {
    val nameClean = StringBuilder()
    forEach {
        val ch = when (it) {
            'ż' -> 'z'
            'ó' -> 'o'
            'ł' -> 'l'
            'ć' -> 'c'
            'ę' -> 'e'
            'ś' -> 's'
            'ą' -> 'a'
            'ź' -> 'z'
            'ń' -> 'n'
            else -> it
        }
        nameClean.append(ch)
    }
    return nameClean.toString()
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

fun TextView.setText(@StringRes resid: Int, vararg formatArgs: Any) {
    text = context.getString(resid, *formatArgs)
}

fun MaterialAlertDialogBuilder.setTitle(@StringRes resid: Int, vararg formatArgs: Any): MaterialAlertDialogBuilder {
    setTitle(context.getString(resid, *formatArgs))
    return this
}

fun MaterialAlertDialogBuilder.setMessage(@StringRes resid: Int, vararg formatArgs: Any): MaterialAlertDialogBuilder {
    setMessage(context.getString(resid, *formatArgs))
    return this
}

fun JsonObject(vararg properties: Pair<String, Any?>): JsonObject {
    return JsonObject().apply {
        for (property in properties) {
            when (property.second) {
                is JsonElement -> add(property.first, property.second as JsonElement?)
                is String -> addProperty(property.first, property.second as String?)
                is Char -> addProperty(property.first, property.second as Char?)
                is Number -> addProperty(property.first, property.second as Number?)
                is Boolean -> addProperty(property.first, property.second as Boolean?)
            }
        }
    }
}

fun JsonObject.toBundle(): Bundle {
    return Bundle().also {
        for ((key, value) in this.entrySet()) {
            when (value) {
                is JsonObject -> it.putBundle(key, value.toBundle())
                is JsonPrimitive -> when {
                    value.isString -> it.putString(key, value.asString)
                    value.isBoolean -> it.putBoolean(key, value.asBoolean)
                    value.isNumber -> it.putInt(key, value.asInt)
                }
            }
        }
    }
}

fun JsonArray(vararg properties: Any?): JsonArray {
    return JsonArray().apply {
        for (property in properties) {
            when (property) {
                is JsonElement -> add(property as JsonElement?)
                is String -> add(property as String?)
                is Char -> add(property as Char?)
                is Number -> add(property as Number?)
                is Boolean -> add(property as Boolean?)
            }
        }
    }
}

fun Bundle(vararg properties: Pair<String, Any?>): Bundle {
    return Bundle().apply {
        for (property in properties) {
            when (property.second) {
                is String -> putString(property.first, property.second as String?)
                is Char -> putChar(property.first, property.second as Char)
                is Int -> putInt(property.first, property.second as Int)
                is Long -> putLong(property.first, property.second as Long)
                is Float -> putFloat(property.first, property.second as Float)
                is Short -> putShort(property.first, property.second as Short)
                is Double -> putDouble(property.first, property.second as Double)
                is Boolean -> putBoolean(property.first, property.second as Boolean)
                is Bundle -> putBundle(property.first, property.second as Bundle)
                is Parcelable -> putParcelable(property.first, property.second as Parcelable)
                is Array<*> -> putParcelableArray(property.first, property.second as Array<out Parcelable>)
            }
        }
    }
}
fun Intent(action: String? = null, vararg properties: Pair<String, Any?>): Intent {
    return Intent(action).putExtras(Bundle(*properties))
}
fun Intent(packageContext: Context, cls: Class<*>, vararg properties: Pair<String, Any?>): Intent {
    return Intent(packageContext, cls).putExtras(Bundle(*properties))
}

fun Bundle.toJsonObject(): JsonObject {
    val json = JsonObject()
    keySet()?.forEach { key ->
        get(key)?.let {
            when (it) {
                is String -> json.addProperty(key, it)
                is Char -> json.addProperty(key, it)
                is Int -> json.addProperty(key, it)
                is Long -> json.addProperty(key, it)
                is Float -> json.addProperty(key, it)
                is Short -> json.addProperty(key, it)
                is Double -> json.addProperty(key, it)
                is Boolean -> json.addProperty(key, it)
                is Bundle -> json.add(key, it.toJsonObject())
                else -> json.addProperty(key, it.toString())
            }
        }
    }
    return json
}
fun Intent.toJsonObject() = extras?.toJsonObject()

fun JsonArray?.isNullOrEmpty(): Boolean = (this?.size() ?: 0) == 0
fun JsonArray.isEmpty(): Boolean = this.size() == 0
operator fun JsonArray.plusAssign(o: JsonElement) = this.add(o)
operator fun JsonArray.plusAssign(o: String) = this.add(o)
operator fun JsonArray.plusAssign(o: Char) = this.add(o)
operator fun JsonArray.plusAssign(o: Number) = this.add(o)
operator fun JsonArray.plusAssign(o: Boolean) = this.add(o)

@Suppress("UNCHECKED_CAST")
inline fun <T : View> T.onClick(crossinline onClickListener: (v: T) -> Unit) {
    setOnClickListener { v: View ->
        onClickListener(v as T)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T : View> T.onLongClick(crossinline onLongClickListener: (v: T) -> Boolean) {
    setOnLongClickListener { v: View ->
        onLongClickListener(v as T)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T : CompoundButton> T.onChange(crossinline onChangeListener: (v: T, isChecked: Boolean) -> Unit) {
    setOnCheckedChangeListener { buttonView, isChecked ->
        onChangeListener(buttonView as T, isChecked)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T : MaterialButton> T.onChange(crossinline onChangeListener: (v: T, isChecked: Boolean) -> Unit) {
    clearOnCheckedChangeListeners()
    addOnCheckedChangeListener { buttonView, isChecked ->
        onChangeListener(buttonView as T, isChecked)
    }
}

fun View.attachToastHint(stringRes: Int) = onLongClick {
    Toast.makeText(it.context, stringRes, Toast.LENGTH_SHORT).show()
    true
}

fun View.detachToastHint() = setOnLongClickListener(null)

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T?) {
            observer.onChanged(t)
            removeObserver(this)
        }
    })
}

/**
 * Convert a value in dp to pixels.
 */
val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
/**
 * Convert a value in pixels to dp.
 */
val Int.px: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

@ColorInt
fun @receiver:AttrRes Int.resolveAttr(context: Context?): Int {
    val typedValue = TypedValue()
    context?.theme?.resolveAttribute(this, typedValue, true)
    return typedValue.data
}
@ColorInt
fun @receiver:ColorRes Int.resolveColor(context: Context): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.resources.getColor(this, context.theme)
    }
    else {
        context.resources.getColor(this)
    }
}
fun @receiver:DrawableRes Int.resolveDrawable(context: Context): Drawable {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.resources.getDrawable(this, context.theme)
    }
    else {
        context.resources.getDrawable(this)
    }
}

fun View.findParentById(targetId: Int): View? {
    if (id == targetId) {
        return this
    }
    val viewParent = this.parent ?: return null
    if (viewParent is View) {
        return viewParent.findParentById(targetId)
    }
    return null
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

operator fun Time?.compareTo(other: Time?): Int {
    if (this == null && other == null)
        return 0
    if (this == null)
        return -1
    if (other == null)
        return 1
    return this.compareTo(other)
}

operator fun StringBuilder.plusAssign(str: String?) {
    this.append(str)
}

fun Context.timeTill(time: Int, delimiter: String = " ", countInSeconds: Boolean = false): String {
    val parts = mutableListOf<Pair<Int, Int>>()

    val hours = time / 3600
    val minutes = (time - hours*3600) / 60
    val seconds = time - minutes*60 - hours*3600

    if (!countInSeconds) {
        var prefixAdded = false
        if (hours > 0) {
            if (!prefixAdded) parts += R.plurals.time_till_text to hours
            prefixAdded = true
            parts += R.plurals.time_till_hours to hours
        }
        if (minutes > 0) {
            if (!prefixAdded) parts += R.plurals.time_till_text to minutes
            prefixAdded = true
            parts += R.plurals.time_till_minutes to minutes
        }
        if (hours == 0 && minutes < 10) {
            if (!prefixAdded) parts += R.plurals.time_till_text to seconds
            prefixAdded = true
            parts += R.plurals.time_till_seconds to seconds
        }
    } else {
        parts += R.plurals.time_till_text to time
        parts += R.plurals.time_till_seconds to time
    }

    return parts.joinToString(delimiter) { resources.getQuantityString(it.first, it.second, it.second) }
}

fun Context.timeLeft(time: Int, delimiter: String = " ", countInSeconds: Boolean = false): String {
    val parts = mutableListOf<Pair<Int, Int>>()

    val hours = time / 3600
    val minutes = (time - hours*3600) / 60
    val seconds = time - minutes*60 - hours*3600

    if (!countInSeconds) {
        var prefixAdded = false
        if (hours > 0) {
            if (!prefixAdded) parts += R.plurals.time_left_text to hours
            prefixAdded = true
            parts += R.plurals.time_left_hours to hours
        }
        if (minutes > 0) {
            if (!prefixAdded) parts += R.plurals.time_left_text to minutes
            prefixAdded = true
            parts += R.plurals.time_left_minutes to minutes
        }
        if (hours == 0 && minutes < 10) {
            if (!prefixAdded) parts += R.plurals.time_left_text to seconds
            prefixAdded = true
            parts += R.plurals.time_left_seconds to seconds
        }
    } else {
        parts += R.plurals.time_left_text to time
        parts += R.plurals.time_left_seconds to time
    }

    return parts.joinToString(delimiter) { resources.getQuantityString(it.first, it.second, it.second) }
}

inline fun <reified T> Any?.instanceOfOrNull(): T? {
    return when (this) {
        is T -> this
        else -> null
    }
}

fun Drawable.setTintColor(color: Int): Drawable {
    colorFilter = PorterDuffColorFilter(
            color,
            PorterDuff.Mode.SRC_ATOP
    )
    return this
}

inline fun <T> List<T>.ifNotEmpty(block: (List<T>) -> Unit) {
    if (!isEmpty())
        block(this)
}

val String.firstLettersName: String
    get() {
        var nameShort = ""
        this.split(" ").forEach {
            if (it.isBlank())
                return@forEach
            nameShort += it[0].toLowerCase()
        }
        return nameShort
    }

val Throwable.stackTraceString: String
    get() {
        val sw = StringWriter()
        printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

inline fun <T> LongSparseArray<T>.filter(predicate: (T) -> Boolean): List<T> {
    val destination = ArrayList<T>()
    this.forEach { _, element -> if (predicate(element)) destination.add(element) }
    return destination
}

fun CharSequence.replace(oldValue: String, newValue: CharSequence, ignoreCase: Boolean = false): CharSequence =
        splitToSequence(oldValue, ignoreCase = ignoreCase).toList().concat(newValue)

fun Int.toColorStateList(): ColorStateList {
    val states = arrayOf(
        intArrayOf( android.R.attr.state_enabled ),
        intArrayOf(-android.R.attr.state_enabled ),
        intArrayOf(-android.R.attr.state_checked ),
        intArrayOf( android.R.attr.state_pressed )
    )

    val colors = intArrayOf(
            this,
            this,
            this,
            this
    )

    return ColorStateList(states, colors)
}

fun SpannableStringBuilder.appendText(text: CharSequence): SpannableStringBuilder {
    append(text)
    return this
}
fun SpannableStringBuilder.appendSpan(text: CharSequence, what: Any, flags: Int): SpannableStringBuilder {
    val start: Int = length
    append(text)
    setSpan(what, start, length, flags)
    return this
}

fun joinNotNullStrings(delimiter: String = "", vararg parts: String?): String {
    var first = true
    val sb = StringBuilder()
    for (part in parts) {
        if (part == null)
            continue
        if (!first)
            sb += delimiter
        first = false
        sb += part
    }
    return sb.toString()
}

fun String.notEmptyOrNull(): String? {
    return if (isEmpty())
        null
    else
        this
}

fun String.base64Encode(): String {
    return encodeToString(toByteArray(), NO_WRAP)
}
fun ByteArray.base64Encode(): String {
    return encodeToString(this, NO_WRAP)
}
fun String.base64Decode(): ByteArray {
    return Base64.decode(this, Base64.DEFAULT)
}
fun String.base64DecodeToString(): String {
    return Base64.decode(this, Base64.DEFAULT).toString(Charset.defaultCharset())
}

fun CheckBox.trigger() { isChecked = !isChecked }

fun Context.plural(@PluralsRes resId: Int, value: Int): String = resources.getQuantityString(resId, value, value)

fun Context.getNotificationTitle(type: Int): String {
    return getString(when (type) {
        Notification.TYPE_UPDATE -> R.string.notification_type_update
        Notification.TYPE_ERROR -> R.string.notification_type_error
        Notification.TYPE_TIMETABLE_CHANGED -> R.string.notification_type_timetable_change
        Notification.TYPE_TIMETABLE_LESSON_CHANGE -> R.string.notification_type_timetable_lesson_change
        Notification.TYPE_NEW_GRADE -> R.string.notification_type_new_grade
        Notification.TYPE_NEW_EVENT -> R.string.notification_type_new_event
        Notification.TYPE_NEW_HOMEWORK -> R.string.notification_type_new_homework
        Notification.TYPE_NEW_SHARED_EVENT -> R.string.notification_type_new_shared_event
        Notification.TYPE_NEW_SHARED_HOMEWORK -> R.string.notification_type_new_shared_homework
        Notification.TYPE_REMOVED_SHARED_EVENT -> R.string.notification_type_removed_shared_event
        Notification.TYPE_NEW_MESSAGE -> R.string.notification_type_new_message
        Notification.TYPE_NEW_NOTICE -> R.string.notification_type_notice
        Notification.TYPE_NEW_ATTENDANCE -> R.string.notification_type_attendance
        Notification.TYPE_SERVER_MESSAGE -> R.string.notification_type_server_message
        Notification.TYPE_LUCKY_NUMBER -> R.string.notification_type_lucky_number
        Notification.TYPE_FEEDBACK_MESSAGE -> R.string.notification_type_feedback_message
        Notification.TYPE_NEW_ANNOUNCEMENT -> R.string.notification_type_new_announcement
        Notification.TYPE_AUTO_ARCHIVING -> R.string.notification_type_auto_archiving
        Notification.TYPE_TEACHER_ABSENCE -> R.string.notification_type_new_teacher_absence
        Notification.TYPE_GENERAL -> R.string.notification_type_general
        else -> R.string.notification_type_general
    })
}

fun Cursor?.getString(columnName: String) = this?.getStringOrNull(getColumnIndex(columnName))
fun Cursor?.getInt(columnName: String) = this?.getIntOrNull(getColumnIndex(columnName))
fun Cursor?.getLong(columnName: String) = this?.getLongOrNull(getColumnIndex(columnName))

fun CharSequence.containsAll(list: List<CharSequence>, ignoreCase: Boolean = false): Boolean {
    for (i in list) {
        if (!contains(i, ignoreCase))
            return false
    }
    return true
}

inline fun RadioButton.setOnSelectedListener(crossinline listener: (buttonView: CompoundButton) -> Unit)
        = setOnCheckedChangeListener { buttonView, isChecked -> if (isChecked) listener(buttonView) }

fun Response.toErrorCode() = when (this.code()) {
    400 -> ERROR_REQUEST_HTTP_400
    401 -> ERROR_REQUEST_HTTP_401
    403 -> ERROR_REQUEST_HTTP_403
    404 -> ERROR_REQUEST_HTTP_404
    405 -> ERROR_REQUEST_HTTP_405
    410 -> ERROR_REQUEST_HTTP_410
    424 -> ERROR_REQUEST_HTTP_424
    500 -> ERROR_REQUEST_HTTP_500
    503 -> ERROR_REQUEST_HTTP_503
    else -> null
}
fun Throwable.toErrorCode() = when (this) {
    is UnknownHostException -> ERROR_REQUEST_FAILURE_HOSTNAME_NOT_FOUND
    is SSLException -> ERROR_REQUEST_FAILURE_SSL_ERROR
    is SocketTimeoutException -> ERROR_REQUEST_FAILURE_TIMEOUT
    is InterruptedIOException, is ConnectException -> ERROR_REQUEST_FAILURE_NO_INTERNET
    is SzkolnyApiException -> this.error?.toErrorCode()
    else -> null
}
fun ApiResponse.Error.toErrorCode() = when (this.code) {
    "PdoError" -> ERROR_API_PDO_ERROR
    "InvalidClient" -> ERROR_API_INVALID_CLIENT
    "InvalidArgument" -> ERROR_API_INVALID_ARGUMENT
    "InvalidSignature" -> ERROR_API_INVALID_SIGNATURE
    "MissingScopes" -> ERROR_API_MISSING_SCOPES
    "ResourceNotFound" -> ERROR_API_RESOURCE_NOT_FOUND
    "InternalServerError" -> ERROR_API_INTERNAL_SERVER_ERROR
    "PhpError" -> ERROR_API_PHP_E_ERROR
    "PhpWarning" -> ERROR_API_PHP_E_WARNING
    "PhpParse" -> ERROR_API_PHP_E_PARSE
    "PhpNotice" -> ERROR_API_PHP_E_NOTICE
    "PhpOther" -> ERROR_API_PHP_E_OTHER
    "ApiMaintenance" -> ERROR_API_MAINTENANCE
    "MissingArgument" -> ERROR_API_MISSING_ARGUMENT
    "MissingPayload" -> ERROR_API_PAYLOAD_EMPTY
    "InvalidAction" -> ERROR_API_INVALID_ACTION
    "VersionNotFound" -> ERROR_API_UPDATE_NOT_FOUND
    "InvalidDeviceIdUserCode" -> ERROR_API_INVALID_DEVICEID_USERCODE
    "InvalidPairToken" -> ERROR_API_INVALID_PAIRTOKEN
    "InvalidBrowserId" -> ERROR_API_INVALID_BROWSERID
    "InvalidDeviceId" -> ERROR_API_INVALID_DEVICEID
    "InvalidDeviceIdBrowserId" -> ERROR_API_INVALID_DEVICEID_BROWSERID
    "HelpCategoryNotFound" -> ERROR_API_HELP_CATEGORY_NOT_FOUND
    else -> ERROR_API_EXCEPTION
}
fun Throwable.toApiError(tag: String) = ApiError.fromThrowable(tag, this)

inline fun <A, B, R> ifNotNull(a: A?, b: B?, code: (A, B) -> R): R? {
    if (a != null && b != null) {
        return code(a, b)
    }
    return null
}

@kotlin.jvm.JvmName("averageOrNullOfInt")
fun Iterable<Int>.averageOrNull() = this.average().let { if (it.isNaN()) null else it }
@kotlin.jvm.JvmName("averageOrNullOfFloat")
fun Iterable<Float>.averageOrNull() = this.average().let { if (it.isNaN()) null else it }

fun String.copyToClipboard(context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText("Tekst", this)
    clipboard.setPrimaryClip(clipData)
}

fun TextView.getTextPosition(range: IntRange): Rect {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Initialize global value
    var parentTextViewRect = Rect()

    // Initialize values for the computing of clickedText position
    //val completeText = parentTextView.text as SpannableString
    val textViewLayout = this.layout

    val startOffsetOfClickedText = range.first//completeText.getSpanStart(clickedText)
    val endOffsetOfClickedText = range.last//completeText.getSpanEnd(clickedText)
    var startXCoordinatesOfClickedText = textViewLayout.getPrimaryHorizontal(startOffsetOfClickedText)
    var endXCoordinatesOfClickedText = textViewLayout.getPrimaryHorizontal(endOffsetOfClickedText)

    // Get the rectangle of the clicked text
    val currentLineStartOffset = textViewLayout.getLineForOffset(startOffsetOfClickedText)
    val currentLineEndOffset = textViewLayout.getLineForOffset(endOffsetOfClickedText)
    val keywordIsInMultiLine = currentLineStartOffset != currentLineEndOffset
    textViewLayout.getLineBounds(currentLineStartOffset, parentTextViewRect)

    // Update the rectangle position to his real position on screen
    val parentTextViewLocation = intArrayOf(0, 0)
    this.getLocationOnScreen(parentTextViewLocation)

    val parentTextViewTopAndBottomOffset = (parentTextViewLocation[1] - this.scrollY + this.compoundPaddingTop)
    parentTextViewRect.top += parentTextViewTopAndBottomOffset
    parentTextViewRect.bottom += parentTextViewTopAndBottomOffset

    // In the case of multi line text, we have to choose what rectangle take
    if (keywordIsInMultiLine) {
        val screenHeight = windowManager.defaultDisplay.height
        val dyTop = parentTextViewRect.top
        val dyBottom = screenHeight - parentTextViewRect.bottom
        val onTop = dyTop > dyBottom

        if (onTop) {
            endXCoordinatesOfClickedText = textViewLayout.getLineRight(currentLineStartOffset);
        } else {
            parentTextViewRect = Rect()
            textViewLayout.getLineBounds(currentLineEndOffset, parentTextViewRect);
            parentTextViewRect.top += parentTextViewTopAndBottomOffset;
            parentTextViewRect.bottom += parentTextViewTopAndBottomOffset;
            startXCoordinatesOfClickedText = textViewLayout.getLineLeft(currentLineEndOffset);
        }
    }

    parentTextViewRect.left += (
            parentTextViewLocation[0] +
                    startXCoordinatesOfClickedText +
                    this.compoundPaddingLeft -
                    this.scrollX
            ).toInt()
    parentTextViewRect.right = (
            parentTextViewRect.left +
                    endXCoordinatesOfClickedText -
                    startXCoordinatesOfClickedText
            ).toInt()

    return parentTextViewRect
}

inline fun ViewPager.addOnPageSelectedListener(crossinline block: (position: Int) -> Unit) = addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
    override fun onPageScrollStateChanged(state: Int) {}
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) { block(position) }
})

val SwipeRefreshLayout.onScrollListener: RecyclerView.OnScrollListener
    get() = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (recyclerView.canScrollVertically(-1))
                this@onScrollListener.isEnabled = false
            if (!recyclerView.canScrollVertically(-1) && newState == RecyclerView.SCROLL_STATE_IDLE)
                this@onScrollListener.isEnabled = true
        }
    }

operator fun <K, V> Iterable<Pair<K, V>>.get(key: K): V? {
    return firstOrNull { it.first == key }?.second
}

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

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

fun Context.getSyncInterval(interval: Int): String {
    val hours = interval / 60 / 60
    val minutes = interval / 60 % 60
    val hoursText = if (hours > 0)
        plural(R.plurals.time_till_hours, hours)
    else
        null
    val minutesText = if (minutes > 0)
        plural(R.plurals.time_till_minutes, minutes)
    else
        ""
    return hoursText?.plus(" $minutesText") ?: minutesText
}

fun Profile.getSchoolYearConstrains(): CalendarConstraints {
    return CalendarConstraints.Builder()
        .setStart(dateSemester1Start.inMillisUtc)
        .setEnd(dateYearEnd.inMillisUtc)
        .build()
}

fun CharSequence.getWordBounds(position: Int, onlyInWord: Boolean = false): Pair<Int, Int>? {
    if (length == 0)
        return null

    // only if cursor between letters
    if (onlyInWord) {
        if (position < 1)
            return null
        if (position == length)
            return null

        val charBefore = this[position - 1]
        if (!charBefore.isLetterOrDigit())
            return null
        val charAfter = this[position]
        if (!charAfter.isLetterOrDigit())
            return null
    }

    var rangeStart = substring(0 until position).indexOfLast { !it.isLetterOrDigit() }
    if (rangeStart == -1) // no whitespace, set to first index
        rangeStart = 0
    else // cut the leading whitespace
        rangeStart += 1

    var rangeEnd = substring(position).indexOfFirst { !it.isLetterOrDigit() }
    if (rangeEnd == -1) // no whitespace, set to last index
        rangeEnd = length
    else // append the substring offset
        rangeEnd += position

    if (!onlyInWord && rangeStart == rangeEnd)
        return null
    return rangeStart to rangeEnd
}

infix fun Int.hasSet(what: Int) = this and what == what
