/*
 * Copyright (c) Kuba Szczodrzyński 2023-3-24.
 */

package pl.szczodrzynski.edziennik.ui.login.recaptcha

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.SYSTEM_USER_AGENT
import pl.szczodrzynski.edziennik.ext.app
import pl.szczodrzynski.edziennik.ext.isNightMode
import java.nio.charset.Charset

class RecaptchaActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "RecaptchaActivity"

        private const val CODE = """
            PCFET0NUWVBFIGh0bWw+PGh0bWw+PGhlYWQ+PHNjcmlwdCBzcmM9Imh0dHBzOi8vd3d3Lmdvb2ds
            ZS5jb20vcmVjYXB0Y2hhL2FwaS5qcz9vbmxvYWQ9cmVhZHkmcmVuZGVyPWV4cGxpY2l0Ij48L3Nj
            cmlwdD48L2hlYWQ+PGJvZHk+PGJyPjxkaXYgaWQ9ImdyIiBzdHlsZT0icG9zaXRpb246YWJzb2x1
            dGU7dG9wOjUwJTt0cmFuc2Zvcm06dHJhbnNsYXRlKDAsLTUwJSk7Ij48L2Rpdj48YnI+PHNjcmlw
            dD5mdW5jdGlvbiByZWFkeSgpe2dyZWNhcHRjaGEucmVuZGVyKCJnciIse3NpdGVrZXk6IlNJVEVL
            RVkiLHRoZW1lOiJUSEVNRSIsY2FsbGJhY2s6ZnVuY3Rpb24oZSl7d2luZG93LmlmLmNhbGxiYWNr
            KGUpO30sImV4cGlyZWQtY2FsbGJhY2siOndpbmRvdy5pZi5leHBpcmVkQ2FsbGJhY2ssImVycm9y
            LWNhbGxiYWNrIjp3aW5kb3cuaWYuZXJyb3JDYWxsYmFja30pO308L3NjcmlwdD48L2JvZHk+PC9o
            dG1sPg==
        """
    }

    private var isSuccessful = false
    private lateinit var jsInterface: CaptchaCallbackInterface

    interface CaptchaCallbackInterface {
        @JavascriptInterface
        fun callback(recaptchaResponse: String)

        @JavascriptInterface
        fun expiredCallback()

        @JavascriptInterface
        fun errorCallback()
    }

    @SuppressLint("AddJavascriptInterface", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.recaptcha_dialog_title)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        val siteKey = intent.getStringExtra("siteKey") ?: return
        val referer = intent.getStringExtra("referer") ?: return
        val userAgent = intent.getStringExtra("userAgent") ?: SYSTEM_USER_AGENT

        val htmlContent = Base64.decode(CODE, Base64.DEFAULT)
            .toString(Charset.defaultCharset())
            .replace("THEME", if (this.isNightMode) "dark" else "light")
            .replace("SITEKEY", siteKey)

        jsInterface = object : CaptchaCallbackInterface {
            @JavascriptInterface
            override fun callback(recaptchaResponse: String) {
                isSuccessful = true
                EventBus.getDefault().post(
                    RecaptchaResult(
                        isError = false,
                        code = recaptchaResponse,
                    )
                )
                finish()
            }

            @JavascriptInterface
            override fun expiredCallback() {
                isSuccessful = false
            }

            @JavascriptInterface
            override fun errorCallback() {
                isSuccessful = false
                EventBus.getDefault().post(
                    RecaptchaResult(
                        isError = true,
                        code = null,
                    )
                )
                finish()
            }
        }

        val webView = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.userAgentString = userAgent
            addJavascriptInterface(jsInterface, "if")
            loadDataWithBaseURL(
                referer,
                htmlContent,
                "text/html",
                "UTF-8",
                null,
            )
            // setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        setContentView(webView)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isSuccessful)
            EventBus.getDefault().post(
                RecaptchaResult(
                    isError = false,
                    code = null,
                )
            )
    }
}
