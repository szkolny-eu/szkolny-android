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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.EventGetEvent
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.databinding.DialogEventDetailsBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.asColoredSpannable
import pl.szczodrzynski.edziennik.ext.attachToastHint
import pl.szczodrzynski.edziennik.ext.concat
import pl.szczodrzynski.edziennik.ext.isNotNullNorBlank
import pl.szczodrzynski.edziennik.ext.onChange
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.putExtras
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ext.setText
import pl.szczodrzynski.edziennik.ext.setTintColor
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
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
) : BindingDialog<DialogEventDetailsBinding>(activity) {

    override fun getTitleRes(): Int? = null
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogEventDetailsBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.close
    override fun getNeutralButtonText() = if (event.addedManually) R.string.remove else null

    private val eventShared = event.sharedBy != null
    private val eventOwn = event.sharedBy == "self"
    private val manager
        get() = app.eventManager

    private val api by lazy {
        SzkolnyApi(app)
    }

    private var progressDialog: BaseDialog<*>? = null

    override suspend fun onNeutralClick(): Boolean {
        showRemoveEventDialog()
        return NO_DISMISS
    }

    override suspend fun onShow() {
        // watch the event for changes
        app.db.eventDao().getById(event.profileId, event.id).observe(activity) {
            event = it ?: return@observe
            update()
        }
    }

    override suspend fun onDismiss() {
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
                SimpleDialog<Unit>(activity) {
                    title(R.string.event_mark_as_done_title)
                    message(R.string.event_mark_as_done_text)
                    positive(R.string.ok) {
                        event.isDone = true
                        withContext(Dispatchers.IO) {
                            app.db.eventDao().replace(event)
                        }
                        update()
                        b.checkDoneButton.isChecked = true
                    }
                    negative(R.string.cancel)
                }.show()
            }
            else if (!isChecked && event.isDone) {
                event.isDone = false
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
                            dismiss()
                            return@EventManualDialog
                        }
                        // this should not be needed as the event is observed by the ID
                        // event = it
                        // update()
                    },
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
            dismiss()
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

        BetterLink.attach(b.topic, onActionSelected = ::dismiss)

        event.teacherName?.let { name ->
            BetterLink.attach(
                b.teacherName,
                teachers = mapOf(event.teacherId to name),
                onActionSelected = ::dismiss
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
            BetterLink.attach(b.body, onActionSelected = ::dismiss)
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

        progressDialog = SimpleDialog<Unit>(activity) {
            title(R.string.please_wait)
            message(R.string.event_removing_text)
            cancelable(false)
        }.show()
    }

    private fun showRemoveEventDialog() {
        val shareNotice = when {
            eventShared && eventOwn -> "\n\n"+activity.getString(R.string.dialog_event_manual_remove_shared_self)
            eventShared && !eventOwn -> "\n\n"+activity.getString(R.string.dialog_event_manual_remove_shared)
            else -> ""
        }
        SimpleDialog<Unit>(activity) {
            title(R.string.are_you_sure)
            message(activity.getString(R.string.dialog_register_event_manual_remove_confirmation) + shareNotice)
            positive(R.string.yes) {
                removeEvent()
            }
            negative(R.string.no)
        }.show()
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

        dismiss()
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
