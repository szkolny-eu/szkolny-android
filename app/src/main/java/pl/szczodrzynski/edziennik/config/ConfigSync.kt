/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.data.api.szkolny.response.RegisterAvailabilityStatus
import pl.szczodrzynski.edziennik.ext.HOUR
import pl.szczodrzynski.edziennik.utils.models.Time

@Suppress("RemoveExplicitTypeArguments")
class ConfigSync(base: Config) {

    var enabled by base.config<Boolean>("syncEnabled", true)
    var interval by base.config<Int>("syncInterval", 1 * HOUR.toInt())
    var onlyWifi by base.config<Boolean>("syncOnlyWifi", false)

    var dontShowAppManagerDialog by base.config<Boolean>(false)
    var lastAppSync by base.config<Long>(0L)
    var notifyAboutUpdates by base.config<Boolean>(true)
    var webPushEnabled by base.config<Boolean>(true)

    // Quiet Hours
    var quietHoursEnabled by base.config<Boolean>(false)
    var quietHoursStart by base.config<Time?>(null)
    var quietHoursEnd by base.config<Time?>(null)
    var quietDuringLessons by base.config<Boolean>(false)

    var luckyNumberOnlyMe by base.config<Boolean>(false)

    // FCM Tokens
    var tokenApp by base.config<String?>(null)
    var tokenMobidziennik by base.config<String?>(null)
    var tokenLibrus by base.config<String?>(null)
    var tokenVulcan by base.config<String?>(null)
    var tokenVulcanHebe by base.config<String?>(null)

    var tokenMobidziennikList by base.config<List<Int>> { listOf() }
    var tokenLibrusList by base.config<List<Int>> { listOf() }
    var tokenVulcanList by base.config<List<Int>> { listOf() }
    var tokenVulcanHebeList by base.config<List<Int>> { listOf() }

    // Register Availability
    private var registerAvailabilityMap by base.config<Map<String, RegisterAvailabilityStatus>>("registerAvailability") { mapOf() }
    private var registerAvailabilityFlavor by base.config<String?>(null)

    var registerAvailability: Map<String, RegisterAvailabilityStatus>
        get() {
            if (BuildConfig.FLAVOR != registerAvailabilityFlavor)
                return mapOf()
            return registerAvailabilityMap
        }
        set(value) {
            registerAvailabilityMap = value
            registerAvailabilityFlavor = BuildConfig.FLAVOR
        }
}
