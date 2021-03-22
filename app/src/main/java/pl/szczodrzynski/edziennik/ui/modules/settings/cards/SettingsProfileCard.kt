/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.modules.settings.cards

import android.content.Intent
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.szkolny.font.SzkolnyFont
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.dialogs.settings.ProfileRemoveDialog
import pl.szczodrzynski.edziennik.ui.modules.login.LoginActivity
import pl.szczodrzynski.edziennik.ui.modules.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.modules.settings.SettingsUtil

class SettingsProfileCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        null,
        items = getItems(),
        itemsMore = getItemsMore()
    )

    override fun getItems() = listOf(
        util.createProfileItem(
            profile = app.profile
        ) {

        },

        util.createActionItem(
            text = R.string.settings_add_student_text,
            subText = R.string.settings_add_student_subtext,
            icon = CommunityMaterial.Icon.cmd_account_plus_outline
        ) {
            activity.startActivity(Intent(activity, LoginActivity::class.java))
        },

        util.createActionItem(
            text = R.string.settings_profile_remove_text,
            subText = R.string.settings_profile_remove_subtext,
            icon = SzkolnyFont.Icon.szf_delete_empty_outline
        ) {
            ProfileRemoveDialog(activity, app.profile.id, app.profile.name, false)
        }
    )

    override fun getItemsMore() = listOf(
        util.createPropertyItem(
            text = R.string.settings_profile_sync_text,
            subText = R.string.settings_profile_sync_subtext,
            icon = CommunityMaterial.Icon.cmd_account_convert,
            value = app.profile.syncEnabled,
        ) { _, it ->
            app.profile.syncEnabled = it
            app.profileSave()
        }
    )
}
