package pl.szczodrzynski.edziennik.ui.modules.login

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.LIBRUS_USER_AGENT
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.Utils.hexFromColorInt
import java.nio.charset.Charset

class LoginLibrusCaptchaActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LoginLibrusCaptchaActivity"
    }

    private lateinit var webView: WebView
    private lateinit var dialog: AlertDialog
    private lateinit var jsInterface: CaptchaCallbackInterface

    @SuppressLint("AddJavascriptInterface", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(Themes.appThemeNoDisplay)
        setFinishOnTouchOutside(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        val base64Content = """
PCFET0NUWVBFIGh0bWw+PGh0bWw+PGhlYWQ+PHNjcmlwdCBzcmM9Imh0dHBzOi8vd3d3Lmdvb2ds
ZS5jb20vcmVjYXB0Y2hhL2FwaS5qcz9vbmxvYWQ9cmVhZHkmcmVuZGVyPWV4cGxpY2l0Ij48L3Nj
cmlwdD48L2hlYWQ+PGJvZHk+PGJyPjxjZW50ZXIgaWQ9ImdyIj48L2NlbnRlcj48YnI+PHNjcmlw
dD5mdW5jdGlvbiByZWFkeSgpe2dyZWNhcHRjaGEucmVuZGVyKCdncicse3NpdGVrZXk6JzZMZjQ4
bW9VQUFBQUFCOUNsaGR2SHI0NmdSV1ItQ04zMUNYUVBHMlUnLHRoZW1lOidUSEVNRScsY2FsbGJh
Y2s6ZnVuY3Rpb24oZSl7d2luZG93LmlmLmNhbGxiYWNrKGUpO30sImV4cGlyZWQtY2FsbGJhY2si
OmZ1bmN0aW9uKCl7d2luZG93LmlmLmV4cGlyZWRDYWxsYmFjayhlKTt9LCJlcnJvci1jYWxsYmFj
ayI6ZnVuY3Rpb24oKXt3aW5kb3cuaWYuZXJyb3JDYWxsYmFjayhlKTt9fSk7fTwvc2NyaXB0Pjwv
Ym9keT48L2h0bWw+"""

        val backgroundColor = if (Themes.isDark) 0x424242 else 0xffffff
        val backgroundColorString = hexFromColorInt(backgroundColor)

        val htmlContent = Base64.decode(base64Content, Base64.DEFAULT)
                .toString(Charset.defaultCharset())
                .replace("COLOR", backgroundColorString, true)
                .replace("THEME", if (Themes.isDark) "dark" else "light")

        jsInterface = object : CaptchaCallbackInterface {
            @JavascriptInterface
            override fun callback(recaptchaResponse: String) {
                MaterialDialog.Builder(this@LoginLibrusCaptchaActivity)
                        .title("Captcha checked")
                        .content("Response: $recaptchaResponse")
                        .positiveText("OK")
                        .show()
            }

            @JavascriptInterface
            override fun expiredCallback() {
                MaterialDialog.Builder(this@LoginLibrusCaptchaActivity)
                        .title("Captcha expired")
                        .content("Captcha expired")
                        .positiveText("OK")
                        .show()
            }

            @JavascriptInterface
            override fun errorCallback() {
                MaterialDialog.Builder(this@LoginLibrusCaptchaActivity)
                        .title("Captcha error")
                        .content("Captcha error")
                        .positiveText("OK")
                        .show()
            }
        }

        webView = WebView(this).apply {
            //setBackgroundColor((backgroundColor.toLong() or 0xff000000).toInt())
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.userAgentString = LIBRUS_USER_AGENT
            addJavascriptInterface(jsInterface, "if")
            loadDataWithBaseURL("https://portal.librus.pl/rodzina/login/", htmlContent, "text/html", "UTF-8", null)
            setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
        }

        dialog = MaterialAlertDialogBuilder(this)
                .setTitle(R.string.login_librus_captcha_title)
                .setView(webView)
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .setCancelable(false)
                .show()
    }

    interface CaptchaCallbackInterface {
        @JavascriptInterface
        fun callback(recaptchaResponse: String)
        @JavascriptInterface
        fun expiredCallback()
        @JavascriptInterface
        fun errorCallback()
    }
}
