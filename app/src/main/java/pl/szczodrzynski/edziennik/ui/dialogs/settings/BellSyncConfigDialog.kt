/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-20.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.text.Editable
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog
import pl.szczodrzynski.edziennik.utils.models.Time

class BellSyncConfigDialog(
    activity: AppCompatActivity,
    private val onChangeListener: (() -> Unit)? = null,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog<Unit>(activity, onShowListener, onDismissListener) {

    override val TAG = "BellSyncConfigDialog"

    override fun getTitleRes() = R.string.bell_sync_title
    override fun getMessageRes() = R.string.bell_sync_adjust_content

    override fun getPositiveButtonText() = R.string.ok
    override fun getNeutralButtonText() = R.string.reset
    override fun getNegativeButtonText() = R.string.cancel

    override fun getInputType() = InputType.TYPE_CLASS_TEXT
    override fun getInputHint() = "±H:MM:SS"
    override fun getInputValue() = app.config.timetable.bellSyncDiff?.let {
        (if (app.config.timetable.bellSyncMultiplier == -1) "-" else "+") + it.stringHMS
    } ?: "+0:00:00"

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

    override suspend fun onInputTextChanged(input: TextInputEditText, text: Editable?) {
        val value = text?.toString()
        input.error =
            if (value != null && parse(value) == null)
                activity.getString(R.string.bell_sync_adjust_error)
            else
                null
    }

    override suspend fun onPositiveClick(): Boolean {
        val input = getInput()?.text?.toString() ?: return NO_DISMISS
        val parsed = parse(input)
        if (parsed == null) {
            Toast.makeText(activity, R.string.bell_sync_adjust_error, Toast.LENGTH_SHORT).show()
            return NO_DISMISS
        }

        val (time, multiplier) = parsed
        app.config.timetable.bellSyncDiff =
            if (time.value == 0)
                null
            else
                time
        app.config.timetable.bellSyncMultiplier = multiplier

        onChangeListener?.invoke()
        return DISMISS
    }

    override suspend fun onNeutralClick(): Boolean {
        app.config.timetable.bellSyncDiff = null
        app.config.timetable.bellSyncMultiplier = 0
        onChangeListener?.invoke()
        return DISMISS
    }
}
