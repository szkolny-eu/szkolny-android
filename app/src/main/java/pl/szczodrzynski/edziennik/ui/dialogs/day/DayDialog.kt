/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-16.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.day

import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogDayBinding
import pl.szczodrzynski.edziennik.onClick
import pl.szczodrzynski.edziennik.setText
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventDetailsDialog
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventListAdapter
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventManualDialog
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Week
import kotlin.coroutines.CoroutineContext

class DayDialog(
        val activity: AppCompatActivity,
        val profileId: Int,
        val date: Date,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "DayDialog"
    }

    private lateinit var app: App
    private lateinit var b: DialogDayBinding
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var adapter: EventListAdapter

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App
        b = DialogDayBinding.inflate(activity.layoutInflater)
        dialog = MaterialAlertDialogBuilder(activity)
                .setView(b.root)
                .setPositiveButton(R.string.close) { dialog, _ ->
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.add, null)
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .show()

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.onClick {
            EventManualDialog(
                    activity,
                    profileId,
                    defaultDate = date,
                    onShowListener = onShowListener,
                    onDismissListener = onDismissListener
            )
        }

        update()
    }}

    private fun update() {
        b.dayDate.setText(
                R.string.dialog_day_date_format,
                Week.getFullDayName(date.weekDay),
                date.formattedString
        )

        adapter = EventListAdapter(
                activity,
                onItemClick = {
                    EventDetailsDialog(
                            activity,
                            it,
                            onShowListener = onShowListener,
                            onDismissListener = onDismissListener
                    )
                },
                onEventEditClick = {
                    EventManualDialog(
                            activity,
                            it.profileId,
                            editingEvent = it,
                            onShowListener = onShowListener,
                            onDismissListener = onDismissListener
                    )
                }
        )

        app.db.eventDao().getAllByDate(profileId, date).observe(activity, Observer { events ->
            adapter.items = events
            if (b.eventsView.adapter == null) {
                b.eventsView.adapter = adapter
                b.eventsView.apply {
                    isNestedScrollingEnabled = false
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    addItemDecoration(SimpleDividerItemDecoration(context))
                }
            }
            adapter.notifyDataSetChanged()

            if (events != null && events.isNotEmpty()) {
                b.eventsView.visibility = View.VISIBLE
                b.eventsNoData.visibility = View.GONE
            } else {
                b.eventsView.visibility = View.GONE
                b.eventsNoData.visibility = View.VISIBLE
            }
        })
    }
}
