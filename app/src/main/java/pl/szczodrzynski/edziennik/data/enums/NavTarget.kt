/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.enums

import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.szkolny.font.SzkolnyFont
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.agenda.AgendaFragment
import pl.szczodrzynski.edziennik.ui.announcements.AnnouncementsFragment
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceFragment
import pl.szczodrzynski.edziennik.ui.behaviour.BehaviourFragment
import pl.szczodrzynski.edziennik.ui.debug.DebugFragment
import pl.szczodrzynski.edziennik.ui.debug.LabFragment
import pl.szczodrzynski.edziennik.ui.feedback.FeedbackFragment
import pl.szczodrzynski.edziennik.ui.grades.GradesListFragment
import pl.szczodrzynski.edziennik.ui.grades.editor.GradesEditorFragment
import pl.szczodrzynski.edziennik.ui.home.HomeFragment
import pl.szczodrzynski.edziennik.ui.homework.HomeworkFragment
import pl.szczodrzynski.edziennik.ui.messages.compose.MessagesComposeFragment
import pl.szczodrzynski.edziennik.ui.messages.list.MessagesFragment
import pl.szczodrzynski.edziennik.ui.messages.single.MessageFragment
import pl.szczodrzynski.edziennik.ui.notes.NotesFragment
import pl.szczodrzynski.edziennik.ui.notifications.NotificationsListFragment
import pl.szczodrzynski.edziennik.ui.settings.ProfileManagerFragment
import pl.szczodrzynski.edziennik.ui.settings.SettingsFragment
import pl.szczodrzynski.edziennik.ui.settings.contributors.ContributorsFragment
import pl.szczodrzynski.edziennik.ui.teachers.TeachersListFragment
import pl.szczodrzynski.edziennik.ui.template.TemplateFragment
import pl.szczodrzynski.edziennik.ui.timetable.TimetableFragment
import pl.szczodrzynski.edziennik.ui.webpush.WebPushFragment
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem

