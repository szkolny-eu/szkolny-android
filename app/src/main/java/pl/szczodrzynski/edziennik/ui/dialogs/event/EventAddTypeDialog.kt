/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-12.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.event

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class EventAddTypeDialog(
        val activity: Activity,
        val profileId: Int,
        val date: Date? = null,
        val time: Time? = null
) {
    companion object {
        private const val TAG = "EventAddTypeDialog"
    }

    private lateinit var dialog: AlertDialog

    init { run {
        dialog = MaterialAlertDialogBuilder(activity)
                .setItems(R.array.main_menu_add_options) { dialog, which ->
                    dialog.dismiss()
                    EventManualDialogOld(activity, profileId)
                            .show(
                                    activity.application as App,
                                    null,
                                    date,
                                    time,
                                    when (which) {
                                        1 -> EventManualDialogOld.DIALOG_HOMEWORK
                                        else -> EventManualDialogOld.DIALOG_EVENT
                                    }
                            )

                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
    }}
}
