/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-15.
 */

package pl.szczodrzynski.edziennik.ui.captcha

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.RecaptchaViewBinding
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ui.dialogs.base.BindingDialog

class LibrusCaptchaDialog(
    activity: AppCompatActivity,
    private val onSuccess: (recaptchaCode: String) -> Unit,
    private val onFailure: (() -> Unit)?,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BindingDialog<RecaptchaViewBinding>(activity, onShowListener, onDismissListener) {
    override val TAG = "LibrusCaptchaDialog"

    override fun getTitleRes(): Int? = null
    override fun inflate(layoutInflater: LayoutInflater) =
        RecaptchaViewBinding.inflate(layoutInflater)

    override fun getNegativeButtonText() = R.string.cancel

    private lateinit var checkboxBackground: Drawable
    private lateinit var checkboxForeground: Drawable
    private var success = false

    override suspend fun onShow() {
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
            ).show()
        }
    }

    override fun onDismiss() {
        if (!success)
            onFailure?.invoke()
    }
}
