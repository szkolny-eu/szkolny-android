/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-13.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogLessonDetailsBinding
import pl.szczodrzynski.edziennik.utils.models.Notification
import java.util.*
import kotlin.coroutines.CoroutineContext

class ProfileRemoveDialog(
        val activity: MainActivity,
        val profileId: Int,
        val profileName: String
) : CoroutineScope {
    companion object {
        private const val TAG = "ProfileRemoveDialog"
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val app by lazy { activity.application as App }
    private lateinit var b: DialogLessonDetailsBinding
    private lateinit var dialog: AlertDialog

    init { run {
        job = Job()

        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.profile_menu_remove_confirm)
                .setMessage(activity.getString(R.string.profile_menu_remove_confirm_text_format, profileName, profileName))
                .setPositiveButton(R.string.remove) { _, _ ->
                    removeProfile()
                }
                .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setCancelable(false)
                .show()
    }}

    private fun removeProfile() { launch {
        val deferred = async(Dispatchers.Default) {
            val profileObject = app.db.profileDao().getByIdNow(profileId) ?: return@async
            app.db.announcementDao().clear(profileId)
            app.db.attendanceDao().clear(profileId)
            app.db.eventDao().clear(profileId)
            app.db.eventTypeDao().clear(profileId)
            app.db.gradeDao().clear(profileId)
            app.db.gradeCategoryDao().clear(profileId)
            app.db.lessonDao().clear(profileId)
            app.db.lessonChangeDao().clear(profileId)
            app.db.luckyNumberDao().clear(profileId)
            app.db.noticeDao().clear(profileId)
            app.db.subjectDao().clear(profileId)
            app.db.teacherDao().clear(profileId)
            app.db.teamDao().clear(profileId)
            app.db.messageRecipientDao().clear(profileId)
            app.db.messageDao().clear(profileId)
            app.db.endpointTimerDao().clear(profileId)
            app.db.attendanceTypeDao().clear(profileId)
            app.db.classroomDao().clear(profileId)
            app.db.lessonRangeDao().clear(profileId)
            app.db.noticeTypeDao().clear(profileId)
            app.db.teacherAbsenceDao().clear(profileId)
            app.db.teacherAbsenceTypeDao().clear(profileId)
            app.db.timetableDao().clear(profileId)

            val loginStoreId = profileObject.loginStoreId
            val profilesUsingLoginStore = app.db.profileDao().getIdsByLoginStoreIdNow(loginStoreId)
            if (profilesUsingLoginStore.size == 1) {
                app.db.loginStoreDao().remove(loginStoreId)
            }
            app.db.profileDao().remove(profileId)
            app.db.metadataDao().deleteAll(profileId)

            val toRemove = ArrayList<Notification>()
            for (notification in app.appConfig.notifications) {
                if (notification.profileId == profileId) {
                    toRemove.add(notification)
                }
            }
            app.appConfig.notifications.removeAll(toRemove)

            app.profile = null
            App.profileId = -1

            app.profileLoadById(app.profileLastId())
        }
        deferred.await()
        dialog.dismiss()
        activity.reloadTarget()
        Toast.makeText(activity, R.string.dialog_profile_remove_success, Toast.LENGTH_LONG).show()
    }}
}