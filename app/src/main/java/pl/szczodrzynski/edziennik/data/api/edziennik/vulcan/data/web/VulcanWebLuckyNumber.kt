/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-20.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.web

import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.data.api.VULCAN_WEB_ENDPOINT_LUCKY_NUMBER
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_WEB_LUCKY_NUMBERS
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanWebMain
import pl.szczodrzynski.edziennik.data.db.entity.LuckyNumber
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week

class VulcanWebLuckyNumber(override val data: DataVulcan,
                           override val lastSync: Long?,
                           val onSuccess: (endpointId: Int) -> Unit
) : VulcanWebMain(data, lastSync) {
    companion object {
        const val TAG = "VulcanWebLuckyNumber"
    }

    init {
        webGetJson(TAG, WEB_MAIN, VULCAN_WEB_ENDPOINT_LUCKY_NUMBER, parameters = mapOf(
                "permissions" to data.webPermissions
        )) { json, _ ->
            val tiles = json
                    .getJsonArray("data")
                    ?.mapNotNull { data.app.gson.fromJson(it.toString(), HomepageTile::class.java) }
                    ?.flatMap { it.children }

            if (tiles == null) {
                data.setSyncNext(ENDPOINT_VULCAN_WEB_LUCKY_NUMBERS, SYNC_ALWAYS)
                onSuccess(ENDPOINT_VULCAN_WEB_LUCKY_NUMBERS)
                return@webGetJson
            }

            var nextSync = System.currentTimeMillis() + 1* DAY *1000

            tiles.firstOrNull { it.name == data.schoolShort }?.children?.firstOrNull()?.let { tile ->
                // "Szczęśliwy numer w dzienniku: 16"
                return@let tile.name?.substringAfterLast(' ')?.toIntOrNull()?.let { number ->
                    // lucky number present
                    val luckyNumberObject = LuckyNumber(
                            profileId,
                            Date.getToday(),
                            number
                    )

                    data.luckyNumberList.add(luckyNumberObject)
                    data.metadataList.add(
                            Metadata(
                                    profileId,
                                    Metadata.TYPE_LUCKY_NUMBER,
                                    luckyNumberObject.date.value.toLong(),
                                    true,
                                    profile?.empty ?: false
                            ))
                }
            } ?: {
                // no lucky number
                if (Date.getToday().weekDay <= Week.FRIDAY && Time.getNow().hour >= 22) {
                    // working days, after 10PM
                    // consider the lucky number is disabled; sync in 4 days
                    nextSync = System.currentTimeMillis() + 4*DAY*1000
                }
                else if (Date.getToday().weekDay <= Week.FRIDAY && Time.getNow().hour < 22) {
                    // working days, before 10PM

                }
                else {
                    // weekends
                    nextSync = Week.getNearestWeekDayDate(Week.MONDAY).combineWith(Time(5, 0, 0))
                }
            }()

            data.setSyncNext(ENDPOINT_VULCAN_WEB_LUCKY_NUMBERS, SYNC_ALWAYS)
            onSuccess(ENDPOINT_VULCAN_WEB_LUCKY_NUMBERS)
        }
    }
}
