/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-14.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.MessagesConfigDialogBinding
import pl.szczodrzynski.edziennik.ui.dialogs.base.ConfigDialog

class MessagesConfigDialog(
    activity: AppCompatActivity,
    reloadOnDismiss: Boolean = true,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : ConfigDialog<MessagesConfigDialogBinding>(
    activity,
    reloadOnDismiss,
    onShowListener,
    onDismissListener,
) {

    override val TAG = "MessagesConfigDialog"

    override fun getTitleRes() = R.string.menu_messages_config
    override fun inflate(layoutInflater: LayoutInflater) =
        MessagesConfigDialogBinding.inflate(layoutInflater)

    override suspend fun loadConfig() {
        b.config = app.profile.config.ui

        b.greetingText.setText(
            app.profile.config.ui.messagesGreetingText
                ?: "\n\nZ poważaniem\n${app.profile.accountOwnerName}"
        )
    }

    override suspend fun saveConfig() {
        val greetingText = b.greetingText.text?.toString()?.trim()
        if (greetingText.isNullOrEmpty())
            app.profile.config.ui.messagesGreetingText = null
        else
            app.profile.config.ui.messagesGreetingText = "\n\n$greetingText"
    }
}
