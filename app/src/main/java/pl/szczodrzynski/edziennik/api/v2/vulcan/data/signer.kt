/*
 * Copyright (c) Wulkanowy kiedyś tam.
 */

package pl.szczodrzynski.edziennik.api.v2.vulcan.data

import android.util.Base64
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

fun signContent(password: String, certificate: String?, content: String): String {
    val keystore = KeyStore.getInstance("pkcs12").apply {
        load(ByteArrayInputStream(Base64.decode(certificate, Base64.DEFAULT)), password.toCharArray())
    }
    val signature = Signature.getInstance("SHA1WithRSA").apply {
        initSign(keystore.getKey("LoginCert", password.toCharArray()) as PrivateKey)
        update(content.toByteArray())
    }
    return Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
}

fun signContent(privateKey: String, content: String): String {
    val key = PKCS8EncodedKeySpec(Base64.decode(privateKey, Base64.DEFAULT)).let {
        KeyFactory.getInstance("RSA").generatePrivate(it)
    }
    val signature = Signature.getInstance("SHA1WithRSA").apply {
        initSign(key)
        update(content.toByteArray())
    }
    return Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
}

fun getPrivateKeyFromCert(password: String, certificate: String): String {
    val keystore = KeyStore.getInstance("pkcs12").apply {
        load(ByteArrayInputStream(Base64.decode(certificate, Base64.DEFAULT)), password.toCharArray())
    }
    val keyFactory = KeyFactory.getInstance("RSA")
    val keySpec = keyFactory.getKeySpec(
            keystore.getKey("LoginCert", password.toCharArray()) as PrivateKey,
            PKCS8EncodedKeySpec::class.java
    )
    return Base64.encodeToString(keySpec.encoded, Base64.NO_WRAP)
}