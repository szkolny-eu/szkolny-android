package pl.szczodrzynski.edziennik

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.*
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.util.Base64.NO_WRAP
import android.util.Base64.encodeToString
import android.util.LongSparseArray
import android.util.SparseArray
import android.util.TypedValue
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.*
import androidx.core.app.ActivityCompat
import androidx.core.util.forEach
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import im.wangchao.mhttp.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import okio.Buffer
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.navlib.getColorFromRes
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


fun List<Teacher>.byId(id: Long) = firstOrNull { it.id == id }
fun List<Teacher>.byNameFirstLast(nameFirstLast: String) = firstOrNull { it.name + " " + it.surname == nameFirstLast }
fun List<Teacher>.byNameLastFirst(nameLastFirst: String) = firstOrNull { it.surname + " " + it.name == nameLastFirst }
fun List<Teacher>.byNameFDotLast(nameFDotLast: String) = firstOrNull { it.name + "." + it.surname == nameFDotLast }
fun List<Teacher>.byNameFDotSpaceLast(nameFDotSpaceLast: String) = firstOrNull { it.name + ". " + it.surname == nameFDotSpaceLast }

fun JsonObject?.get(key: String): JsonElement? = this?.get(key)

fun JsonObject?.getBoolean(key: String): Boolean? = get(key)?.let { if (it.isJsonNull) null else it.asBoolean }
fun JsonObject?.getString(key: String): String? = get(key)?.let { if (it.isJsonNull) null else it.asString }
fun JsonObject?.getInt(key: String): Int? = get(key)?.let { if (it.isJsonNull) null else it.asInt }
fun JsonObject?.getLong(key: String): Long? = get(key)?.let { if (it.isJsonNull) null else it.asLong }
fun JsonObject?.getFloat(key: String): Float? = get(key)?.let { if(it.isJsonNull) null else it.asFloat }
fun JsonObject?.getJsonObject(key: String): JsonObject? = get(key)?.let { if (it.isJsonNull) null else it.asJsonObject }
fun JsonObject?.getJsonArray(key: String): JsonArray? = get(key)?.let { if (it.isJsonNull) null else it.asJsonArray }

fun JsonObject?.getBoolean(key: String, defaultValue: Boolean): Boolean = get(key)?.let { if (it.isJsonNull) defaultValue else it.asBoolean } ?: defaultValue
fun JsonObject?.getString(key: String, defaultValue: String): String = get(key)?.let { if (it.isJsonNull) defaultValue else it.asString } ?: defaultValue
fun JsonObject?.getInt(key: String, defaultValue: Int): Int = get(key)?.let { if (it.isJsonNull) defaultValue else it.asInt } ?: defaultValue
fun JsonObject?.getLong(key: String, defaultValue: Long): Long = get(key)?.let { if (it.isJsonNull) defaultValue else it.asLong } ?: defaultValue
fun JsonObject?.getFloat(key: String, defaultValue: Float): Float = get(key)?.let { if(it.isJsonNull) defaultValue else it.asFloat } ?: defaultValue
fun JsonObject?.getJsonObject(key: String, defaultValue: JsonObject): JsonObject = get(key)?.let { if (it.isJsonNull) defaultValue else it.asJsonObject } ?: defaultValue
fun JsonObject?.getJsonArray(key: String, defaultValue: JsonArray): JsonArray = get(key)?.let { if (it.isJsonNull) defaultValue else it.asJsonArray } ?: defaultValue

fun JsonArray?.asJsonObjectList() = this?.map { it.asJsonObject }

