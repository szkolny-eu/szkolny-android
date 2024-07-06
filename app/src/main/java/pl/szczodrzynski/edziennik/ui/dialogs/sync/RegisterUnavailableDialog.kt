/*
 * Copyright (c) Kuba Szczodrzyński 2020-9-3.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.sync

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import coil.load
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.response.RegisterAvailabilityStatus
import pl.szczodrzynski.edziennik.databinding.DialogRegisterUnavailableBinding
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog
import pl.szczodrzynski.edziennik.utils.Utils

class RegisterUnavailableDialog(
    activity: AppCompatActivity,
    private val status: RegisterAvailabilityStatus,
) : BindingDialog<DialogRegisterUnavailableBinding>(activity) {

    override fun getTitleRes(): Int? = null
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogRegisterUnavailableBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.close

    override suspend fun onBeforeShow(): Boolean {
        if (!status.available && status.userMessage != null)
            return true

        if (status.minVersionCode <= BuildConfig.VERSION_CODE)
            return false

        val update = app.config.update
        UpdateAvailableDialog(
            activity = activity,
            update = update,
            mandatory = update != null && update.versionCode >= status.minVersionCode,
        ).show()
        return false
    }

    override suspend fun onShow() {
        b.message = status.userMessage ?: return
        b.text.movementMethod = LinkMovementMethod.getInstance()

        if (status.userMessage.image != null) {
            b.image.load(status.userMessage.image)
        }
        if (status.userMessage.url != null) {
            b.readMore.onClick {
                Utils.openUrl(activity, status.userMessage.url)
            }
        }
    }
}
