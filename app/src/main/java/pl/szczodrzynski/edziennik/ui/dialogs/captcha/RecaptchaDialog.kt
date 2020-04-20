/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-15.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.captcha

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import okhttp3.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.databinding.RecaptchaDialogBinding
import java.io.IOException
import kotlin.coroutines.CoroutineContext

class RecaptchaDialog(
        val activity: AppCompatActivity,
        val siteKey: String,
        val referer: String,
        val autoRetry: Boolean = true,
        val onSuccess: (recaptchaCode: String) -> Unit,
        val onFailure: (() -> Unit)? = null,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "RecaptchaDialog"
    }

    private lateinit var app: App
    private val b by lazy { RecaptchaDialogBinding.inflate(LayoutInflater.from(activity)) }
    private var dialog: AlertDialog? = null

    private val captchaUrl = "https://www.google.com/recaptcha/api/fallback?k=$siteKey"
    private var success = false

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private var code = ""
    private var payload = ""

    init { run {
        if (activity.isFinishing)
            return@run
        app = activity.applicationContext as App
        onShowListener?.invoke(TAG)
        success = false

        launch { initCaptcha() }
    }}

    private suspend fun initCaptcha() {
        withContext(Dispatchers.Default) {
            val request = Request.Builder()
                    .url(captchaUrl)
                    .addHeader("Referer", referer)
                    .addHeader("Accept-Language", "pl")
                    .build()
            app.http.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val html = response.body()?.string() ?: return
                    Log.d(TAG, html)
                    parseHtml(html)
                }

                override fun onFailure(call: Call, e: IOException) {

                }
            })
        }
    }

    private fun parseHtml(html: String) {
        launch {
            "class=\"rc-imageselect-desc(?:-no-canonical)?\">(.+?) <strong>(.+?)</strong>".toRegex().find(html)?.let {
                b.descTitle.text = it.groupValues[1]
                b.descText.text = it.groupValues[2]
            }
            code = "name=\"c\" value=\"([A-z0-9-_]+)\"".toRegex().find(html)?.let { it.groupValues[1] } ?: return@launch
            payload = "https://www.google.com/recaptcha/api2/payload?c=$code&k=$siteKey"
            withContext(Dispatchers.Default) {
                val request = Request.Builder()
                        .url(payload)
                        .addHeader("Referer", captchaUrl)
                        .addHeader("Accept-Language", "pl")
                        .build()
                app.http.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val bitmap: Bitmap? = BitmapFactory.decodeStream(response.body()?.byteStream())
                        Handler(activity.mainLooper).post {
                            if (bitmap == null) {
                                onFailure?.invoke()
                                Toast.makeText(activity, "Nie udało się załadować reCAPTCHA.", Toast.LENGTH_SHORT).show()
                                return@post
                            }
                            b.payload.setImageBitmap(bitmap)
                            showDialog()
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        onFailure?.invoke()
                    }
                })
            }
        }
    }

    private fun showDialog() {
        if (dialog == null) {
            dialog = MaterialAlertDialogBuilder(activity)
                    .setView(b.root)
                    .setPositiveButton("OK") { _, _ ->
                        validateAnswer()
                    }
                    .setOnDismissListener {
                        if (!success)
                            onFailure?.invoke()
                        onDismissListener?.invoke(TAG)
                    }
                    .create()
        }
        b.image0.isChecked = false
        b.image1.isChecked = false
        b.image2.isChecked = false
        b.image3.isChecked = false
        b.image4.isChecked = false
        b.image5.isChecked = false
        b.image6.isChecked = false
        b.image7.isChecked = false
        b.image8.isChecked = false
        dialog!!.show()
    }

    private fun validateAnswer() {
        launch {
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
                    .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), list.joinToString("&")))
                    .build()
            withContext(Dispatchers.Default) {
                app.http.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val html = response.body()?.string() ?: return
                        val match = "<textarea.+?>([A-z0-9-_]+)</textarea>".toRegex().find(html)
                        if (match == null) {
                            parseHtml(html)
                            return
                        }
                        Handler(activity.mainLooper).post {
                            success = true
                            onSuccess(match.groupValues[1])
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {

                    }
                })
            }
        }
    }
}
