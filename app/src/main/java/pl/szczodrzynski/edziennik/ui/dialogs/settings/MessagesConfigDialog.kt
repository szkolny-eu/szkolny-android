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

    private val profileConfig by lazy { app.config.getFor(app.profileId).ui }

    override suspend fun loadConfig() {
        b.config = profileConfig

        b.greetingText.setText(
            profileConfig.messagesGreetingText
                ?: "\n\nZ poważaniem\n${app.profile.accountOwnerName}"
        )
    }

    override suspend fun saveConfig() {
        val greetingText = b.greetingText.text?.toString()?.trim()
        if (greetingText.isNullOrEmpty())
            profileConfig.messagesGreetingText = null
        else
            profileConfig.messagesGreetingText = "\n\n$greetingText"
    }
}
