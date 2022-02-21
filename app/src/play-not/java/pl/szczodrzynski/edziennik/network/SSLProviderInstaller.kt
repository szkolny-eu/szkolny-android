/*
 * Copyright (c) Kuba Szczodrzyński 2022-2-21.
 */

package pl.szczodrzynski.edziennik.network

import android.content.Context
import eu.szkolny.sslprovider.SSLProvider
import eu.szkolny.sslprovider.enableSupportedTls
import okhttp3.OkHttpClient
import timber.log.Timber

object SSLProviderInstaller {

    fun install(applicationContext: Context, rebuildCallback: () -> Unit) {
        SSLProvider.install(
            applicationContext,
            downloadIfNeeded = true,
            supportTls13 = false,
            onFinish = {
                rebuildCallback()
            },
            onError = {
                Timber.e("Failed to install SSLProvider: $it")
                it.printStackTrace()
            }
        )
    }

    fun enableSupportedTls(builder: OkHttpClient.Builder, enableCleartext: Boolean = true) {
        builder.enableSupportedTls(enableCleartext)
    }
}
