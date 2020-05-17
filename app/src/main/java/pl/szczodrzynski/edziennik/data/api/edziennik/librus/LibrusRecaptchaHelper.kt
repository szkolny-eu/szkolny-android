/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-8.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus

import android.content.Context
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.startCoroutineTimer
import kotlin.coroutines.CoroutineContext

class LibrusRecaptchaHelper(
        val context: Context,
        url: String,
        html: String,
        val onSuccess: (url: String) -> Unit,
        val onTimeout: () -> Unit
) : CoroutineScope {
    companion object {
        private const val TAG = "LibrusRecaptchaHelper"
    }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    private val webView by lazy {
        WebView(context).also {
            it.settings.javaScriptEnabled = true
            it.webViewClient = WebViewClient()
        }
    }

    private var timeout: Job? = null

    inner class WebViewClient : android.webkit.WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            timeout?.cancel()
            onSuccess(url)
            return true
        }
    }

    init {
        launch(Dispatchers.Main) {
            webView.loadDataWithBaseURL(url, html, "text/html", "UTF-8", null)
        }
        timeout = startCoroutineTimer(delayMillis = 10000L) {
            onTimeout()
        }
    }
}
