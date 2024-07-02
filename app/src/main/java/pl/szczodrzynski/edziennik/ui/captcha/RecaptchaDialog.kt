/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-15.
 */

package pl.szczodrzynski.edziennik.ui.captcha

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.*
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.RecaptchaDialogBinding
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RecaptchaDialog(
    activity: AppCompatActivity,
    private val siteKey: String,
    private val referer: String,
    private val autoRetry: Boolean = true,
    private val onSuccess: (recaptchaCode: String) -> Unit,
    private val onFailure: (() -> Unit)? = null,
    private val onServerError: (() -> Unit)? = null,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BindingDialog<RecaptchaDialogBinding>(activity, onShowListener, onDismissListener) {

    override val TAG = "RecaptchaDialog"

    override fun getTitleRes(): Int? = null
    override fun inflate(layoutInflater: LayoutInflater) =
        RecaptchaDialogBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.ok

    private val captchaUrl = "https://www.google.com/recaptcha/api/fallback?k=$siteKey"
    private var success = false
    private var code = ""
    private var payload = ""

    override suspend fun onBeforeShow(): Boolean {
        val (title, text, bitmap) = withContext(Dispatchers.Default) {
            val html = loadCaptchaHtml()
            if (html == null) {
                onServerError?.invoke()
                return@withContext null
            }
            return@withContext loadCaptchaData(html)
        } ?: run {
            onFailure?.invoke()
            return false
        }

        initViews(title, text, bitmap)
        return true
    }

    override suspend fun onShow() {
        b.image0.isChecked = false
        b.image1.isChecked = false
        b.image2.isChecked = false
        b.image3.isChecked = false
        b.image4.isChecked = false
        b.image5.isChecked = false
        b.image6.isChecked = false
        b.image7.isChecked = false
        b.image8.isChecked = false
    }

    override fun onDismiss() {
        if (!success)
            onFailure?.invoke()
    }

    private fun initViews(title: String, text: String, bitmap: Bitmap) {
        b.descTitle.text = title
        b.descText.text = text
        b.payload.setImageBitmap(bitmap)
    }

    private suspend fun loadCaptchaHtml(): String? {
        val request = Request.Builder()
            .url(captchaUrl)
            .addHeader("Referer", referer)
            .addHeader("Accept-Language", "pl")
            .build()

        return suspendCoroutine { cont ->
            app.http.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val html = response.body()?.string()
                    cont.resume(html)
                }

                override fun onFailure(call: Call, e: IOException) {
                    cont.resume(null)
                }
            })
        }
    }

    private suspend fun loadCaptchaData(html: String): Triple<String, String, Bitmap>? {
        var title = ""
        var text = ""
        "class=\"rc-imageselect-desc(?:-no-canonical)?\">(.+?) <strong>(.+?)</strong>"
            .toRegex()
            .find(html)
            ?.let {
                title = it.groupValues[1]
                text = it.groupValues[2]
            }

        code = "name=\"c\" value=\"([A-z0-9-_]+)\""
            .toRegex()
            .find(html)
            ?.let { it.groupValues[1] }
            ?: return null

        payload = "https://www.google.com/recaptcha/api2/payload?c=$code&k=$siteKey"
        val request = Request.Builder()
            .url(payload)
            .addHeader("Referer", captchaUrl)
            .addHeader("Accept-Language", "pl")
            .build()

        val bitmap = suspendCoroutine<Bitmap?> { cont ->
            app.http.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val bitmap: Bitmap? = BitmapFactory.decodeStream(response.body()?.byteStream())
                    if (bitmap == null) {
                        Toast.makeText(
                            activity,
                            "Nie udało się załadować reCAPTCHA.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    cont.resume(bitmap)
                }

                override fun onFailure(call: Call, e: IOException) {
                    cont.resume(null)
                }
            })
        } ?: return null

        return Triple(title, text, bitmap)
    }

    override suspend fun onPositiveClick(): Boolean {
        val list = mutableListOf(
            "c=$code"
        )
        if (b.image0.isChecked) list += "response=0"
        if (b.image1.isChecked) list += "response=1"
        if (b.image2.isChecked) list += "response=2"
        if (b.image3.isChecked) list += "response=3"
        if (b.image4.isChecked) list += "response=4"
        if (b.image5.isChecked) list += "response=5"
        if (b.image6.isChecked) list += "response=6"
        if (b.image7.isChecked) list += "response=7"
        if (b.image8.isChecked) list += "response=8"

        val request = Request.Builder()
            .url(captchaUrl)
            .addHeader("Referer", captchaUrl)
            .addHeader("Accept-Language", "pl")
            .addHeader("Origin", "https://www.google.com")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .post(RequestBody.create(
                MediaType.parse("application/x-www-form-urlencoded"),
                list.joinToString("&"),
            ))
            .build()

        val (code, html) = withContext(Dispatchers.Default) {
            return@withContext suspendCoroutine<Pair<String?, String?>> { cont ->
                app.http.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val html = response.body()?.string() ?: run {
                            cont.resume(null to null)
                            return
                        }
                        val match = "<textarea.+?>([A-z0-9-_]+)</textarea>".toRegex().find(html)
                        if (match == null) {
                            cont.resume(null to html)
                        } else {
                            cont.resume(match.groupValues[1] to null)
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        cont.resume(null to null)
                    }
                })
            }
        }

        when {
            code != null -> {
                success = true
                onSuccess(code)
                return DISMISS
            }
            html != null -> {
                val (title, text, bitmap) = withContext(Dispatchers.Default) {
                    return@withContext loadCaptchaData(html)
                } ?: run {
                    onFailure?.invoke()
                    return DISMISS
                }
                initViews(title, text, bitmap)
                return NO_DISMISS
            }
            else -> {
                onFailure?.invoke()
                return DISMISS
            }
        }
    }
}
