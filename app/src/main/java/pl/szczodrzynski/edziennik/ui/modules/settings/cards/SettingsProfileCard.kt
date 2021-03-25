/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.modules.settings.cards

import android.content.Intent
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.dialogs.profile.ProfileConfigDialog
import pl.szczodrzynski.edziennik.ui.modules.login.LoginActivity
import pl.szczodrzynski.edziennik.ui.modules.settings.MaterialAboutProfileItem
import pl.szczodrzynski.edziennik.ui.modules.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.modules.settings.SettingsUtil

class SettingsProfileCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        null,
        items = getItems(),
        itemsMore = listOf()
    )

    private fun getProfileItem(): MaterialAboutProfileItem = util.createProfileItem(
        profile = app.profile
    ) { item, profile ->
        ProfileConfigDialog(activity, profile, onProfileSaved = {
            val index = card.items.indexOf(item)
            if (index == -1)
                return@ProfileConfigDialog
            card.items.remove(item)
            card.items.add(index, getProfileItem())
            util.refresh()
        })
    }

    override fun getItems() = listOf(
        getProfileItem(),

        util.createActionItem(
            text = R.string.settings_add_student_text,
            subText = R.string.settings_add_student_subtext,
            icon = CommunityMaterial.Icon.cmd_account_plus_outline
        ) {
            activity.startActivity(Intent(activity, LoginActivity::class.java))
        }
    )
}
