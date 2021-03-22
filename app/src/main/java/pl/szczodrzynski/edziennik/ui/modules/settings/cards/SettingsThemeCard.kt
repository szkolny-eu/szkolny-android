/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.modules.settings.cards

import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.dialogs.settings.AppLanguageDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.MiniMenuConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.ThemeChooserDialog
import pl.szczodrzynski.edziennik.ui.modules.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.modules.settings.SettingsUtil
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.models.Date

class SettingsThemeCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        R.string.settings_card_theme_title,
        items = getItems(),
        itemsMore = getItemsMore()
    )

    override fun getItems() = listOfNotNull(
        if (Date.getToday().month % 11 == 1) // cool math games
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

        util.createActionItem(
            text = R.string.settings_theme_theme_text,
            subText = Themes.getThemeNameRes(),
            icon = CommunityMaterial.Icon3.cmd_palette_outline
        ) {
            ThemeChooserDialog(activity)
        },

        util.createActionItem(
            text = R.string.settings_about_language_text,
            subText = R.string.settings_about_language_subtext,
            icon = CommunityMaterial.Icon3.cmd_translate
        ) {
            AppLanguageDialog(activity)
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

    override fun getItemsMore() = listOf(
        util.createActionItem(
            text = R.string.settings_theme_mini_drawer_buttons_text,
            icon = CommunityMaterial.Icon2.cmd_format_list_checks
        ) {
            MiniMenuConfigDialog(activity)
        },

        util.createActionItem(
            text = R.string.settings_theme_drawer_header_text,
            icon = CommunityMaterial.Icon2.cmd_image_outline
        ) {
            // TODO: 2021-03-17
        },

        util.createActionItem(
            text = R.string.settings_theme_app_background_text,
            subText = R.string.settings_theme_app_background_subtext,
            icon = CommunityMaterial.Icon2.cmd_image_filter_hdr
        ) {
            // TODO: 2021-03-17
        },

        util.createPropertyItem(
            text = R.string.settings_theme_open_drawer_on_back_pressed_text,
            icon = CommunityMaterial.Icon3.cmd_menu_open,
            value = configGlobal.ui.openDrawerOnBackPressed
        ) { _, it ->
            configGlobal.ui.openDrawerOnBackPressed = it
        }
    )
}
