/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-13.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog

class ProfileRemoveDialog(
    activity: AppCompatActivity,
    val profileId: Int,
    val profileName: String,
    val noProfileRemoval: Boolean = false,
    val onRemove: (() -> Unit)? = null,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog<Any>(activity, onShowListener, onDismissListener) {

    override val TAG = "ProfileRemoveDialog"

    override fun getTitleRes() = R.string.profile_menu_remove_confirm
    override fun getMessageFormat() =
        R.string.profile_menu_remove_confirm_text_format to listOf(
            profileName,
            profileName
        )

    override fun isCancelable() = false
    override fun getPositiveButtonText() = R.string.remove
    override fun getNeutralButtonText() = R.string.cancel

    override suspend fun onPositiveClick(): Boolean {
        withContext(Dispatchers.Default) {
            val profileObject = app.db.profileDao().getByIdNow(profileId) ?: return@withContext
            app.db.announcementDao().clear(profileId)
            app.db.attendanceDao().clear(profileId)
            app.db.attendanceTypeDao().clear(profileId)
            app.db.classroomDao().clear(profileId)
            app.db.endpointTimerDao().clear(profileId)
            app.db.eventDao().clear(profileId)
            app.db.eventTypeDao().clear(profileId)
            app.db.gradeCategoryDao().clear(profileId)
            app.db.gradeDao().clear(profileId)
            app.db.lessonRangeDao().clear(profileId)
            app.db.librusLessonDao().clear(profileId)
            app.db.luckyNumberDao().clear(profileId)
            app.db.messageDao().clear(profileId)
            app.db.messageRecipientDao().clear(profileId)
            app.db.noticeDao().clear(profileId)
            app.db.noticeTypeDao().clear(profileId)
            app.db.notificationDao().clear(profileId)
            app.db.subjectDao().clear(profileId)
            app.db.teacherAbsenceDao().clear(profileId)
            app.db.teacherAbsenceTypeDao().clear(profileId)
            app.db.teacherDao().clear(profileId)
            app.db.teamDao().clear(profileId)
            app.db.timetableDao().clear(profileId)

            app.db.metadataDao().deleteAll(profileId)

            if (noProfileRemoval)
                return@withContext

            app.db.configDao().clear(profileId)

            val loginStoreId = profileObject.loginStoreId
            val profilesUsingLoginStore =
                app.db.profileDao().getIdsByLoginStoreIdNow(loginStoreId)
            if (profilesUsingLoginStore.size == 1) {
                app.db.loginStoreDao().remove(loginStoreId)
            }
            app.db.profileDao().remove(profileId)

            if (App.profileId == profileId) {
                app.profileLoadLast { }
            }
        }

        if (activity is MainActivity)
            activity.reloadTarget()

        Toast.makeText(activity, R.string.dialog_profile_remove_success, Toast.LENGTH_LONG).show()
        onRemove?.invoke()

        return DISMISS
    }
}
