/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-24.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.ext.input

class StudentNumberDialog(
        val activity: AppCompatActivity,
        val profile: Profile,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "StudentNumberDialog"
    }

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.card_lucky_number_set_title)
            .input(
                message = activity.getString(R.string.card_lucky_number_set_text),
                type = InputType.TYPE_CLASS_NUMBER,
                hint = null,
                value = if (profile.studentNumber == -1) null else profile.studentNumber.toString(),
                positiveButton = R.string.ok,
                positiveListener = { _, input ->
                    profile.studentNumber = input.toIntOrNull() ?: -1
                    true
                }
            )
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                onDismissListener?.invoke(TAG)
            }
            .show()
    }}
}
