/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.settings.cards

import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.szkolny.font.SzkolnyFont
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.ext.after
import pl.szczodrzynski.edziennik.ext.getStudentData
import pl.szczodrzynski.edziennik.ext.hasUIFeature
import pl.szczodrzynski.edziennik.ext.set
import pl.szczodrzynski.edziennik.ui.dialogs.settings.AgendaConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.AttendanceConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.BellSyncConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.GradesConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.MessagesConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.RegistrationConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.TimetableConfigDialog
import pl.szczodrzynski.edziennik.ui.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.settings.SettingsUtil

class SettingsRegisterCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        R.string.settings_card_register_title,
        items = ::getItems,
        itemsMore = ::getItemsMore,
    )

    private fun getBellSync() =
        configGlobal.timetable.bellSyncDiff?.let {
            activity.getString(
                R.string.settings_register_bell_sync_subtext_format,
                (if (configGlobal.timetable.bellSyncMultiplier == -1) "-" else "+") + it.stringHMS
            )
        } ?: activity.getString(R.string.settings_register_bell_sync_subtext_disabled)

    private val sharedEventsDefaultItem by lazy {
        util.createPropertyItem(
            text = R.string.settings_register_share_by_default_text,
            subText = R.string.settings_register_share_by_default_subtext,
            icon = CommunityMaterial.Icon3.cmd_toggle_switch_outline,
            value = configProfile.shareByDefault
        ) { _, value ->
            configProfile.shareByDefault = value
        }
    }

    override fun getItems(card: MaterialAboutCard) = listOfNotNull(
        util.createActionItem(
            text = R.string.menu_timetable_config,
            icon = CommunityMaterial.Icon3.cmd_timetable
        ) {
            TimetableConfigDialog(activity, reloadOnDismiss = false).show()
        }.takeIf { app.profile.hasUIFeature(FeatureType.TIMETABLE) },

        util.createActionItem(
            text = R.string.menu_agenda_config,
            icon = CommunityMaterial.Icon.cmd_calendar_outline
        ) {
            AgendaConfigDialog(activity, reloadOnDismiss = false).show()
        }.takeIf { app.profile.hasUIFeature(FeatureType.AGENDA) },

        util.createActionItem(
            text = R.string.menu_grades_config,
            icon = CommunityMaterial.Icon3.cmd_numeric_5_box_outline
        ) {
            GradesConfigDialog(activity, reloadOnDismiss = false).show()
        }.takeIf { app.profile.hasUIFeature(FeatureType.GRADES) },

        util.createActionItem(
            text = R.string.menu_messages_config,
            icon = CommunityMaterial.Icon.cmd_email_outline
        ) {
            MessagesConfigDialog(activity, reloadOnDismiss = false).show()
        }.takeIf {
            app.profile.hasUIFeature(FeatureType.MESSAGES_INBOX) || app.profile.hasUIFeature(
                FeatureType.MESSAGES_SENT)
        },

        util.createActionItem(
            text = R.string.menu_attendance_config,
            icon = CommunityMaterial.Icon.cmd_calendar_remove_outline
        ) {
            AttendanceConfigDialog(activity, reloadOnDismiss = false).show()
        }.takeIf { app.profile.hasUIFeature(FeatureType.ATTENDANCE) },

        util.createMoreItem(
            card = card,
            items = listOfNotNull(
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

                util.createPropertyItem(
                    text = R.string.settings_register_show_teacher_absences_text,
                    icon = CommunityMaterial.Icon.cmd_account_arrow_right_outline,
                    value = app.profile.getStudentData("showTeacherAbsences", true)
                ) { _, it ->
                    app.profile["showTeacherAbsences"] = it
                    app.profileSave()
                }.takeIf { app.profile.loginStoreType == LoginType.LIBRUS },

                util.createPropertyItem(
                    text = R.string.settings_register_hide_sticks_from_old,
                    icon = CommunityMaterial.Icon3.cmd_numeric_1_box_outline,
                    value = configProfile.grades.hideSticksFromOld
                ) { _, it ->
                    configProfile.grades.hideSticksFromOld = it
                }.takeIf { App.devMode && app.profile.hasUIFeature(FeatureType.GRADES) },
            ),
        ),

        *(getRegistrationItems().takeIf { !app.profile.archived } ?: arrayOf()),
    )

    private fun getRegistrationItems() = listOfNotNull(
        util.createSectionItem(
            text = R.string.settings_registration_section,
        ),

        util.createPropertyItem(
            text = R.string.settings_register_allow_registration_text,
            subText = R.string.settings_register_allow_registration_subtext,
            icon = CommunityMaterial.Icon.cmd_account_circle_outline,
            value = app.profile.canShare,
            beforeChange =
            { item, value ->
                if (app.profile.canShare == value)
                // allow the switch to change - needed for util.refresh() to change the visual state
                    return@createPropertyItem true
                val dialog =
                    RegistrationConfigDialog(activity,
                        app.profile,
                        onChangeListener = { enabled ->
                            if (item.isChecked == enabled)
                                return@RegistrationConfigDialog
                            item.isChecked = enabled
                            if (value) {
                                card.items.after(item, sharedEventsDefaultItem)
                            } else {
                                card.items.remove(sharedEventsDefaultItem)
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

        sharedEventsDefaultItem.takeIf { app.profile.canShare },
    ).toTypedArray()
}
