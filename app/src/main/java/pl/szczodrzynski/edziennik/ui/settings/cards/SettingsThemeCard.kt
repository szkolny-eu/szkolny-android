/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.settings.cards

import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.dialogs.settings.AppLanguageDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.MiniMenuConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.ThemeChooserDialog
import pl.szczodrzynski.edziennik.ui.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.settings.SettingsUtil
import pl.szczodrzynski.edziennik.utils.BigNightUtil
import pl.szczodrzynski.edziennik.utils.models.Date

class SettingsThemeCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        R.string.settings_card_theme_title,
        items = ::getItems,
        itemsMore = ::getItemsMore,
    )

    override fun getItems(card: MaterialAboutCard) = listOfNotNull(
        if (Date.getToday().month / 3 % 4 == 0) // cool math games
            util.createPropertyItem(
                text = R.string.settings_theme_snowfall_text,
                subText = R.string.settings_theme_snowfall_subtext,
                icon = CommunityMaterial.Icon3.cmd_snowflake,
                value = configGlobal.ui.snowfall
            ) { _, it ->
                configGlobal.ui.snowfall = it
                activity.recreate()
            }
        else null,

        if (BigNightUtil().isDataWielkanocyNearDzisiaj()) // cool klasa for utility to dzień wielkanocy
            util.createPropertyItem(
                text = R.string.settings_theme_eggfall_text,
                subText = R.string.settings_theme_eggfall_subtext,
                icon = CommunityMaterial.Icon.cmd_egg_easter,
                value = configGlobal.ui.eggfall
            ) { _, it ->
                configGlobal.ui.eggfall = it
                activity.recreate()
            }
        else null,

        util.createActionItem(
            text = R.string.settings_theme_theme_text,
            subText = app.uiManager.themeColor.nameRes,
            icon = CommunityMaterial.Icon3.cmd_palette_outline
        ) {
            ThemeChooserDialog(activity).show()
        },

        util.createActionItem(
            text = R.string.settings_about_language_text,
            subText = R.string.settings_about_language_subtext,
            icon = CommunityMaterial.Icon3.cmd_translate
        ) {
            AppLanguageDialog(activity).show()
        },

        util.createPropertyItem(
            text = R.string.settings_theme_mini_drawer_text,
            subText = R.string.settings_theme_mini_drawer_subtext,
            icon = CommunityMaterial.Icon.cmd_dots_vertical,
            value = configGlobal.ui.miniMenuVisible
        ) { _, it ->
            configGlobal.ui.miniMenuVisible = it
            activity.navView.drawer.miniDrawerVisiblePortrait = it
        }
    )

    override fun getItemsMore(card: MaterialAboutCard) = listOf(
        util.createActionItem(
            text = R.string.settings_theme_mini_drawer_buttons_text,
            icon = CommunityMaterial.Icon2.cmd_format_list_checks
        ) {
            MiniMenuConfigDialog(activity).show()
        },

        util.createActionItem(
            text = R.string.settings_theme_drawer_header_text,
            icon = CommunityMaterial.Icon2.cmd_image_outline
        ) {
            if (app.config.ui.headerBackground == null) {
                setHeaderBackground()
                return@createActionItem
            }
            MaterialAlertDialogBuilder(activity)
                .setItems(
                    arrayOf(
                        activity.getString(R.string.settings_theme_drawer_header_dialog_set),
                        activity.getString(R.string.settings_theme_drawer_header_dialog_restore)
                    )
                ) { _, which ->
                    when (which) {
                        0 -> setHeaderBackground()
                        1 -> {
                            app.config.ui.headerBackground = null
                            activity.drawer.setAccountHeaderBackground(null)
                            activity.drawer.open()
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        },

        util.createActionItem(
            text = R.string.settings_theme_app_background_text,
            subText = R.string.settings_theme_app_background_subtext,
            icon = CommunityMaterial.Icon2.cmd_image_filter_hdr
        ) {
            if (app.config.ui.appBackground == null) {
                setAppBackground()
                return@createActionItem
            }
            MaterialAlertDialogBuilder(activity)
                .setItems(
                    arrayOf(
                        activity.getString(R.string.settings_theme_app_background_dialog_set),
                        activity.getString(R.string.settings_theme_app_background_dialog_restore)
                    )
                ) { _, which ->
                    when (which) {
                        0 -> setAppBackground()
                        1 -> {
                            app.config.ui.appBackground = null
                            activity.setAppBackground()
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        },

        util.createPropertyItem(
            text = R.string.settings_theme_open_drawer_on_back_pressed_text,
            icon = CommunityMaterial.Icon3.cmd_menu_open,
            value = configGlobal.ui.openDrawerOnBackPressed
        ) { _, it ->
            configGlobal.ui.openDrawerOnBackPressed = it
        }
    )

    private fun setHeaderBackground() = activity.requestHandler.requestHeaderBackground {
        activity.drawer.setAccountHeaderBackground(null)
        activity.drawer.setAccountHeaderBackground(app.config.ui.headerBackground)
        activity.drawer.open()
    }

    private fun setAppBackground() = activity.requestHandler.requestAppBackground {
        activity.setAppBackground()
    }
}
