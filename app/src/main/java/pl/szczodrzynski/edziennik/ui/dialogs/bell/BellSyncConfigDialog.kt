/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-20.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.bell

import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.coroutines.CoroutineContext

class BellSyncConfigDialog(
    val activity: AppCompatActivity,
    val onChangeListener: (() -> Unit)? = null,
    val onShowListener: ((tag: String) -> Unit)? = null,
    val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "BellSyncConfigDialog"
    }

    private lateinit var app: App
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local variables go here

    private fun parse(input: String): Pair<Time, Int>? {
        if (input.length < 8) {
            return null
        }
        if (input[2] != ':' || input[5] != ':') {
            return null
        }
        val multiplier = when {
            input[0] == '+' -> 1
            input[0] == '-' -> -1
            else -> return null
        }
        val time = Time.fromH_m_s("0" + input.substring(1))

        return time to multiplier
    }

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App

        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.bell_sync_title)
            .setView(R.layout.dialog_edit_text)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.reset) { _, _ ->
                app.config.timetable.bellSyncDiff = null
                app.config.timetable.bellSyncMultiplier = 0
                onChangeListener?.invoke()
            }
            .setOnDismissListener {
                onDismissListener?.invoke(TAG)
            }
            .show()

        val message = dialog.findViewById<TextView>(android.R.id.title)
        val editText = dialog.findViewById<TextInputEditText>(android.R.id.text1)
        val textLayout = dialog.findViewById<TextInputLayout>(R.id.text_input_layout)

        message?.setText(R.string.bell_sync_adjust_content)
        editText?.hint = "±H:MM:SS"
        editText?.setText(app.config.timetable.bellSyncDiff?.let {
            (if (app.config.timetable.bellSyncMultiplier == -1) "-" else "+") + it.stringHMS
        } ?: "+0:00:00")
        editText?.addTextChangedListener { text ->
            val input = text?.toString()
            textLayout?.error =
                if (input != null && parse(input) == null)
                    activity.getString(R.string.bell_sync_adjust_error)
                else
                    null
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.onClick {
            val input = editText?.text?.toString() ?: return@onClick
            val parsed = parse(input)
            if (parsed == null) {
                Toast.makeText(activity, R.string.bell_sync_adjust_error, Toast.LENGTH_SHORT).show()
                return@onClick
            }

            val (time, multiplier) = parsed
            app.config.timetable.bellSyncDiff =
                if (time.value == 0)
                    null
                else
                    time
            app.config.timetable.bellSyncMultiplier = multiplier

            onChangeListener?.invoke()
            dialog.dismiss()
        }
    }}
}
