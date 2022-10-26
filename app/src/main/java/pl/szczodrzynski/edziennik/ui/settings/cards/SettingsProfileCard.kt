/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.settings.cards

import android.content.Intent
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.dialogs.settings.ProfileConfigDialog
import pl.szczodrzynski.edziennik.ui.login.LoginActivity
import pl.szczodrzynski.edziennik.ui.settings.MaterialAboutProfileItem
import pl.szczodrzynski.edziennik.ui.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.settings.SettingsUtil

class SettingsProfileCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        null,
        items = ::getItems,
        itemsMore = ::getItemsMore,
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
        }).show()
    }

    override fun getItems(card: MaterialAboutCard) = listOf(
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
