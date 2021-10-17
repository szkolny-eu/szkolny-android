/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.util.Base64
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun String.crc16(): Int {
    var crc = 0xFFFF
    for (aBuffer in this) {
        crc = crc.ushr(8) or (crc shl 8) and 0xffff
        crc = crc xor (aBuffer.code and 0xff) // byte to int, trunc sign
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

    return Base64.encodeToString(mac.doFinal(), Base64.NO_WRAP)
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

fun String.base64Encode(): String {
    return Base64.encodeToString(toByteArray(), Base64.NO_WRAP)
}
fun ByteArray.base64Encode(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP)
}
fun String.base64Decode(): ByteArray {
    return Base64.decode(this, Base64.DEFAULT)
}
fun String.base64DecodeToString(): String {
    return Base64.decode(this, Base64.DEFAULT).toString(Charset.defaultCharset())
}
