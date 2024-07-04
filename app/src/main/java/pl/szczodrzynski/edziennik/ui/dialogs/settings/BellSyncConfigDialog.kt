/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-20.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogEditTextBinding
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog
import pl.szczodrzynski.edziennik.utils.models.Time

class BellSyncConfigDialog(
    activity: AppCompatActivity,
    private val onChangeListener: (() -> Unit)? = null,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BindingDialog<DialogEditTextBinding>(activity, onShowListener, onDismissListener) {

    override val TAG = "BellSyncConfigDialog"

    override fun getTitleRes() = R.string.bell_sync_title
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogEditTextBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.ok
    override fun getNeutralButtonText() = R.string.reset
    override fun getNegativeButtonText() = R.string.cancel

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

    override suspend fun onShow() {
        b.title.setText(R.string.bell_sync_adjust_content)
        b.text1.hint = "±H:MM:SS"
        b.text1.setText(app.config.timetable.bellSyncDiff?.let {
            (if (app.config.timetable.bellSyncMultiplier == -1) "-" else "+") + it.stringHMS
        } ?: "+0:00:00")
        b.text1.addTextChangedListener { text ->
            val input = text?.toString()
            b.textInputLayout.error =
                if (input != null && parse(input) == null)
                    activity.getString(R.string.bell_sync_adjust_error)
                else
                    null
        }
    }

    override suspend fun onPositiveClick(): Boolean {
        val input = b.text1.text?.toString() ?: return NO_DISMISS
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
