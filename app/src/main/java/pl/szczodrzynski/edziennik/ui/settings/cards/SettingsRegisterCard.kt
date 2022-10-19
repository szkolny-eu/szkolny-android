/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.settings.cards

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.szkolny.font.SzkolnyFont
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.enums.LoginType
import pl.szczodrzynski.edziennik.ext.after
import pl.szczodrzynski.edziennik.ui.dialogs.settings.*
import pl.szczodrzynski.edziennik.ui.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.settings.SettingsUtil

class SettingsRegisterCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        R.string.settings_card_register_title,
        items = getItems(),
        itemsMore = getItemsMore()
    )

    private fun getBellSync() =
        configGlobal.timetable.bellSyncDiff?.let {
            activity.getString(
                R.string.settings_register_bell_sync_subtext_format,
                (if (configGlobal.timetable.bellSyncMultiplier == -1) "-" else "+") + it.stringHMS
            )
        } ?: activity.getString(R.string.settings_register_bell_sync_subtext_disabled)

    private val sharedEventsItem by lazy {
        util.createPropertyItem(
            text = R.string.settings_register_shared_events_text,
            subText = R.string.settings_register_shared_events_subtext,
            icon = CommunityMaterial.Icon3.cmd_share_outline,
            value = app.profile.enableSharedEvents
        ) { _, value ->
            app.profile.enableSharedEvents = value
            app.profileSave()
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.event_sharing)
                .setMessage(
                    if (value)
                        R.string.settings_register_shared_events_dialog_enabled_text
                    else
                        R.string.settings_register_shared_events_dialog_disabled_text
                )
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    override fun getItems() = listOfNotNull(
        util.createActionItem(
            text = R.string.menu_agenda_config,
            icon = CommunityMaterial.Icon.cmd_calendar_outline
        ) {
            AgendaConfigDialog(activity, reloadOnDismiss = false).show()
        },

        util.createActionItem(
            text = R.string.menu_grades_config,
            icon = CommunityMaterial.Icon3.cmd_numeric_5_box_outline
        ) {
            GradesConfigDialog(activity, reloadOnDismiss = false).show()
        },

        util.createActionItem(
            text = R.string.menu_messages_config,
            icon = CommunityMaterial.Icon.cmd_calendar_outline
        ) {
            MessagesConfigDialog(activity, reloadOnDismiss = false).show()
        },

        util.createActionItem(
            text = R.string.menu_attendance_config,
            icon = CommunityMaterial.Icon.cmd_calendar_remove_outline
        ) {
            AttendanceConfigDialog(activity, reloadOnDismiss = false).show()
        },

        if (app.profile.archived)
            null
        else
            util.createPropertyItem(
                text = R.string.settings_register_allow_registration_text,
                subText = R.string.settings_register_allow_registration_subtext,
                icon = CommunityMaterial.Icon.cmd_account_circle_outline,
                value = app.profile.canShare,
                beforeChange = { item, value ->
                    if (app.profile.canShare == value)
                    // allow the switch to change - needed for util.refresh() to change the visual state
                        return@createPropertyItem true
                    val dialog =
                        RegistrationConfigDialog(activity, app.profile, onChangeListener = { enabled ->
                            if (item.isChecked == enabled)
                                return@RegistrationConfigDialog
                            item.isChecked = enabled
                            if (value) {
                                card.items.after(item, sharedEventsItem)
                            } else {
                                card.items.remove(sharedEventsItem)
                            }
                            util.refresh()
                        })
                    if (value)
                        dialog.showEnableDialog()
                    else
                        dialog.showDisableDialog()
                    false
                }
            ) { _, _ -> },

        if (app.profile.canShare)
            sharedEventsItem
        else
            null
    )

    override fun getItemsMore() = listOfNotNull(
        util.createActionItem(
            text = R.string.settings_register_bell_sync_text,
            icon = SzkolnyFont.Icon.szf_alarm_bell_outline,
            onClick = {
                BellSyncConfigDialog(activity, onChangeListener = {
                    it.subText = getBellSync()
                    util.refresh()
                }).show()
            }
        ).also {
            it.subText = getBellSync()
        },

        util.createPropertyItem(
            text = R.string.settings_register_count_in_seconds_text,
            subText = R.string.settings_register_count_in_seconds_subtext,
            icon = CommunityMaterial.Icon3.cmd_timer_outline,
            value = configGlobal.timetable.countInSeconds
        ) { _, it ->
            configGlobal.timetable.countInSeconds = it
        },

        if (app.profile.loginStoreType == LoginType.LIBRUS)
            util.createPropertyItem(
                text = R.string.settings_register_show_teacher_absences_text,
                icon = CommunityMaterial.Icon.cmd_account_arrow_right_outline,
                value = app.profile.getStudentData("showTeacherAbsences", true)
            ) { _, it ->
                app.profile.putStudentData("showTeacherAbsences", it)
                app.profileSave()
            }
        else
            null,

        if (App.devMode)
            util.createPropertyItem(
                text = R.string.settings_register_hide_sticks_from_old,
                icon = CommunityMaterial.Icon3.cmd_numeric_1_box_outline,
                value = configProfile.grades.hideSticksFromOld
            ) { _, it ->
                configProfile.grades.hideSticksFromOld = it
            }
        else
            null
    )
}
