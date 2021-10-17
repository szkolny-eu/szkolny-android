/*
 * Copyright (c) Kuba Szczodrzyński 2020-10-18.
 */

package pl.szczodrzynski.edziennik.ui.login

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.md5
import kotlin.coroutines.CoroutineContext

class LoginEggsFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginEggsFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var view: ViewGroup
    private lateinit var webView: WebView
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        webView = WebView(activity)
        view = FrameLayout(activity)
        view.addView(webView)
        return view
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return

        if (!LoginChooserFragment.isRotated) {
            nav.navigateUp()
            return
        }

        val anim = RotateAnimation(
                180f,
                0f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
        )
        anim.interpolator = AccelerateDecelerateInterpolator()
        anim.duration = 10
        anim.fillAfter = true
        activity.getRootView().startAnimation(anim)

        webView.apply {
            settings.apply {
                javaScriptEnabled = true
            }
            addJavascriptInterface(object : Any() {
                @Suppress("NAME_SHADOWING")
                @JavascriptInterface
                fun getPrize() {
                    val anim = RotateAnimation(
                            0f,
                            180f,
                            Animation.RELATIVE_TO_SELF,
                            0.5f,
                            Animation.RELATIVE_TO_SELF,
                            0.5f
                    )
                    anim.interpolator = AccelerateDecelerateInterpolator()
                    anim.duration = 10
                    anim.fillAfter = true
                    activity.runOnUiThread {
                        activity.getRootView().startAnimation(anim)
                        nav.navigate(R.id.loginPrizeFragment, null, activity.navOptions)
                    }
                }
            }, "EggInterface")
            loadUrl("https://szkolny.eu/game/runner.html")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        val deviceId = app.deviceId.md5()
                        val version = BuildConfig.VERSION_NAME
                        val js = """initPage("$deviceId", true, "$version");"""
                        webView.evaluateJavascript(js) {}
                    }
                }
            }
        }
    }
}
