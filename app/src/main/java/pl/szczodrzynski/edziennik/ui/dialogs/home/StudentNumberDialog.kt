/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-24.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.home

import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

class StudentNumberDialog(
        val activity: AppCompatActivity,
        val profile: Profile,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "StudentNumberDialog"
    }

    private lateinit var dialog: AlertDialog

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        MaterialDialog.Builder(activity)
                .title(R.string.card_lucky_number_set_title)
                .content(R.string.card_lucky_number_set_text)
                .inputType(InputType.TYPE_CLASS_NUMBER)
                .input(null, if (profile.studentNumber == -1) "" else profile.studentNumber.toString()) { _: MaterialDialog?, input: CharSequence ->
                    try {
                        profile.studentNumber = input.toString().toInt()
                    } catch (e: Exception) {
                        Toast.makeText(activity, R.string.incorrect_format, Toast.LENGTH_SHORT).show()
                    }
                }
                .dismissListener {
                    onDismissListener?.invoke(TAG)
                }.show()
    }}
}