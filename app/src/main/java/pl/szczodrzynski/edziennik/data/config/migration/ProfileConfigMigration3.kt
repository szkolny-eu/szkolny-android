/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-27.
 */

package pl.szczodrzynski.edziennik.data.config.migration

import pl.szczodrzynski.edziennik.data.config.BaseMigration
import pl.szczodrzynski.edziennik.data.config.ProfileConfig
import pl.szczodrzynski.edziennik.ui.home.HomeCard
import pl.szczodrzynski.edziennik.ui.home.HomeCardModel

class ProfileConfigMigration3 : BaseMigration<ProfileConfig>() {

    override fun migrate(config: ProfileConfig) = config.apply {
        if (ui.homeCards.isNotEmpty()) {
            ui.homeCards += HomeCardModel(
                profileId = config.profileId ?: -1,
                cardId = HomeCard.CARD_NOTES,
            )
        }
    }
}
