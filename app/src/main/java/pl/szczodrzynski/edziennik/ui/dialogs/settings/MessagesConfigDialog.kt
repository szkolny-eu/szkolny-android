/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-14.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.MessagesConfigDialogBinding

class MessagesConfigDialog(
    private val activity: AppCompatActivity,
    private val reloadOnDismiss: Boolean = true,
    private val onShowListener: ((tag: String) -> Unit)? = null,
    private val onDismissListener: ((tag: String) -> Unit)? = null
) {
    companion object {
        const val TAG = "MessagesConfigDialog"
    }

    private val app by lazy { activity.application as App }
    private val config by lazy { app.config.ui }
    private val profileConfig by lazy { app.config.forProfile().ui }

    private lateinit var b: MessagesConfigDialogBinding
    private lateinit var dialog: AlertDialog

    init { run {
        if (activity.isFinishing)
            return@run
        b = MessagesConfigDialogBinding.inflate(activity.layoutInflater)
        onShowListener?.invoke(TAG)
        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.menu_messages_config)
            .setView(b.root)
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener {
                saveConfig()
                onDismissListener?.invoke(TAG)
                if (reloadOnDismiss) (activity as? MainActivity)?.reloadTarget()
            }
            .create()
        loadConfig()
        dialog.show()
    }}

    private fun loadConfig() {
        b.config = profileConfig

        b.greetingText.setText(
            profileConfig.messagesGreetingText ?: "\n\nZ poważaniem\n${app.profile.accountOwnerName}"
        )
    }

    private fun saveConfig() {
        val greetingText = b.greetingText.text?.toString()?.trim()
        if (greetingText.isNullOrEmpty())
            profileConfig.messagesGreetingText = null
        else
            profileConfig.messagesGreetingText = "\n\n$greetingText"
    }
}
