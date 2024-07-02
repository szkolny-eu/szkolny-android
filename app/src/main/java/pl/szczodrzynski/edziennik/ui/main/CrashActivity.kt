package pl.szczodrzynski.edziennik.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.ERROR_APP_CRASH
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.api.szkolny.request.ErrorReportRequest
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ext.resolveColor
import pl.szczodrzynski.edziennik.utils.html.BetterHtml
import kotlin.coroutines.CoroutineContext

/*
 * Copyright 2014-2017 Eduard Ereza Martínez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class CrashActivity : AppCompatActivity(), CoroutineScope {
    companion object {
        const val TAG = "CrashActivity"
    }

    private val app by lazy { application as App }
    private val api by lazy { SzkolnyApi(app) }

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.uiManager.applyTheme(this)
        setContentView(R.layout.activity_crash)
        val config = CustomActivityOnCrash.getConfigFromIntent(intent)
        if (config == null) { //This should never happen - Just finish the activity to avoid a recursive crash.
            finish()
            return
        }

        //Close/restart button logic:
        //If a class if set, use restart.
        //Else, use close and just finish the app.
        //It is recommended that you follow this logic if implementing a custom error activity.
        val restartButton = findViewById<Button>(R.id.crash_restart_btn)
        restartButton.setOnClickListener { CustomActivityOnCrash.restartApplication(this@CrashActivity, config) }

        val reportButton = findViewById<Button>(R.id.crash_report_btn)
        reportButton.setOnClickListener {
            launch {
                api.runCatching({
                    withContext(Dispatchers.Default) {
                        errorReport(listOf(getReportableError(intent)))
                    }
                }, {
                    Toast.makeText(app, getString(R.string.crash_report_cannot_send) + it, Toast.LENGTH_LONG).show()
                }) ?: return@launch

                Toast.makeText(app, getString(R.string.crash_report_sent), Toast.LENGTH_SHORT).show()
                reportButton.isEnabled = false
                reportButton.setTextColor(android.R.color.darker_gray.resolveColor(this@CrashActivity))
            }
        }

        val moreInfoButton = findViewById<Button>(R.id.crash_details_btn)
        moreInfoButton.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.attr.materialAlertDialogMonospaceTheme.resolveAttr(this))
                .setTitle(R.string.crash_details)
                .setMessage(BetterHtml.fromHtml(context = null, getErrorString(intent, false)))
                .setPositiveButton(R.string.close, null)
                .setNeutralButton(R.string.copy_to_clipboard) { _, _ -> copyErrorToClipboard() }
                .show()
        }

        val errorInformation = CustomActivityOnCrash.getAllErrorDetailsFromIntent(this@CrashActivity, intent)

        if (errorInformation.contains("MANUAL CRASH")) {
            findViewById<View>(R.id.crash_notice).visibility = View.GONE
            findViewById<View>(R.id.crash_report_btn).visibility = View.GONE
            findViewById<View>(R.id.crash_feature).visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.crash_notice).visibility = View.VISIBLE
            findViewById<View>(R.id.crash_report_btn).visibility = View.VISIBLE
            findViewById<View>(R.id.crash_feature).visibility = View.GONE
        }
    }

    private fun getErrorString(intent: Intent, plain: Boolean): String {
        var contentPlain = "Crash report:\n\n" + CustomActivityOnCrash.getStackTraceFromIntent(intent)
        var content = "<small>$contentPlain</small>"
        val packageName = packageName.replace(".debug", "")
        content = content.replace(packageName.toRegex(), "<font color='#4caf50'>$packageName</font>")
        content = content.replace("\n".toRegex(), "<br>")
        contentPlain += "\n" + Build.MANUFACTURER + "\n" + Build.BRAND + "\n" + Build.MODEL + "\n" + Build.DEVICE + "\n"
        if (!app.profile.canShare) {
            contentPlain += "U: " + app.profile.userCode + "\nS: " + app.profile.studentNameLong + "\n"
        }
        contentPlain += BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE
        return if (plain) contentPlain else content
    }

    private fun getReportableError(intent: Intent): ErrorReportRequest.Error {
        val content = CustomActivityOnCrash.getStackTraceFromIntent(intent)
        val errorCode: Int = ERROR_APP_CRASH

        val errorText = app.resources.getIdentifier("error_$errorCode", "string", app.packageName).let {
            if (it != 0) getString(it) else "?"
        }
        val errorReason = app.resources.getIdentifier("error_" + errorCode + "_reason", "string", app.packageName).let {
            if (it != 0) getString(it) else "?"
        }
        return ErrorReportRequest.Error(
                System.currentTimeMillis(),
                TAG,
                errorCode,
                errorText,
                errorReason,
                content,
                null,
                null,
                null,
                true
        )
    }

    private fun copyErrorToClipboard() {
        val errorInformation = CustomActivityOnCrash.getAllErrorDetailsFromIntent(this@CrashActivity, intent)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.apply {
            val clip = ClipData.newPlainText(getString(R.string.customactivityoncrash_error_activity_error_details_clipboard_label), errorInformation)
            setPrimaryClip(clip)
            Toast.makeText(this@CrashActivity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
    }
}
