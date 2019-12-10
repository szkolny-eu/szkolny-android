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
import androidx.lifecycle.Observer
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull
import pl.szczodrzynski.edziennik.data.db.modules.events.EventType
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.data.db.modules.timetable.LessonFull
import pl.szczodrzynski.edziennik.databinding.DialogEventManualV2Binding
import pl.szczodrzynski.edziennik.utils.Anim
import pl.szczodrzynski.edziennik.utils.TextInputDropDown
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week
import kotlin.coroutines.CoroutineContext

class EventManualDialog(
        val activity: AppCompatActivity,
        val profileId: Int,
        val defaultLesson: LessonFull? = null,
        val defaultDate: Date? = null,
        val defaultTime: Time? = null,
        val defaultType: Int? = null,
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
    private var removeEventDialog: AlertDialog? = null
    private var defaultLoaded = false

    private lateinit var event: Event
    private var customColor: Int? = null

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
                .create()
                .apply {
                    setOnShowListener { dialog ->
                        val positiveButton = (dialog as AlertDialog).getButton(BUTTON_POSITIVE)
                        positiveButton?.setOnClickListener {
                            saveEvent()
                        }

                        val negativeButton = dialog.getButton(BUTTON_NEUTRAL)
                        negativeButton?.setOnClickListener {
                            showRemoveEventDialog()
                        }
                    }

                    show()
                }

        event = editingEvent?.clone() ?: Event().also { event ->
            event.profileId = profileId
            defaultType?.let {
                event.type = it
            }
            b.shareSwitch.isChecked = event.sharedBy != null
        }

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

    private fun showRemoveEventDialog() {
        removeEventDialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.are_you_sure)
                .setMessage(activity.getString(R.string.dialog_register_event_manual_remove_confirmation))
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

    private fun updateShareText(checked: Boolean = b.shareSwitch.isChecked) {
        val editingShared = editingEvent?.sharedBy != null
        val editingOwn = editingEvent?.sharedBy == "self"

        b.shareDetails.visibility = if (checked || editingShared)
            View.VISIBLE
        else View.GONE

        val text = when {
            checked && editingShared && editingOwn -> R.string.dialog_event_manual_share_will_change
            checked && editingShared -> R.string.dialog_event_manual_share_will_request
            !checked && editingShared -> R.string.dialog_event_manual_share_will_remove
            else -> R.string.dialog_event_manual_share_first_notice
        }

        b.shareDetails.setText(text)
    }

    private fun loadLists() { launch {
        val deferred = async(Dispatchers.Default) {
            // get the team list
            val teams = app.db.teamDao().getAllNow(profileId)
            b.teamDropdown.clear()
            b.teamDropdown += TextInputDropDown.Item(
                    -1,
                    activity.getString(R.string.dialog_event_manual_no_team),
                    ""
            )
            b.teamDropdown += teams.map { TextInputDropDown.Item(it.id, it.name, tag = it) }

            // get the subject list
            val subjects = app.db.subjectDao().getAllNow(profileId)
            b.subjectDropdown.clear()
            b.subjectDropdown += TextInputDropDown.Item(
                    -1,
                    activity.getString(R.string.dialog_event_manual_no_subject),
                    ""
            )
            b.subjectDropdown += subjects.map { TextInputDropDown.Item(it.id, it.longName, tag = it) }

            // get the teacher list
            val teachers = app.db.teacherDao().getAllNow(profileId)
            b.teacherDropdown.clear()
            b.teacherDropdown += TextInputDropDown.Item(
                    -1,
                    activity.getString(R.string.dialog_event_manual_no_teacher),
                    ""
            )
            b.teacherDropdown += teachers.map { TextInputDropDown.Item(it.id, it.fullName, tag = it) }

            // get the event type list
            val eventTypes = app.db.eventTypeDao().getAllNow(profileId)
            b.typeDropdown.clear()
            b.typeDropdown += eventTypes.map { TextInputDropDown.Item(it.id, it.name, tag = it) }
        }
        deferred.await()

        b.teamDropdown.isEnabled = true
        b.subjectDropdown.isEnabled = true
        b.teacherDropdown.isEnabled = true
        b.typeDropdown.isEnabled = true

        defaultType?.let {
            b.typeDropdown.select(it.toLong())
        }

        b.typeDropdown.selected?.let { item ->
            customColor = (item.tag as EventType).color
        }

        // copy IDs from event being edited
        editingEvent?.let {
            b.teamDropdown.select(it.teamId)
            b.subjectDropdown.select(it.subjectId)
            b.teacherDropdown.select(it.teacherId)
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
            b.subjectDropdown.select(it.displaySubjectId)
            b.teacherDropdown.select(it.displayTeacherId)
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

        loadDates()
    }}

    private fun loadDates() { launch {
        val date = Date.getToday()
        val today = date.value
        var weekDay = date.weekDay

        val deferred = async(Dispatchers.Default) {
            val dates = mutableListOf<TextInputDropDown.Item>()
            // item choosing the next lesson of specific subject
            b.subjectDropdown.selected?.let {
                if (it.tag is Subject) {
                    dates += TextInputDropDown.Item(
                            -it.id,
                            activity.getString(R.string.dialog_event_manual_date_next_lesson, it.tag.longName)
                    )
                }
            }

            // TODAY
            dates += TextInputDropDown.Item(
                    date.value.toLong(),
                    activity.getString(R.string.dialog_event_manual_date_today, date.formattedString),
                    tag = date.clone()
            )

            // TOMORROW
            if (weekDay < 4) {
                date.stepForward(0, 0, 1)
                weekDay++
                dates += TextInputDropDown.Item(
                        date.value.toLong(),
                        activity.getString(R.string.dialog_event_manual_date_tomorrow, date.formattedString),
                        tag = date.clone()
                )
            }
            // REMAINING SCHOOL DAYS OF THE CURRENT WEEK
            while (weekDay < 4) {
                date.stepForward(0, 0, 1) // step one day forward
                weekDay++
                dates += TextInputDropDown.Item(
                        date.value.toLong(),
                        activity.getString(R.string.dialog_event_manual_date_this_week, Week.getFullDayName(weekDay), date.formattedString),
                        tag = date.clone()
                )
            }
            // go to next week Monday
            date.stepForward(0, 0, -weekDay + 7)
            weekDay = 0
            // ALL SCHOOL DAYS OF THE NEXT WEEK
            while (weekDay < 4) {
                dates += TextInputDropDown.Item(
                        date.value.toLong(),
                        activity.getString(R.string.dialog_event_manual_date_next_week, Week.getFullDayName(weekDay), date.formattedString),
                        tag = date.clone()
                )
                date.stepForward(0, 0, 1) // step one day forward
                weekDay++
            }
            dates += TextInputDropDown.Item(
                    -1L,
                    activity.getString(R.string.dialog_event_manual_date_other)
            )
            dates
        }

        val dates = deferred.await()
        b.dateDropdown.clear().append(dates)

        defaultDate?.let {
            event.eventDate = it
            if (b.dateDropdown.select(it) == null)
                b.dateDropdown.select(TextInputDropDown.Item(
                        it.value.toLong(),
                        it.formattedString,
                        tag = it
                ))
        }

        editingEvent?.eventDate?.let {
            b.dateDropdown.select(TextInputDropDown.Item(
                    it.value.toLong(),
                    it.formattedString,
                    tag = it
            ))
        }

        defaultLesson?.displayDate?.let {
            b.dateDropdown.select(TextInputDropDown.Item(
                    it.value.toLong(),
                    it.formattedString,
                    tag = it
            ))
        }

        if (b.dateDropdown.selected == null) {
            b.dateDropdown.select(today.toLong())
        }

        b.dateDropdown.isEnabled = true

        b.dateDropdown.setOnChangeListener { item ->
            when {
                // next lesson with specified subject
                item.id < -1 -> {
                    val teamId = defaultLesson?.teamId ?: -1
                    val selectedLessonDate = defaultLesson?.date ?: Date.getToday()

                    when (teamId) {
                        -1L -> app.db.timetableDao().getNextWithSubject(profileId, selectedLessonDate, -item.id)
                        else -> app.db.timetableDao().getNextWithSubjectAndTeam(profileId, selectedLessonDate, -item.id, teamId)
                    }.observeOnce(activity, Observer {
                        val lessonDate = it?.displayDate ?: return@Observer
                        b.dateDropdown.select(TextInputDropDown.Item(
                                lessonDate.value.toLong(),
                                lessonDate.formattedString,
                                tag = lessonDate
                        ))
                        b.teamDropdown.select(it.displayTeamId)
                        b.subjectDropdown.select(it.displaySubjectId)
                        b.teacherDropdown.select(it.displayTeacherId)
                        defaultLoaded = false
                        loadHours(it.displayStartTime)
                    })
                    return@setOnChangeListener false
                }
                // custom date
                item.id == -1L -> {
                    MaterialDatePicker.Builder
                            .datePicker()
                            .setSelection((b.dateDropdown.selectedId?.let { Date.fromValue(it.toInt()) }
                                    ?: Date.getToday()).inMillis)
                            .build()
                            .apply {
                                addOnPositiveButtonClickListener {
                                    val dateSelected = Date.fromMillis(it)
                                    b.dateDropdown.select(TextInputDropDown.Item(
                                            dateSelected.value.toLong(),
                                            dateSelected.formattedString,
                                            tag = dateSelected
                                    ))
                                    loadHours()
                                }
                                show(this@EventManualDialog.activity.supportFragmentManager, "MaterialDatePicker")
                            }

                    return@setOnChangeListener false
                }
                // a specific date
                else -> {
                    b.dateDropdown.select(item)
                    loadHours()
                }
            }
            return@setOnChangeListener true
        }

        loadHours()
    }}

    private fun loadHours(defaultHour: Time? = null) {
        b.timeDropdown.isEnabled = false
        // get the selected date
        val date = b.dateDropdown.selectedId?.let { Date.fromValue(it.toInt()) } ?: return
        // get all lessons for selected date
        app.db.timetableDao().getForDate(profileId, date).observeOnce(activity, Observer { lessons ->
            val hours = mutableListOf<TextInputDropDown.Item>()
            // add All day time choice
            hours += TextInputDropDown.Item(
                    0L,
                    activity.getString(R.string.dialog_event_manual_all_day)
            )
            lessons.forEach { lesson ->
                if (lesson.type == Lesson.TYPE_NO_LESSONS) {
                    // indicate there are no lessons this day
                    hours += TextInputDropDown.Item(
                            -2L,
                            activity.getString(R.string.dialog_event_manual_no_lessons)
                    )
                    return@forEach
                }
                // create the lesson caption
                val text = listOfNotEmpty(
                        lesson.displayStartTime?.stringHM ?: "",
                        lesson.displaySubjectName?.let {
                            when {
                                lesson.type == Lesson.TYPE_CANCELLED
                                        || lesson.type == Lesson.TYPE_SHIFTED_SOURCE -> it.asStrikethroughSpannable()
                                lesson.type != Lesson.TYPE_NORMAL -> it.asItalicSpannable()
                                else -> it
                            }
                        } ?: ""
                )
                // add an item with LessonFull as the tag
                hours += TextInputDropDown.Item(
                        lesson.displayStartTime?.value?.toLong() ?: -1,
                        text.concat(" "),
                        tag = lesson
                )
            }
            b.timeDropdown.clear().append(hours)

            if (defaultLoaded) {
                b.timeDropdown.deselect()
                // select the TEAM_CLASS if possible
                b.teamDropdown.items.singleOrNull {
                    it.tag is Team && it.tag.type == Team.TYPE_CLASS
                }?.let {
                    b.teamDropdown.select(it)
                } ?: b.teamDropdown.deselect()

                // clear subject, teacher selection
                b.subjectDropdown.deselect()
                b.teacherDropdown.deselect()
            }
            else {
                defaultTime?.let {
                    event.startTime = it
                    if (b.timeDropdown.select(it) == null)
                        b.timeDropdown.select(TextInputDropDown.Item(
                                it.value.toLong(),
                                it.stringHM,
                                tag = it
                        ))
                }

                editingEvent?.let {
                    b.timeDropdown.select(it.startTime?.value?.toLong())
                }

                defaultLesson?.let {
                    b.timeDropdown.select(it.displayStartTime?.value?.toLong())
                }

                defaultHour?.let {
                    b.timeDropdown.select(it.value.toLong())
                }
            }
            defaultLoaded = true
            b.timeDropdown.isEnabled = true

            // attach a listener to time dropdown
            b.timeDropdown.setOnChangeListener { item ->
                when (item.id) {
                    // no lessons this day
                    -2L -> {
                        b.timeDropdown.deselect()
                        return@setOnChangeListener false
                    }

                    // custom start hour
                    -1L -> return@setOnChangeListener false

                    // selected a specific lesson
                    else -> {
                        if (item.tag is LessonFull) {
                            // update team, subject, teacher dropdowns,
                            // using the LessonFull from item tag
                            b.teamDropdown.deselect()
                            b.subjectDropdown.deselect()
                            b.teacherDropdown.deselect()
                            item.tag.displayTeamId?.let {
                                b.teamDropdown.select(it)
                            }
                            item.tag.displaySubjectId?.let {
                                b.subjectDropdown.select(it)
                            }
                            item.tag.displayTeacherId?.let {
                                b.teacherDropdown.select(it)
                            }
                        }
                    }
                }
                return@setOnChangeListener true
            }
        })
    }

    private fun saveEvent() {
        val date = b.dateDropdown.selected?.tag.instanceOfOrNull<Date>()
        val lesson = b.timeDropdown.selected?.tag.instanceOfOrNull<LessonFull>()
        val team = b.teamDropdown.selected?.tag.instanceOfOrNull<Team>()
        val share = b.shareSwitch.isChecked
        val type = b.typeDropdown.selected?.tag.instanceOfOrNull<EventType>()
        val topic = b.topic.text?.toString()
        val subject = b.subjectDropdown.selected?.tag.instanceOfOrNull<Subject>()
        val teacher = b.teacherDropdown.selected?.tag.instanceOfOrNull<Teacher>()

        b.teamDropdown.error = null
        b.typeDropdown.error = null
        b.topic.error = null

        var isError = false

        if (share && team == null) {
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
                lesson?.displayStartTime,
                topic,
                customColor ?: -1,
                type?.id?.toInt() ?: Event.TYPE_DEFAULT,
                true,
                teacher?.id ?: -1,
                subject?.id ?: -1,
                team?.id ?: -1
        )

        val metadataObject = Metadata(
                profileId,
                when (type?.id?.toInt()) {
                    Event.TYPE_HOMEWORK -> Metadata.TYPE_HOMEWORK
                    else -> Metadata.TYPE_EVENT
                },
                eventObject.id,
                true,
                true,
                editingEvent?.addedDate ?: System.currentTimeMillis()
        )

        finishAdding(eventObject, metadataObject)
    }

    private fun finishAdding(eventObject: Event, metadataObject: Metadata) {
        launch {
            withContext(Dispatchers.Default) {
                app.db.eventDao().add(eventObject)
                app.db.metadataDao().add(metadataObject)
            }
        }

        dialog.dismiss()
        Toast.makeText(app, R.string.saved, Toast.LENGTH_SHORT).show()
        (activity as MainActivity).reloadTarget()
    }

    private fun removeEvent() {
        launch {
            withContext(Dispatchers.Default) {
                app.db.eventDao().remove(editingEvent)
            }
        }

        removeEventDialog?.dismiss()
        dialog.dismiss()
        Toast.makeText(app, R.string.removed, Toast.LENGTH_SHORT).show()
        (activity as MainActivity).reloadTarget()
    }
}
