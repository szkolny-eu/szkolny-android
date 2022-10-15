/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-15.
 */

package pl.szczodrzynski.edziennik.ui.login.oauth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.utils.Utils.d

class OAuthLoginActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "OAuthLoginActivity"
    }

    private var isSuccessful = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.oauth_dialog_title)

        val authorizeUrl = intent.getStringExtra("authorizeUrl") ?: return
        val redirectUrl = intent.getStringExtra("redirectUrl") ?: return

        val webView = WebView(this)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                d(TAG, "Navigating to $url")
                if (url.startsWith(redirectUrl)) {
                    isSuccessful = true
                    EventBus.getDefault().post(OAuthLoginResult(
                        isError = false,
                        responseUrl = url,
                    ))
                    finish()
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        setContentView(webView)

        webView.loadUrl(authorizeUrl)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isSuccessful)
            EventBus.getDefault().post(OAuthLoginResult(
                isError = false,
                responseUrl = null,
            ))
    }
}
