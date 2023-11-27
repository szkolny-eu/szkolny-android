/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-18.
 */

package pl.szczodrzynski.edziennik.ui.event

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.EventGetEvent
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.databinding.DialogEventDetailsBinding
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget
import pl.szczodrzynski.edziennik.ui.dialogs.base.BindingDialog
import pl.szczodrzynski.edziennik.ui.notes.setupNotesButton
import pl.szczodrzynski.edziennik.ui.timetable.TimetableFragment
import pl.szczodrzynski.edziennik.utils.BetterLink
import pl.szczodrzynski.edziennik.utils.models.Date

// TODO: 2021-10-19 rewrite to the new dialog style
class EventDetailsDialog(
    activity: AppCompatActivity,
    // this event is observed for changes
    private var event: EventFull,
    private val showNotes: Boolean = true,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BindingDialog<DialogEventDetailsBinding>(activity, onShowListener, onDismissListener) {

    override val TAG = "EventDetailsDialog"

    override fun getTitleRes(): Int? = null
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogEventDetailsBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.close
    override fun getNeutralButtonText() = if (event.addedManually) R.string.remove else null

    private var removeEventDialog: AlertDialog? = null
    private val eventShared = event.sharedBy != null
    private val eventOwn = event.sharedBy == "self"
    private val manager
        get() = app.eventManager

    private val api by lazy {
        SzkolnyApi(app)
    }

    private var progressDialog: AlertDialog? = null

    override suspend fun onNeutralClick(): Boolean {
        showRemoveEventDialog()
        return NO_DISMISS
    }

    override suspend fun onBeforeShow(): Boolean {
        EventBus.getDefault().register(this)
        return true
    }

    override suspend fun onShow() {
        // watch the event for changes
        app.db.eventDao().getById(event.profileId, event.id).observe(activity) {
            event = it ?: return@observe
            update()
        }
    }

    override fun onDismiss() {
        EventBus.getDefault().unregister(this@EventDetailsDialog)
        progressDialog?.dismiss()
    }

    private fun update() {
        b.event = event
        b.eventShared = eventShared
        b.eventOwn = eventOwn

        b.topic.text = event.topicHtml
        b.body.text = event.bodyHtml

        if (!event.seen) {
            manager.markAsSeen(event)
        }

        event.filterNotes()

        val bullet = " • "
        val colorSecondary = android.R.attr.textColorSecondary.resolveAttr(activity)

        try {
            b.monthName = app.resources.getStringArray(R.array.months_day_of_array)[event.date.month - 1]
        }
        catch (_: Exception) {}

        manager.setLegendText(b.legend, event, showNotes)

        b.typeColor.background?.setTintColor(MaterialColors.harmonizeWithPrimary(b.root.context, event.eventColor))

        val agendaSubjectImportant = event.subjectLongName != null
                && App.config[event.profileId].ui.agendaSubjectImportant

        b.name = if (agendaSubjectImportant)
            event.subjectLongName
        else
            event.typeName

        b.details = listOfNotNull(
            if (agendaSubjectImportant)
                event.typeName
            else
                event.subjectLongName,
            event.teamName?.asColoredSpannable(colorSecondary)
        ).concat(bullet)

        b.addedBy.setText(
                when (event.sharedBy) {
                    null -> when {
                        event.addedManually -> R.string.event_details_added_by_self_format
                        event.teacherName == null -> R.string.event_details_added_by_unknown_format
                        else -> R.string.event_details_added_by_format
                    }
                    "self" -> R.string.event_details_shared_by_self_format
                    else -> R.string.event_details_shared_by_format
                },
                Date.fromMillis(event.addedDate).formattedString,
                event.sharedByName ?: event.teacherName ?: ""
        )


        // MARK AS DONE
        b.checkDoneButton.isChecked = event.isDone
        b.checkDoneButton.onChange { _, isChecked ->
            if (isChecked && !event.isDone) {
                b.checkDoneButton.isChecked = false
                MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.event_mark_as_done_title)
                        .setMessage(R.string.event_mark_as_done_text)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            event.isDone = isChecked
                            launch(Dispatchers.Default) {
                                app.db.eventDao().replace(event)
                            }
                            update()
                            b.checkDoneButton.isChecked = true
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
            }
            else if (!isChecked && event.isDone) {
                event.isDone = isChecked
                launch(Dispatchers.Default) {
                    app.db.eventDao().replace(event)
                }
                update()
            }
        }
        b.checkDoneButton.attachToastHint(R.string.hint_mark_as_done)

        // EDIT EVENT
        b.editButton.visibility = if (event.addedManually) View.VISIBLE else View.GONE
        b.editButton.setOnClickListener {
            EventManualDialog(
                    activity,
                    event.profileId,
                    editingEvent = event,
                    onSaveListener = {
                        if (it == null) {
                            dialog.dismiss()
                            return@EventManualDialog
                        }
                        // this should not be needed as the event is observed by the ID
                        // event = it
                        // update()
                    },
                    onShowListener = onShowListener,
                    onDismissListener = onDismissListener
            ).show()
        }
        b.editButton.attachToastHint(R.string.hint_edit_event)

        // SAVE IN CALENDAR
        b.saveInCalendarButton.onClick {
            openInCalendar()
        }
        b.saveInCalendarButton.attachToastHint(R.string.hint_save_in_calendar)

        // GO TO TIMETABLE
        b.goToTimetableButton.onClick {
            dialog.dismiss()
            val dateStr = event.date.stringY_m_d

            val intent =
                    if (activity is MainActivity && activity.navTarget == NavTarget.TIMETABLE)
                        Intent(TimetableFragment.ACTION_SCROLL_TO_DATE)
                    else if (activity is MainActivity)
                        Intent("android.intent.action.MAIN")
                    else
                        Intent(activity, MainActivity::class.java)

            intent.apply {
                putExtras(
                    "fragmentId" to NavTarget.TIMETABLE,
                    "timetableDate" to dateStr,
                )
            }
            if (activity is MainActivity)
                activity.sendBroadcast(intent)
            else
                activity.startActivity(intent)
        }
        b.goToTimetableButton.attachToastHint(R.string.hint_go_to_timetable)

        // RE-DOWNLOAD
        b.downloadButton.isVisible = App.devMode
        b.downloadButton.onClick {
            EdziennikTask.eventGet(event.profileId, event).enqueue(activity)
        }
        b.downloadButton.attachToastHint(R.string.hint_download_again)

        BetterLink.attach(b.topic, onActionSelected = dialog::dismiss)

        event.teacherName?.let { name ->
            BetterLink.attach(
                b.teacherName,
                teachers = mapOf(event.teacherId to name),
                onActionSelected = dialog::dismiss
            )
        }

        if (!event.addedManually && (!event.isDownloaded || event.isHomework && event.homeworkBody == null)) {
            b.bodyTitle.isVisible = true
            b.bodyProgressBar.isVisible = true
            b.body.isVisible = false
            EdziennikTask.eventGet(event.profileId, event).enqueue(activity)
        }
        else if (event.homeworkBody.isNullOrBlank()) {
            b.bodyTitle.isVisible = false
            b.bodyProgressBar.isVisible = false
            b.body.isVisible = false
        }
        else {
            b.bodyTitle.isVisible = true
            b.bodyProgressBar.isVisible = false
            b.body.isVisible = true
            BetterLink.attach(b.body, onActionSelected = dialog::dismiss)
        }

        if (event.attachmentIds.isNullOrEmpty() || event.attachmentNames.isNullOrEmpty()) {
            b.attachmentsTitle.isVisible = false
            b.attachmentsFragment.isVisible = false
        }
        else {
            b.attachmentsTitle.isVisible = true
            b.attachmentsFragment.isVisible = true
            b.attachmentsFragment.init(Bundle().also {
                it.putInt("profileId", event.profileId)
                it.putLongArray("attachmentIds", event.attachmentIds!!.toLongArray())
                it.putStringArray("attachmentNames", event.attachmentNames!!.toTypedArray())
            }, owner = event)
        }

        b.notesButton.isVisible = showNotes
        b.notesButton.setupNotesButton(
            activity = activity,
            owner = event,
            onShowListener = onShowListener,
            onDismissListener = onDismissListener,
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEventGetEvent(event: EventGetEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.event.homeworkBody == null)
            event.event.homeworkBody = ""
        update()
    }

    private fun showRemovingProgressDialog() {
        if (progressDialog != null) {
            return
        }

        progressDialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.please_wait)
                .setMessage(R.string.event_removing_text)
                .setCancelable(false)
                .show()
    }

    private fun showRemoveEventDialog() {
        val shareNotice = when {
            eventShared && eventOwn -> "\n\n"+activity.getString(R.string.dialog_event_manual_remove_shared_self)
            eventShared && !eventOwn -> "\n\n"+activity.getString(R.string.dialog_event_manual_remove_shared)
            else -> ""
        }
        removeEventDialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.are_you_sure)
                .setMessage(activity.getString(R.string.dialog_register_event_manual_remove_confirmation)+shareNotice)
                .setPositiveButton(R.string.yes, null)
                .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
                .create()
                .apply {
                    setOnShowListener { dialog ->
                        val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                        positiveButton?.setOnClickListener {
                            removeEvent()
                        }
                    }

                    show()
                }
    }

    private fun removeEvent() {
        launch {
            if (eventShared && eventOwn) {
                // unshare + remove own event
                showRemovingProgressDialog()

                api.runCatching(activity) {
                    unshareEvent(event)
                } ?: run {
                    progressDialog?.dismiss()
                    return@launch
                }

                finishRemoving()
            } else if (eventShared && !eventOwn) {
                // remove + blacklist somebody's event
                Toast.makeText(activity, "Nie zaimplementowana opcja :(", Toast.LENGTH_SHORT).show()
                // TODO
            } else {
                // remove event
                Toast.makeText(activity, R.string.event_manual_remove, Toast.LENGTH_SHORT).show()
                finishRemoving()
            }
            progressDialog?.dismiss()
        }
    }

    private fun finishRemoving() {
        launch {
            withContext(Dispatchers.Default) {
                app.db.eventDao().remove(event)
            }
        }

        removeEventDialog?.dismiss()
        dialog.dismiss()
        Toast.makeText(activity, R.string.removed, Toast.LENGTH_SHORT).show()
    }

    private fun openInCalendar() { launch {
        val title = event.typeName ?: "" +
                (if (event.typeName.isNotNullNorBlank() && event.subjectLongName.isNotNullNorBlank()) " - " else " ") +
                (event.subjectLongName ?: "")

        val intent = Intent(Intent.ACTION_EDIT).apply {
            data = Events.CONTENT_URI
            putExtra(Events.TITLE, title)
            putExtra(Events.DESCRIPTION, event.topicHtml.toString())

            if (event.time == null) {
                putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.date.inMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.date.inMillis)
            } else {
                val startTime = event.date.combineWith(event.time)
                val endTime = startTime + 45 * 60 * 1000 /* 45 min */

                putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            }
        }

        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.calendar_app_not_found, Toast.LENGTH_SHORT).show()
        }
    }}
}
