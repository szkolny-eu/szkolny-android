/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-14.
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.interceptor

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Base64
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.sha256
import java.security.MessageDigest

object Signing {

    private external fun iLoveApple(data: ByteArray, signature: String, timestamp: Long): String

    init {
        System.loadLibrary("szkolny-signing")
    }

    var appCertificate = ""
    fun getCert(context: Context) {
        with(context) {
        try {
            val packageInfo: PackageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in packageInfo.signatures) {
                val signatureBytes = signature.toByteArray()
                val md = MessageDigest.getInstance("SHA")
                md.update(signatureBytes)
                appCertificate = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }}

    val appPassword by lazy {
        iLoveApple(
                "ThisIsOurHardWorkPleaseDoNotCopyOrSteal(c)2019.KubaSz".sha256(),
                BuildConfig.VERSION_NAME.substringBeforeLast('+'),
                BuildConfig.VERSION_CODE.toLong()
        )
    }

    /*fun provideKey(param1: String, param2: Long): ByteArray {*/
    fun pleaseStopRightNow(param1: String, param2: Long): ByteArray {
        return "$param1.MTIzNDU2Nzg5MDY8+Uq3So===.$param2".sha256()
    }
}