enum class NavTarget(
    val id: Int,
    val fragmentClass: Class<out Fragment>?,
    val location: NavTargetLocation = NavTargetLocation.NOWHERE,
    @StringRes
    val nameRes: Int,
    @StringRes
    val descriptionRes: Int? = null,
    @StringRes
    val titleRes: Int? = null,
    val icon: IIcon? = null,
    val popTo: NavTarget? = null,
    val badgeType: MetadataType? = null,
    val featureType: FeatureType? = null,
    val devModeOnly: Boolean = false,
) {
    HOME(
        id = 1,
        fragmentClass = HomeFragment::class.java,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_home_page,
        titleRes = R.string.app_name,
        icon = CommunityMaterial.Icon2.cmd_home_outline,
    ),
    TIMETABLE(
        id = 11,
        fragmentClass = TimetableFragment::class.java,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_timetable,
        icon = CommunityMaterial.Icon3.cmd_timetable,
        popTo = HOME,
        badgeType = MetadataType.LESSON_CHANGE,
        featureType = FeatureType.TIMETABLE,
    ),
    AGENDA(
        id = 12,
        fragmentClass = AgendaFragment::class.java,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_agenda,
        icon = CommunityMaterial.Icon.cmd_calendar_outline,
        popTo = HOME,
        badgeType = MetadataType.EVENT,
        featureType = FeatureType.AGENDA,
    ),
    GRADES(
        id = 13,
        fragmentClass = GradesListFragment::class.java,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_grades,
        icon = CommunityMaterial.Icon3.cmd_numeric_5_box_outline,
        popTo = HOME,
        badgeType = MetadataType.GRADE,
        featureType = FeatureType.GRADES,
    ),
    MESSAGES(
        id = 17,
        fragmentClass = MessagesFragment::class.java,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_messages,
        icon = CommunityMaterial.Icon.cmd_email_outline,
        popTo = HOME,
        badgeType = MetadataType.MESSAGE,
        featureType = FeatureType.MESSAGES_INBOX,
    ),
    HOMEWORK(
        id = 14,
        fragmentClass = HomeworkFragment::class.java,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_homework,
        icon = SzkolnyFont.Icon.szf_notebook_outline,
        popTo = HOME,
        badgeType = MetadataType.HOMEWORK,
        featureType = FeatureType.HOMEWORK,
    ),
    BEHAVIOUR(
        id = 15,
        fragmentClass = BehaviourFragment::class.java,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_notices,
        icon = CommunityMaterial.Icon.cmd_emoticon_outline,
        popTo = HOME,
        badgeType = MetadataType.NOTICE,
        featureType = FeatureType.BEHAVIOUR,
    ),
    ATTENDANCE(
        id = 16,
        fragmentClass = AttendanceFragment::class.java,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_attendance,
        icon = CommunityMaterial.Icon.cmd_calendar_remove_outline,
        popTo = HOME,
        badgeType = MetadataType.ATTENDANCE,
        featureType = FeatureType.ATTENDANCE,
    ),
    ANNOUNCEMENTS(
        id = 18,
        fragmentClass = AnnouncementsFragment::class.java,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_announcements,
        icon = CommunityMaterial.Icon.cmd_bullhorn_outline,
        popTo = HOME,
        badgeType = MetadataType.ANNOUNCEMENT,
        featureType = FeatureType.ANNOUNCEMENTS,
    ),
    NOTES(
        id = 23,
        fragmentClass = NotesFragment::class.java,
        location = NavTargetLocation.DRAWER_MORE,
        nameRes = R.string.menu_notes,
        icon = CommunityMaterial.Icon3.cmd_text_box_multiple_outline,
    ),
    TEACHERS(
        id = 22,
        fragmentClass = TeachersListFragment::class.java,
        location = NavTargetLocation.DRAWER_MORE,
        nameRes = R.string.menu_teachers,
        icon = CommunityMaterial.Icon3.cmd_shield_account_outline,
    ),
    NOTIFICATIONS(
        id = 20,
        fragmentClass = NotificationsListFragment::class.java,
        location = NavTargetLocation.DRAWER_BOTTOM,
        nameRes = R.string.menu_notifications,
        icon = CommunityMaterial.Icon.cmd_bell_ring_outline,
        popTo = HOME,
    ),
    SETTINGS(
        id = 101,
        fragmentClass = SettingsFragment::class.java,
        location = NavTargetLocation.DRAWER_BOTTOM,
        nameRes = R.string.menu_settings,
        icon = CommunityMaterial.Icon.cmd_cog_outline,
    ),
    LAB(
        id = 1000,
        fragmentClass = LabFragment::class.java,
        location = NavTargetLocation.DRAWER_BOTTOM,
        nameRes = R.string.menu_lab,
        icon = CommunityMaterial.Icon2.cmd_flask_outline,
        popTo = HOME,
        devModeOnly = true,
    ),
    TEMPLATE(
        id = 1001,
        fragmentClass = TemplateFragment::class.java,
        location = NavTargetLocation.DRAWER_BOTTOM,
        nameRes = R.string.menu_template,
        icon = CommunityMaterial.Icon.cmd_code_braces,
        popTo = HOME,
        devModeOnly = true,
    ),
    PROFILE_ADD(
        id = 200,
        fragmentClass = null,
        location = NavTargetLocation.PROFILE_LIST,
        nameRes = R.string.menu_add_new_profile,
        descriptionRes = R.string.drawer_add_new_profile_desc,
        icon = CommunityMaterial.Icon3.cmd_plus,
    ),
    PROFILE_MANAGER(
        id = 203,
        fragmentClass = ProfileManagerFragment::class.java,
        location = NavTargetLocation.NOWHERE,
        nameRes = R.string.menu_manage_profiles,
        titleRes = R.string.title_profile_manager,
        descriptionRes = R.string.drawer_manage_profiles_desc,
        icon = CommunityMaterial.Icon.cmd_account_group,
    ),
    PROFILE_MARK_AS_READ(
        id = 204,
        fragmentClass = null,
        location = NavTargetLocation.PROFILE_LIST,
        nameRes = R.string.menu_mark_everything_as_read,
        icon = CommunityMaterial.Icon.cmd_eye_check_outline,
    ),
    PROFILE_SYNC_ALL(
        id = 201,
        fragmentClass = null,
        location = NavTargetLocation.PROFILE_LIST,
        nameRes = R.string.menu_sync_all,
        icon = CommunityMaterial.Icon.cmd_download_outline,
    ),
    FEEDBACK(
        id = 120,
        fragmentClass = FeedbackFragment::class.java,
        location = NavTargetLocation.BOTTOM_SHEET,
        nameRes = R.string.menu_feedback,
        icon = CommunityMaterial.Icon2.cmd_help_circle_outline,
    ),
    DEBUG(
        id = 102,
        fragmentClass = DebugFragment::class.java,
        location = NavTargetLocation.BOTTOM_SHEET,
        nameRes = R.string.menu_debug,
        icon = CommunityMaterial.Icon.cmd_android_debug_bridge,
        devModeOnly = true,
    ),
    GRADES_EDITOR(
        id = 501,
        fragmentClass = GradesEditorFragment::class.java,
        nameRes = R.string.menu_grades_editor,
    ),
    MESSAGE(
        id = 503,
        fragmentClass = MessageFragment::class.java,
        nameRes = R.string.menu_message,
        popTo = MESSAGES,
    ),
    MESSAGE_COMPOSE(
        id = 504,
        fragmentClass = MessagesComposeFragment::class.java,
        nameRes = R.string.menu_message_compose,
    ),
    WEB_PUSH(
        id = 140,
        fragmentClass = WebPushFragment::class.java,
        nameRes = R.string.menu_web_push,
    ),
    CONTRIBUTORS(
        id = 150,
        fragmentClass = ContributorsFragment::class.java,
        nameRes = R.string.contributors,
    );

    companion object {
        fun getDefaultConfig() = setOf(
            HOME,
            TIMETABLE,
            AGENDA,
            GRADES,
            MESSAGES,
            HOMEWORK,
            SETTINGS
        )

        fun getById(id: Int) = NavTarget.entries.first { it.id == id }
    }

    fun toBottomSheetItem(activity: MainActivity) =
        BottomSheetPrimaryItem(isContextual = false).also {
            it.titleRes = this.nameRes
            if (this.icon != null)
                it.iconicsIcon = this.icon
            it.onClickListener = View.OnClickListener {
                activity.navigate(navTarget = this)
            }
        }
}
