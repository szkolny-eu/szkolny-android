/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-15.
 */

package pl.szczodrzynski.edziennik.ui.captcha

import android.graphics.drawable.Drawable
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.RecaptchaViewBinding
import pl.szczodrzynski.edziennik.onClick
import kotlin.coroutines.CoroutineContext

class LibrusCaptchaDialog(
        val activity: AppCompatActivity,
        val onSuccess: (recaptchaCode: String) -> Unit,
        val onFailure: (() -> Unit)?,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "LibrusCaptchaDialog"
    }

    private lateinit var app: App
    private lateinit var b: RecaptchaViewBinding
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var checkboxBackground: Drawable
    private lateinit var checkboxForeground: Drawable
    private var success = false

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App
        b = RecaptchaViewBinding.inflate(activity.layoutInflater)
        dialog = MaterialAlertDialogBuilder(activity)
                .setView(b.root)
                .setNegativeButton(R.string.cancel, null)
                .setOnDismissListener {
                    if (!success)
                        onFailure?.invoke()
                    onDismissListener?.invoke(TAG)
                }
                .show()

        checkboxBackground = b.checkbox.background
        checkboxForeground = b.checkbox.foreground
        success = false

        b.root.onClick {
            b.checkbox.performClick()
        }
        b.checkbox.onClick {
            b.checkbox.background = null
            b.checkbox.foreground = null
            b.progress.visibility = View.VISIBLE
            RecaptchaDialog(
                    activity,
                    siteKey = "6Lf48moUAAAAAB9ClhdvHr46gRWR-CN31CXQPG2U",
                    referer = "https://portal.librus.pl/rodzina/login",
                    onSuccess = { recaptchaCode ->
                        b.checkbox.background = checkboxBackground
                        b.checkbox.foreground = checkboxForeground
                        b.progress.visibility = View.GONE
                        success = true
                        onSuccess(recaptchaCode)
                        dialog.dismiss()
                    },
                    onFailure = {
                        b.checkbox.background = checkboxBackground
                        b.checkbox.foreground = checkboxForeground
                        b.progress.visibility = View.GONE
                    }
            )
        }
    }}
}
