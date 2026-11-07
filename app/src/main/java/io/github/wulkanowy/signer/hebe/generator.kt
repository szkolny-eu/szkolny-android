package io.github.wulkanowy.signer.hebe

import com.migcomponents.migbase64.Base64
import eu.szkolny.x509.X509Generator
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.time.ZonedDateTime

fun generateKeyPair(): Triple<String, String, String> {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(2048)
    val keyPair = generator.generateKeyPair()
    val publicKey = keyPair.public.encoded
    val privateKey = keyPair.private.encoded

    val publicPem = Base64.encodeToString(publicKey, false)
    val privatePem = Base64.encodeToString(privateKey, false)
    val publicHash = MessageDigest.getInstance("MD5")
            .digest(publicPem.toByteArray())
            .joinToString("") { "%02x".format(it) }
    return Triple(publicPem, privatePem, publicHash)
}

fun generateCertificate(privatePem: String): Pair<String, String> {
    val keyFactory = KeyFactory.getInstance("RSA")
    val privateBytes = Base64.decode(privatePem)
    val privateSpec = PKCS8EncodedKeySpec(privateBytes)
    val privateKey = keyFactory.generatePrivate(privateSpec) as RSAPrivateCrtKey
    val publicSpec = RSAPublicKeySpec(privateKey.modulus, privateKey.publicExponent)
    val publicKey = keyFactory.generatePublic(publicSpec)
    val keyPair = KeyPair(publicKey, privateKey)

    val notBefore = ZonedDateTime.now()
    val notAfter = notBefore.plusYears(20)

    val cert = X509Generator(X509Generator.Algorithm.RSA_SHA256)
            .generate(subject = mapOf("CN" to "APP_CERTIFICATE CA Certificate"),
                    notBefore = notBefore,
                    notAfter = notAfter,
                    serialNumber = 1,
                    keyPair = keyPair
            )

    val certificatePem = Base64.encodeToString(cert, false)
    val certificateHash = MessageDigest.getInstance("SHA-1")
            .digest(cert)
            .joinToString("") { "%02x".format(it) }
    return Pair(certificatePem, certificateHash)
}
