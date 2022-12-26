/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-12.
 */

package pl.szczodrzynski.edziennik.ui.event

import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.config.AppData
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskAllFinishedEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskErrorEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskFinishedEvent
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Subject
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.databinding.DialogEventManualV2Binding
import pl.szczodrzynski.edziennik.ext.JsonObject
import pl.szczodrzynski.edziennik.ext.appendView
import pl.szczodrzynski.edziennik.ext.getStudentData
import pl.szczodrzynski.edziennik.ext.onChange
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.removeFromParent
import pl.szczodrzynski.edziennik.ext.setText
import pl.szczodrzynski.edziennik.ext.setTintColor
import pl.szczodrzynski.edziennik.ui.dialogs.base.BindingDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.RegistrationConfigDialog
import pl.szczodrzynski.edziennik.ui.views.TimeDropdown.Companion.DISPLAY_LESSONS
import pl.szczodrzynski.edziennik.utils.Anim
import pl.szczodrzynski.edziennik.utils.html.BetterHtml
import pl.szczodrzynski.edziennik.utils.managers.TextStylingManager.HtmlMode.SIMPLE
import pl.szczodrzynski.edziennik.utils.managers.TextStylingManager.StylingConfigBase
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

// TODO: 2021-10-19 rewrite to the new dialog style
class EventManualDialog(
    activity: AppCompatActivity,
    private val profileId: Int,
    private val defaultLesson: LessonFull? = null,
    private val defaultDate: Date? = null,
    private val defaultTime: Time? = null,
    private val defaultType: Long? = null,
    private val editingEvent: EventFull? = null,
    private val onSaveListener: ((event: EventFull?) -> Unit)? = null,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BindingDialog<DialogEventManualV2Binding>(activity, onShowListener, onDismissListener) {

    override val TAG = "EventManualDialog"

    override fun getTitleRes() = R.string.dialog_event_manual_title
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogEventManualV2Binding.inflate(layoutInflater)

    override fun isCancelable() = false
    override fun getPositiveButtonText() = R.string.save
    override fun getNeutralButtonText() = if (editingEvent != null) R.string.remove else null
    override fun getNegativeButtonText() = R.string.cancel

    private lateinit var profile: Profile
    private lateinit var stylingConfig: StylingConfigBase

    private val textStylingManager
        get() = app.textStylingManager

    private var customColor: Int? = null
    private val editingShared = editingEvent?.sharedBy != null
    private val editingOwn = editingEvent?.sharedBy == "self"
    private var removeEventDialog: AlertDialog? = null

    private val api by lazy {
        SzkolnyApi(app)
    }

    private var enqueuedWeekDialog: AlertDialog? = null
    private var enqueuedWeekStart = Date.getToday()

    private var progressDialog: AlertDialog? = null

    override suspend fun onPositiveClick(): Boolean {
        saveEvent()
        return NO_DISMISS
    }

    override suspend fun onNeutralClick(): Boolean {
        showRemoveEventDialog()
        return NO_DISMISS
    }

    override suspend fun onBeforeShow(): Boolean {
        EventBus.getDefault().register(this@EventManualDialog)
        return true
    }

    override fun onDismiss() {
        EventBus.getDefault().unregister(this@EventManualDialog)
        enqueuedWeekDialog?.dismiss()
        progressDialog?.dismiss()
    }

    override suspend fun onShow() {
        val data = withContext(Dispatchers.IO) {
            val profile = app.db.profileDao().getByIdSuspend(profileId) ?: return@withContext null
            AppData.get(profile.loginStoreType)
        }
        if (data?.uiConfig?.eventManualShowSubjectDropdown == true) {
            b.subjectDropdownLayout.removeFromParent()
            b.timeDropdownLayout.appendView(b.subjectDropdownLayout)
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

        textStylingManager.attachToField(
            activity = activity,
            textLayout = b.topicLayout,
            textEdit = b.topic,
            onShowListener = onShowListener,
            onDismissListener = onDismissListener,
        )

        stylingConfig = StylingConfigBase(editText = b.topic, htmlMode = SIMPLE)

        updateShareText()
        b.shareSwitch.onChange { _, isChecked ->
            updateShareText(isChecked)
        }

        loadLists()

        val shareByDefault = app.profile.config.shareByDefault && profile.canShare

        b.shareSwitch.isChecked = editingShared || editingEvent == null && shareByDefault
        b.shareSwitch.isEnabled = !editingShared || editingOwn
    }

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

    private fun syncTimetable(date: Date) {
        if (enqueuedWeekDialog != null) {
            return
        }
        if (app.profile.getStudentData("timetableNotPublic", false)) {
            return
        }
        val weekStart = date.weekStart
        enqueuedWeekDialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.please_wait)
                .setMessage(R.string.timetable_syncing_text)
                .setCancelable(false)
                .show()

        enqueuedWeekStart = weekStart

        EdziennikTask.syncProfile(
                profileId = profileId,
                featureTypes = setOf(FeatureType.TIMETABLE),
                arguments = JsonObject(
                        "weekStart" to weekStart.stringY_m_d
                )
        ).enqueue(activity)
    }

    private fun showSharingProgressDialog() {
        if (progressDialog != null) {
            return
        }

        progressDialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.please_wait)
                .setMessage(R.string.event_sharing_text)
                .setCancelable(false)
                .show()
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTaskFinishedEvent(event: ApiTaskFinishedEvent) {
        if (event.profileId == profileId) {
            enqueuedWeekDialog?.dismiss()
            enqueuedWeekDialog = null
            progressDialog?.dismiss()
            launch {
                b.timeDropdown.loadItems()
                b.timeDropdown.selectDefault(editingEvent?.time)
                b.timeDropdown.selectDefault(defaultLesson?.displayStartTime ?: defaultTime)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTaskAllFinishedEvent(event: ApiTaskAllFinishedEvent) {
        enqueuedWeekDialog?.dismiss()
        enqueuedWeekDialog = null
        progressDialog?.dismiss()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTaskErrorEvent(event: ApiTaskErrorEvent) {
        enqueuedWeekDialog?.dismiss()
        enqueuedWeekDialog = null
        progressDialog?.dismiss()
    }

    private suspend fun loadLists() {
        val profile = withContext(Dispatchers.Default) {
            app.db.profileDao().getByIdNow(profileId)
        }
        if (profile == null) {
            Toast.makeText(activity, R.string.event_manual_no_profile, Toast.LENGTH_SHORT).show()
            return
        }
        this@EventManualDialog.profile = profile

        with (b.dateDropdown) {
            db = app.db
            profileId = this@EventManualDialog.profileId
            showWeekDays = false
            showDays = true
            showOtherDate = true
            defaultLesson?.let {
                nextLessonSubjectId = it.displaySubjectId
                nextLessonSubjectName = it.displaySubjectName
                nextLessonTeamId = it.displayTeamId
            }
            loadItems()
            selectDefault(editingEvent?.date)
            selectDefault(defaultLesson?.displayDate ?: defaultDate)
            onDateSelected = { date, lesson ->
                b.timeDropdown.deselect()
                b.timeDropdown.lessonsDate = date
                this@EventManualDialog.launch {
                    if (!b.timeDropdown.loadItems())
                        syncTimetable(date)
                    lesson?.displayStartTime?.let { b.timeDropdown.selectTime(it) }
                    lesson?.displaySubjectId?.let { b.subjectDropdown.selectSubject(it) } ?: b.subjectDropdown.deselect()
                    lesson?.displayTeacherId?.let { b.teacherDropdown.selectTeacher(it) } ?: b.teacherDropdown.deselect()
                    lesson?.displayTeamId?.let { b.teamDropdown.selectTeam(it) } ?: b.teamDropdown.selectTeamClass()
                }
            }
        }

        with (b.timeDropdown) {
            db = app.db
            profileId = this@EventManualDialog.profileId
            showAllDay = true
            showCustomTime = true
            lessonsDate = b.dateDropdown.getSelected() as? Date ?: Date.getToday()
            displayMode = DISPLAY_LESSONS
            if (!loadItems())
                syncTimetable(lessonsDate ?: Date.getToday())
            selectDefault(editingEvent?.time)
            if (editingEvent != null && editingEvent.time == null)
                select(0L)
            selectDefault(defaultLesson?.displayStartTime ?: defaultTime)
            onLessonSelected = { lesson ->
                lesson.displaySubjectId?.let { b.subjectDropdown.selectSubject(it) } ?: b.subjectDropdown.deselect()
                lesson.displayTeacherId?.let { b.teacherDropdown.selectTeacher(it) } ?: b.teacherDropdown.deselect()
                lesson.displayTeamId?.let { b.teamDropdown.selectTeam(it) } ?: b.teamDropdown.selectTeamClass()
            }
        }

        with (b.teamDropdown) {
            db = app.db
            profileId = this@EventManualDialog.profileId
            showNoTeam = true
            loadItems()
            selectTeamClass()
            selectDefault(editingEvent?.teamId)
            selectDefault(defaultLesson?.displayTeamId)
        }

        with (b.subjectDropdown) {
            db = app.db
            profileId = this@EventManualDialog.profileId
            showNoSubject = true
            showCustomSubject = false
            loadItems()
            selectDefault(editingEvent?.subjectId)
            selectDefault(defaultLesson?.displaySubjectId)
        }

        with (b.teacherDropdown) {
            db = app.db
            profileId = this@EventManualDialog.profileId
            showNoTeacher = true
            loadItems()
            selectDefault(editingEvent?.teacherId)
            selectDefault(defaultLesson?.displayTeacherId)
        }

        with (b.typeDropdown) {
            db = app.db
            profileId = this@EventManualDialog.profileId
            loadItems()
            selectDefault(editingEvent?.type)
            selectDefault(defaultType)

            onTypeSelected = {
                b.typeColor.background.setTintColor(it.color)
                customColor = null
            }
        }

        // copy data from event being edited
        editingEvent?.let {
            b.topic.setText(BetterHtml.fromHtml(activity, it.topic, nl2br = true))
            if (it.color != -1)
                customColor = it.color
        }

        b.typeColor.background.setTintColor(
            customColor
                ?: b.typeDropdown.getSelected()?.color
                ?: Event.COLOR_DEFAULT
        )

        // copy IDs from the LessonFull
        defaultLesson?.let {
            b.teamDropdown.select(it.displayTeamId)
        }

        b.typeColor.onClick {
            val currentColor = customColor
                ?: b.typeDropdown.getSelected()?.color
                ?: Event.COLOR_DEFAULT
            val colorPickerDialog = ColorPickerDialog.newBuilder()
                    .setColor(currentColor)
                    .create()
            colorPickerDialog.setColorPickerDialogListener(
                    object : ColorPickerDialogListener {
                        override fun onDialogDismissed(dialogId: Int) {}
                        override fun onColorSelected(dialogId: Int, color: Int) {
                            b.typeColor.background.setTintColor(color)
                            customColor = color
                        }
                    })
            colorPickerDialog.show(activity.supportFragmentManager, "color-picker-dialog")
        }
    }

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
        val timeSelected = b.timeDropdown.getSelected()
        val team = b.teamDropdown.getSelected()
        val type = b.typeDropdown.getSelected()
        val topic = b.topic.text?.toString()
        val subject = b.subjectDropdown.getSelected() as? Subject
        val teacher = b.teacherDropdown.getSelected()

        val share = b.shareSwitch.isChecked

        if (share && !profile.canShare) {
            RegistrationConfigDialog(activity, profile, onChangeListener = { enabled ->
                if (enabled)
                    saveEvent()
            }).showEventShareDialog()
            return
        }

        b.dateDropdown.error = null
        b.teamDropdown.error = null
        b.typeDropdown.error = null
        b.topic.error = null

        var isError = false

        if (date == null) {
            b.dateDropdown.error = app.getString(R.string.dialog_event_manual_date_choose)
            b.dateDropdown.requestFocus()
            isError = true
        }

        if (timeSelected !is Pair<*, *> && timeSelected != 0L) {
            b.timeDropdown.error = app.getString(R.string.dialog_event_manual_time_choose)
            if (!isError) b.timeDropdown.parent.requestChildFocus(b.timeDropdown, b.timeDropdown)
            isError = true
        }

        if (share && team == null) {
            b.teamDropdown.error = app.getString(R.string.dialog_event_manual_team_choose)
            if (!isError) b.teamDropdown.parent.requestChildFocus(b.teamDropdown, b.teamDropdown)
            isError = true
        }

        if (type == null) {
            b.typeDropdown.error = app.getString(R.string.dialog_event_manual_type_choose)
            if (!isError) b.typeDropdown.requestFocus()
            isError = true
        }

        if (topic.isNullOrBlank()) {
            b.topic.error = app.getString(R.string.dialog_event_manual_topic_choose)
            if (!isError) b.topic.requestFocus()
            isError = true
        }

        val startTime = if (timeSelected == 0L)
            null
        else
            (timeSelected as? Pair<*, *>)?.first as? Time

        if (isError) return
        date ?: return
        topic ?: return

        val id = System.currentTimeMillis()

        val topicHtml = textStylingManager.getHtmlText(stylingConfig)
        val eventObject = Event(
                profileId = profileId,
                id = editingEvent?.id ?: id,
                date = date,
                time = startTime,
                topic = topicHtml,
                color = customColor,
                type = type?.id ?: Event.TYPE_DEFAULT,
                teacherId = teacher?.id ?: -1,
                subjectId = subject?.id ?: -1,
                teamId = team?.id ?: -1,
                addedDate = editingEvent?.addedDate ?: System.currentTimeMillis()
        ).also {
            it.addedManually = true
        }

        val metadataObject = Metadata(
                profileId,
                when (type?.id) {
                    Event.TYPE_HOMEWORK -> MetadataType.HOMEWORK
                    else -> MetadataType.EVENT
                },
                eventObject.id,
                true,
                true
        )

        launch {
            val profile = app.db.profileDao().getByIdNow(profileId)

            if (!share && !editingShared) {
                //Toast.makeText(activity, R.string.event_manual_saving, Toast.LENGTH_SHORT).show()
                finishAdding(eventObject, metadataObject)
            }
            else if (editingShared && !editingOwn) {
                Toast.makeText(activity, "Opcja edycji wydarzeń innych uczniów nie została jeszcze zaimplementowana.", Toast.LENGTH_LONG).show()
                // TODO
            }
            else if (!share && editingShared) {
                showSharingProgressDialog()

                eventObject.apply {
                    sharedBy = null
                    sharedByName = profile?.studentNameLong
                }

                api.runCatching(activity) {
                    unshareEvent(eventObject)
                } ?: run {
                    progressDialog?.dismiss()
                    return@launch
                }

                eventObject.sharedByName = null
                finishAdding(eventObject, metadataObject)
            }
            else if (share) {
                showSharingProgressDialog()

                eventObject.apply {
                    sharedBy = profile?.userCode
                    sharedByName = profile?.studentNameLong
                    addedDate = System.currentTimeMillis()
                }

                api.runCatching(activity) {
                    shareEvent(eventObject.withMetadata(metadataObject))
                } ?: run {
                    progressDialog?.dismiss()
                    return@launch
                }

                eventObject.sharedBy = "self"
                finishAdding(eventObject, metadataObject)
            }
            else {
                Toast.makeText(activity, "Unknown action :(", Toast.LENGTH_SHORT).show()
            }
            progressDialog?.dismiss()
        }
    }

    private fun removeEvent() {
        launch {
            if (editingShared && editingOwn) {
                // unshare + remove own event
                showRemovingProgressDialog()

                api.runCatching(activity) {
                    unshareEvent(editingEvent!!)
                } ?: run {
                    progressDialog?.dismiss()
                    return@launch
                }

                finishRemoving()
            } else if (editingShared && !editingOwn) {
                // remove + blacklist somebody's event
                Toast.makeText(activity, "Nie zaimplementowana opcja :(", Toast.LENGTH_SHORT).show()
                // TODO
            } else {
                // remove event
                //Toast.makeText(activity, R.string.event_manual_remove, Toast.LENGTH_SHORT).show()
                finishRemoving()
            }
            progressDialog?.dismiss()
        }
    }

    private fun finishAdding(eventObject: Event, metadataObject: Metadata) {
        launch {
            withContext(Dispatchers.Default) {
                app.db.eventDao().upsert(eventObject)
                app.db.metadataDao().add(metadataObject)
            }
        }

        onSaveListener?.invoke(eventObject.withMetadata(metadataObject).also {
            it.subjectLongName = (b.subjectDropdown.getSelected() as? Subject)?.longName
            it.teacherName = b.teacherDropdown.getSelected()?.fullName
            it.teamName = b.teamDropdown.getSelected()?.name
            it.typeName = b.typeDropdown.getSelected()?.name
        })
        dismiss()
        Toast.makeText(activity, R.string.saved, Toast.LENGTH_SHORT).show()
    }
    private fun finishRemoving() {
        editingEvent ?: return
        launch {
            withContext(Dispatchers.Default) {
                app.db.eventDao().remove(editingEvent)
            }
        }

        removeEventDialog?.dismiss()
        onSaveListener?.invoke(null)
        dismiss()
        Toast.makeText(activity, R.string.removed, Toast.LENGTH_SHORT).show()
    }
}
