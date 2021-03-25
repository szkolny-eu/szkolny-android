/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-17.
 */

package pl.szczodrzynski.edziennik.ui.modules.settings

import com.danielstone.materialaboutlibrary.items.MaterialAboutItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity

abstract class SettingsCard(
    protected val util: SettingsUtil,
) {
    protected val app: App = util.activity.application as App
    protected val activity: MainActivity = util.activity

    protected val configGlobal by lazy { app.config }
    protected val configProfile by lazy { app.config.forProfile() }

    val card by lazy {
        buildCard()
    }

    protected abstract fun buildCard(): MaterialAboutCard
    protected abstract fun getItems(): List<MaterialAboutItem>
    protected open fun getItemsMore(): List<MaterialAboutItem> = listOf()
}
