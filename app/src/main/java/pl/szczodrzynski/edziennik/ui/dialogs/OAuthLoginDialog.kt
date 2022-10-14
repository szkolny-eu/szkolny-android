/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-14.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ui.dialogs.base.ViewDialog
import pl.szczodrzynski.edziennik.utils.Utils.d

class OAuthLoginDialog(
    activity: AppCompatActivity,
    private val authorizeUrl: String,
    private val redirectUrl: String,
    private val onSuccess: (responseUrl: String) -> Unit,
    private val onFailure: (() -> Unit)?,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : ViewDialog<WebView>(activity, onShowListener, onDismissListener) {

    override val TAG = "OAuthLoginDialog"

    override fun getTitleRes() = R.string.oauth_dialog_title
    override fun getPositiveButtonText() = R.string.close

    private var isSuccessful = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun getRootView(): WebView {
        val webView = WebView(activity)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                d(TAG, "Navigating to $url")
                if (url.startsWith(redirectUrl)) {
                    isSuccessful = true
                    onSuccess(url)
                    dismiss()
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        return webView
    }

    override suspend fun onShow() {
        dialog.window?.setLayout(MATCH_PARENT, MATCH_PARENT)
        root.minimumHeight = activity.windowManager.defaultDisplay?.height?.div(2) ?: 300.dp
        root.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        root.loadUrl(authorizeUrl)
    }

    override fun onDismiss() {
        root.stopLoading()
        if (!isSuccessful)
            onFailure?.invoke()
    }
}
