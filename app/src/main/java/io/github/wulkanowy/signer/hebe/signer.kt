package io.github.wulkanowy.signer.hebe

import com.migcomponents.migbase64.Base64
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.security.MessageDigest.getInstance as createSign

private fun getDigest(body: String?): String? {
    if (body == null) return null
    return Base64.encodeToString(createSign("SHA-256").digest(body.toByteArray()), false)
}

private fun getSignatureValue(values: String, privatePem: String): String {
    val keyFactory = KeyFactory.getInstance("RSA")
    val privateBytes = Base64.decode(privatePem)
    val privateSpec = PKCS8EncodedKeySpec(privateBytes)
    val privateKey = keyFactory.generatePrivate(privateSpec)

    val signature = Signature.getInstance("SHA256withRSA")
    signature.initSign(privateKey)
    signature.update(values.toByteArray())

    return Base64.encodeToString(signature.sign(), false)
}

private fun getEncodedPath(path: String): String {
    val url = ("(api/mobile/.+)".toRegex().find(path))
        ?: throw IllegalArgumentException("The URL does not seem correct (does not match `(api/mobile/.+)` regex)")

    return URLEncoder.encode(url.groupValues[0], "UTF-8").orEmpty().toLowerCase()
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
