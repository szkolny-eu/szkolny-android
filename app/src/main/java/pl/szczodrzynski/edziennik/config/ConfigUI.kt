/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.data.enums.NavTarget

@Suppress("RemoveExplicitTypeArguments")
class ConfigUI(base: Config) {

    var theme by base.config<Int>(1)
    var language by base.config<String?>(null)

    var appBackground by base.config<String?>("appBg", null)
    var headerBackground by base.config<String?>("headerBg", null)

    var miniMenuVisible by base.config<Boolean>(false)
    var miniMenuButtons by base.config<Set<NavTarget>> {
        setOf(
            NavTarget.HOME,
            NavTarget.TIMETABLE,
            NavTarget.AGENDA,
            NavTarget.GRADES,
            NavTarget.MESSAGES,
            NavTarget.HOMEWORK,
            NavTarget.SETTINGS
        )
    }
    var openDrawerOnBackPressed by base.config<Boolean>(false)

    var bottomSheetOpened by base.config<Boolean>(false)
    var snowfall by base.config<Boolean>(false)
    var eggfall by base.config<Boolean>(false)
}
