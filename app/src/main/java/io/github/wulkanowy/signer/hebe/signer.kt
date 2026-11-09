/*
 * Vendored from https://github.com/wulkanowy/uonet-request-signer
 * (hebe-jvm, commit a99ca50a31), because the JitPack artifact
 * com.github.wulkanowy.uonet-request-signer:hebe-jvm:a99ca50a31
 * is no longer available.
 *
 * Only the functions used by the app are included.
 * MigBase64 was replaced with android.util.Base64.
 *
 * MIT License
 *
 * Copyright (c) 2019 Wulkanowy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.wulkanowy.signer.hebe

import android.util.Base64
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private fun encodeBase64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
private fun decodeBase64(text: String) = Base64.decode(text, Base64.DEFAULT)

private fun getDigest(body: String?): String? {
    if (body == null) return null
    return encodeBase64(MessageDigest.getInstance("SHA-256").digest(body.toByteArray()))
}

private fun getSignatureValue(values: String, privatePem: String): String {
    val keyFactory = KeyFactory.getInstance("RSA")
    val privateBytes = decodeBase64(privatePem)
    val privateSpec = PKCS8EncodedKeySpec(privateBytes)
    val privateKey = keyFactory.generatePrivate(privateSpec)

    val signature = Signature.getInstance("SHA256withRSA")
    signature.initSign(privateKey)
    signature.update(values.toByteArray())

    return encodeBase64(signature.sign())
}

private fun getEncodedPath(path: String): String {
    val url = ("(api/mobile/.+)".toRegex().find(path))
        ?: throw IllegalArgumentException("The URL does not seem correct (does not match `(api/mobile/.+)` regex)")

    return URLEncoder.encode(url.groupValues[0], "UTF-8").orEmpty().lowercase()
}

private fun getHeaders(digest: String?, canonicalUrl: String, timestamp: ZonedDateTime): MutableMap<String, String> {
    val headers = mutableMapOf<String, String>()
    headers["vCanonicalUrl"] = canonicalUrl
    if (digest != null) headers["Digest"] = digest
    headers["vDate"] = timestamp.format(DateTimeFormatter.RFC_1123_DATE_TIME)
    return headers
}

fun getSignatureHeaders(
    keyId: String,
    privatePem: String,
    body: String?,
    requestPath: String,
    timestamp: ZonedDateTime
): Map<String, String> {
    val canonicalUrl = getEncodedPath(requestPath)
    val digest = getDigest(body)
    val headers = getHeaders(digest, canonicalUrl, timestamp.withZoneSameInstant(ZoneId.of("GMT")))
    val headerNames = headers.keys.joinToString(" ")
    val headerValues = headers.values.joinToString("")
    val signatureValue = getSignatureValue(headerValues, privatePem)

    if (body != null) headers["Digest"] = "SHA-256=${digest}"
    headers["Signature"] = """keyId="$keyId,headers="$headerNames",algorithm="sha256",signature=Base64(sha256withrsa($signatureValue))"""

    return headers
}

fun generateKeyPair(): Triple<String, String, String> {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(2048)
    val keyPair = generator.generateKeyPair()
    val publicKey = keyPair.public.encoded
    val privateKey = keyPair.private.encoded

    val publicPem = encodeBase64(publicKey)
    val privatePem = encodeBase64(privateKey)
    val publicHash = MessageDigest.getInstance("MD5")
        .digest(publicPem.toByteArray())
        .joinToString("") { "%02x".format(it) }
    return Triple(publicPem, privatePem, publicHash)
}
