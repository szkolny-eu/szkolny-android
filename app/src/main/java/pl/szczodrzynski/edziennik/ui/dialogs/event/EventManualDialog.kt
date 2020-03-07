/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-12.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.event

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL
import androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_AGENDA
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.EventType
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.databinding.DialogEventManualV2Binding
import pl.szczodrzynski.edziennik.ui.modules.views.TimeDropdown.Companion.DISPLAY_LESSONS
import pl.szczodrzynski.edziennik.utils.Anim
import pl.szczodrzynski.edziennik.utils.TextInputDropDown
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.coroutines.CoroutineContext

class EventManualDialog(
        val activity: AppCompatActivity,
        val profileId: Int,
        val defaultLesson: LessonFull? = null,
        val defaultDate: Date? = null,
        val defaultTime: Time? = null,
        val defaultType: Long? = null,
        val editingEvent: EventFull? = null,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {

    companion object {
        private const val TAG = "EventManualDialog"
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val app by lazy { activity.application as App }
    private lateinit var b: DialogEventManualV2Binding
    private lateinit var dialog: AlertDialog

    private var customColor: Int? = null
    private val editingShared = editingEvent?.sharedBy != null
    private val editingOwn = editingEvent?.sharedBy == "self"
    private var removeEventDialog: AlertDialog? = null
    private var defaultLoaded = false

    private val api by lazy {
        SzkolnyApi(app)
    }

    init { run {
        if (activity.isFinishing)
            return@run
        job = Job()
        onShowListener?.invoke(TAG)
        b = DialogEventManualV2Binding.inflate(activity.layoutInflater)
        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.dialog_event_manual_title)
                .setView(b.root)
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(R.string.save, null)
                .apply {
                    if (editingEvent != null) {
                        setNeutralButton(R.string.remove, null)
                    }
                }
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .setCancelable(false)
                .create()
                .apply {
                    setOnShowListener { dialog ->
                        val positiveButton = (dialog as AlertDialog).getButton(BUTTON_POSITIVE)
                        positiveButton?.setOnClickListener {
                            saveEvent()
                        }

                        val neutralButton = dialog.getButton(BUTTON_NEUTRAL)
                        neutralButton?.setOnClickListener {
                            showRemoveEventDialog()
                        }
                    }

                    show()
                }

        b.shareSwitch.isChecked = editingShared
        b.shareSwitch.isEnabled = !editingShared || (editingShared && editingOwn)

        b.showMore.onClick { // TODO iconics is broken
            it.apply {
                refreshDrawableState()

                if (isChecked)
                    Anim.expand(b.moreLayout, 200, null)
                else
                    Anim.collapse(b.moreLayout, 200, null)
            }
        }

        updateShareText()
        b.shareSwitch.onChange { _, isChecked ->
            updateShareText(isChecked)
        }

        loadLists()
    }}

    private fun updateShareText(checked: Boolean = b.shareSwitch.isChecked) {
        b.shareDetails.visibility = if (checked || editingShared)
            View.VISIBLE
        else View.GONE

        val text = when {
            checked && editingShared && editingOwn -> R.string.dialog_event_manual_share_will_change
            checked && editingShared -> R.string.dialog_event_manual_share_will_request
            !checked && editingShared -> R.string.dialog_event_manual_share_will_remove
            else -> R.string.dialog_event_manual_share_first_notice
        }

        b.shareDetails.setText(text, editingEvent?.sharedByName ?: "")
    }

    private fun loadLists() { launch {
        with (b.dateDropdown) {
            db = app.db
            profileId = App.profileId
            showWeekDays = false
            showDays = true
            showOtherDate = true
            defaultLesson?.let {
                nextLessonSubjectId = it.displaySubjectId
                nextLessonSubjectName = it.displaySubjectName
                nextLessonTeamId = it.displayTeamId
            }
            loadItems()
            selectDefault(editingEvent?.eventDate)
            selectDefault(defaultLesson?.displayDate ?: defaultDate)
            onDateSelected = { date, lesson ->
                b.timeDropdown.deselect()
                b.timeDropdown.lessonsDate = date
                this@EventManualDialog.launch {
                    b.timeDropdown.loadItems()
                    lesson?.displayStartTime?.let { b.timeDropdown.selectTime(it) }
                    lesson?.displaySubjectId?.let { b.subjectDropdown.selectSubject(it) } ?: b.subjectDropdown.deselect()
                    lesson?.displayTeacherId?.let { b.teacherDropdown.selectTeacher(it) } ?: b.teacherDropdown.deselect()
                    lesson?.displayTeamId?.let { b.teamDropdown.selectTeam(it) } ?: b.teamDropdown.selectTeamClass()
                }
            }
        }

        with (b.timeDropdown) {
            db = app.db
            profileId = App.profileId
            showAllDay = true
            showCustomTime = true
            lessonsDate = b.dateDropdown.getSelected() as? Date ?: Date.getToday()
            displayMode = DISPLAY_LESSONS
            loadItems()
            selectDefault(editingEvent?.startTime)
            selectDefault(defaultLesson?.displayStartTime ?: defaultTime)
            onLessonSelected = { lesson ->
                lesson.displaySubjectId?.let { b.subjectDropdown.selectSubject(it) } ?: b.subjectDropdown.deselect()
                lesson.displayTeacherId?.let { b.teacherDropdown.selectTeacher(it) } ?: b.teacherDropdown.deselect()
                lesson.displayTeamId?.let { b.teamDropdown.selectTeam(it) } ?: b.teamDropdown.selectTeamClass()
            }
        }

        with (b.teamDropdown) {
            db = app.db
            profileId = App.profileId
            showNoTeam = true
            loadItems()
            selectTeamClass()
            selectDefault(editingEvent?.teamId)
            selectDefault(defaultLesson?.displayTeamId)
        }

        with (b.subjectDropdown) {
            db = app.db
            profileId = App.profileId
            showNoSubject = true
            showCustomSubject = false
            loadItems()
            selectDefault(editingEvent?.subjectId)
            selectDefault(defaultLesson?.displaySubjectId)
        }

        with (b.teacherDropdown) {
            db = app.db
            profileId = App.profileId
            showNoTeacher = true
            loadItems()
            selectDefault(editingEvent?.teacherId)
            selectDefault(defaultLesson?.displayTeacherId)
        }


        val deferred = async(Dispatchers.Default) {
            // get the event type list
            val eventTypes = app.db.eventTypeDao().getAllNow(profileId)
            b.typeDropdown.clear()
            b.typeDropdown += eventTypes.map { TextInputDropDown.Item(it.id, it.name, tag = it) }
        }
        deferred.await()

        b.typeDropdown.isEnabled = true

        defaultType?.let {
            b.typeDropdown.select(it)
        }

        b.typeDropdown.selected?.let { item ->
            customColor = (item.tag as EventType).color
        }

        // copy IDs from event being edited
        editingEvent?.let {
            b.topic.setText(it.topic)
            b.typeDropdown.select(it.type.toLong())?.let { item ->
                customColor = (item.tag as EventType).color
            }
            if (it.color != -1)
                customColor = it.color
        }

        // copy IDs from the LessonFull
        defaultLesson?.let {
            b.teamDropdown.select(it.displayTeamId)
        }

        b.typeDropdown.setOnChangeListener {
            b.typeColor.background.colorFilter = PorterDuffColorFilter((it.tag as EventType).color, PorterDuff.Mode.SRC_ATOP)
            customColor = null
            return@setOnChangeListener true
        }

        (customColor ?: Event.COLOR_DEFAULT).let {
            b.typeColor.background.colorFilter = PorterDuffColorFilter(it, PorterDuff.Mode.SRC_ATOP)
        }

        b.typeColor.onClick {
            val currentColor = (b.typeDropdown?.selected?.tag as EventType?)?.color ?: Event.COLOR_DEFAULT
            val colorPickerDialog = ColorPickerDialog.newBuilder()
                    .setColor(currentColor)
                    .create()
            colorPickerDialog.setColorPickerDialogListener(
                    object : ColorPickerDialogListener {
                        override fun onDialogDismissed(dialogId: Int) {}
                        override fun onColorSelected(dialogId: Int, color: Int) {
                            b.typeColor.background.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                            customColor = color
                        }
                    })
            colorPickerDialog.show(activity.fragmentManager, "color-picker-dialog")
        }
    }}

    private fun showRemoveEventDialog() {
        val shareNotice = when {
            editingShared && editingOwn -> "\n\n"+activity.getString(R.string.dialog_event_manual_remove_shared_self)
            editingShared && !editingOwn -> "\n\n"+activity.getString(R.string.dialog_event_manual_remove_shared)
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
                        val positiveButton = (dialog as AlertDialog).getButton(BUTTON_POSITIVE)
                        positiveButton?.setOnClickListener {
                            removeEvent()
                        }
                    }

                    show()
                }
    }

    private fun saveEvent() {
        val date = b.dateDropdown.getSelected() as? Date
        val startTimePair = b.timeDropdown.getSelected() as? Pair<*, *>
        val startTime = startTimePair?.first as? Time
        val teamId = b.teamDropdown.getSelected() as? Long
        val type = b.typeDropdown.selected?.id
        val topic = b.topic.text?.toString()
        val subjectId = b.subjectDropdown.getSelected() as? Long
        val teacherId = b.teacherDropdown.getSelected() as? Long

        val share = b.shareSwitch.isChecked

        b.dateDropdown.error = null
        b.teamDropdown.error = null
        b.typeDropdown.error = null
        b.topic.error = null

        var isError = false

        if (date == null) {
            b.dateDropdown.error = app.getString(R.string.dialog_event_manual_date_choose)
            isError = true
        }

        if (share && teamId == null) {
            b.teamDropdown.error = app.getString(R.string.dialog_event_manual_team_choose)
            isError = true
        }

        if (type == null) {
            b.typeDropdown.error = app.getString(R.string.dialog_event_manual_type_choose)
            isError = true
        }

        if (topic.isNullOrBlank()) {
            b.topic.error = app.getString(R.string.dialog_event_manual_topic_choose)
            isError = true
        }

        if (isError) return

        val id = System.currentTimeMillis()

        val eventObject = Event(
                profileId,
                editingEvent?.id ?: id,
                date,
                startTime,
                topic,
                customColor ?: -1,
                type ?: Event.TYPE_DEFAULT,
                true,
                teacherId ?: -1,
                subjectId ?: -1,
                teamId ?: -1
        )

        val metadataObject = Metadata(
                profileId,
                when (type) {
                    Event.TYPE_HOMEWORK -> Metadata.TYPE_HOMEWORK
                    else -> Metadata.TYPE_EVENT
                },
                eventObject.id,
                true,
                true,
                editingEvent?.addedDate ?: System.currentTimeMillis()
        )

        launch {
            val profile = app.db.profileDao().getByIdNow(profileId)

            if (!share && !editingShared) {
                Toast.makeText(activity, R.string.event_manual_saving, Toast.LENGTH_SHORT).show()
                finishAdding(eventObject, metadataObject)
            }
            else if (editingShared && !editingOwn) {
                Toast.makeText(activity, "Opcja edycji wydarzeń innych uczniów nie została jeszcze zaimplementowana.", Toast.LENGTH_LONG).show()
                // TODO
            }
            else if (!share && editingShared) {
                Toast.makeText(activity, R.string.event_manual_unshare, Toast.LENGTH_SHORT).show()

                eventObject.apply {
                    sharedBy = null
                    sharedByName = profile?.studentNameLong
                }

                api.runCatching(activity) {
                    unshareEvent(eventObject)
                } ?: return@launch

                eventObject.sharedByName = null
                finishAdding(eventObject, metadataObject)
            }
            else if (share) {
                Toast.makeText(activity, R.string.event_manual_share, Toast.LENGTH_SHORT).show()

                eventObject.apply {
                    sharedBy = profile?.userCode
                    sharedByName = profile?.studentNameLong
                }

                metadataObject.addedDate = System.currentTimeMillis()

                api.runCatching(activity) {
                    shareEvent(eventObject.withMetadata(metadataObject))
                } ?: return@launch

                eventObject.sharedBy = "self"
                finishAdding(eventObject, metadataObject)
            }
            else {
                Toast.makeText(activity, "Unknown action :(", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeEvent() {
        launch {
            if (editingShared && editingOwn) {
                // unshare + remove own event
                Toast.makeText(activity, R.string.event_manual_unshare_remove, Toast.LENGTH_SHORT).show()

                api.runCatching(activity) {
                    unshareEvent(editingEvent!!)
                } ?: return@launch

                finishRemoving()
            } else if (editingShared && !editingOwn) {
                // remove + blacklist somebody's event
                Toast.makeText(activity, "Nie zaimplementowana opcja :(", Toast.LENGTH_SHORT).show()
                // TODO
            } else {
                // remove event
                Toast.makeText(activity, R.string.event_manual_remove, Toast.LENGTH_SHORT).show()
                finishRemoving()
            }
        }
    }

    private fun finishAdding(eventObject: Event, metadataObject: Metadata) {
        launch {
            withContext(Dispatchers.Default) {
                app.db.eventDao().add(eventObject)
                app.db.metadataDao().add(metadataObject)
            }
        }

        dialog.dismiss()
        Toast.makeText(activity, R.string.saved, Toast.LENGTH_SHORT).show()
        if (activity is MainActivity && activity.navTargetId == DRAWER_ITEM_AGENDA)
            activity.reloadTarget()
    }
    private fun finishRemoving() {
        launch {
            withContext(Dispatchers.Default) {
                app.db.eventDao().remove(editingEvent)
            }
        }

        removeEventDialog?.dismiss()
        dialog.dismiss()
        Toast.makeText(activity, R.string.removed, Toast.LENGTH_SHORT).show()
        if (activity is MainActivity && activity.navTargetId == DRAWER_ITEM_AGENDA)
            activity.reloadTarget()
    }
}