fun CharSequence?.isNotNullNorEmpty(): Boolean {
    return this != null && this.isNotEmpty()
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

fun String.fixName(): String {
    return this.fixWhiteSpaces().toProperCase()
}

fun String.toProperCase(): String = changeStringCase(this)

fun String.swapFirstLastName(): String {
    return this.split(" ").let {
        if (it.size > 1)
            it[1]+" "+it[0]
        else
            it[0]
    }
}

fun String.getFirstLastName(): Pair<String, String>? {
    return this.split(" ").let {
        if (it.size >= 2) Pair(it[0], it[1])
        else null
    }
}

fun String.getLastFirstName() = this.getFirstLastName()

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

fun List<String>.join(delimiter: String): String {
    return this.joinToString(delimiter)
}

fun colorFromName(context: Context, name: String?): Int {
    var crc = (name ?: "").crc16()
    crc = (crc and 0xff) or (crc shr 8)
    crc %= 16
    val color = when (crc) {
        13 -> R.color.md_red_500
        4  -> R.color.md_pink_A400
        2  -> R.color.md_purple_A400
        9  -> R.color.md_deep_purple_A700
        5  -> R.color.md_indigo_500
        1  -> R.color.md_indigo_A700
        6  -> R.color.md_cyan_A200
        14 -> R.color.md_teal_400
        15 -> R.color.md_green_500
        7  -> R.color.md_yellow_A700
        3  -> R.color.md_deep_orange_A400
        8  -> R.color.md_deep_orange_A700
        10 -> R.color.md_brown_500
        12 -> R.color.md_grey_400
        11 -> R.color.md_blue_grey_400
        else -> R.color.md_light_green_A700
    }
    return context.getColorFromRes(color)
}

fun MutableList<Profile>.filterOutArchived(): MutableList<Profile> {
    this.removeAll { it.archived }
    return this
}

fun Activity.isStoragePermissionGranted(): Boolean {
    return if (Build.VERSION.SDK_INT >= 23) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            false
        }
    } else {
        true
    }
}

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

fun <T> SparseArray<T>.values(): List<T> {
    val result = mutableListOf<T>()
    forEach { _, value ->
        result += value
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

fun Activity.setLanguage(language: String) {
    val locale = Locale(language.toLowerCase(Locale.ROOT))
    val configuration = resources.configuration
    Locale.setDefault(locale)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        configuration.setLocale(locale)
    }
    configuration.locale = locale
    resources.updateConfiguration(configuration, resources.displayMetrics)
    baseContext.resources.updateConfiguration(configuration, baseContext.resources.displayMetrics)
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

/**
 * Returns a new read-only list only of those given elements, that are not empty.
 * Applies for CharSequence and descendants.
 */
fun <T : CharSequence> listOfNotEmpty(vararg elements: T): List<T> = elements.filterNot { it.isEmpty() }

fun List<CharSequence?>.concat(delimiter: String? = null): CharSequence {
    if (this.isEmpty()) {
        return ""
    }

    if (this.size == 1) {
        return this[0] ?: ""
    }

    var spanned = false
    for (piece in this) {
        if (piece is Spanned) {
            spanned = true
            break
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

fun JsonArray?.isNullOrEmpty(): Boolean = (this?.size() ?: 0) == 0
fun JsonArray.isEmpty(): Boolean = this.size() == 0

@Suppress("UNCHECKED_CAST")
inline fun <T : View> T.onClick(crossinline onClickListener: (v: T) -> Unit) {
    setOnClickListener { v: View ->
        onClickListener(v as T)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T : CompoundButton> T.onChange(crossinline onChangeListener: (v: T, isChecked: Boolean) -> Unit) {
    setOnCheckedChangeListener { buttonView, isChecked ->
        onChangeListener(buttonView as T, isChecked)
    }
}

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

fun CoroutineScope.startCoroutineTimer(delayMillis: Long = 0, repeatMillis: Long = 0, action: () -> Unit) = launch {
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

fun Context.timeTill(time: Int, delimiter: String = " "): String {
    val parts = mutableListOf<Pair<Int, Int>>()

    val hours = time / 3600
    val minutes = (time - hours*3600) / 60
    val seconds = time - minutes*60 - hours*3600

    var prefixAdded = false
    if (hours > 0) {
        if (!prefixAdded) parts += R.plurals.time_till_text to hours; prefixAdded = true
        parts += R.plurals.time_till_hours to hours
    }
    if (minutes > 0) {
        if (!prefixAdded) parts += R.plurals.time_till_text to minutes; prefixAdded = true
        parts += R.plurals.time_till_minutes to minutes
    }
    if (hours == 0 && minutes < 10) {
        if (!prefixAdded) parts += R.plurals.time_till_text to seconds; prefixAdded = true
        parts += R.plurals.time_till_seconds to seconds
    }

    return parts.joinToString(delimiter) { resources.getQuantityString(it.first, it.second, it.second) }
}

fun Context.timeLeft(time: Int, delimiter: String = " "): String {
    val parts = mutableListOf<Pair<Int, Int>>()

    val hours = time / 3600
    val minutes = (time - hours*3600) / 60
    val seconds = time - minutes*60 - hours*3600

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
