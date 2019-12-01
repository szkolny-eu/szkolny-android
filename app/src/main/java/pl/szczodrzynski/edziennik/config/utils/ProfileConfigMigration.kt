/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-1.
 */

package pl.szczodrzynski.edziennik.config.utils

import android.content.Context
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.config.Config

class ProfileConfigMigration(app: App, config: Config) {
    init { config.apply {
        val p = app.getSharedPreferences("pl.szczodrzynski.edziennik_profiles", Context.MODE_PRIVATE)
        val s = "app.appConfig"

        if (dataVersion < 1) {

            //dataVersion = 1
        }
        if (dataVersion < 2) {
            //gradesColorMode do profilu !
            //agendaViewType do profilu !
            // app.appConfig.dontCountZeroToAverage do profilu !
        }
    }}
}