/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-22.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_LUCKY_NUMBER
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_LUCKY_NUMBER
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.LuckyNumber
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.getInt
import pl.szczodrzynski.edziennik.ext.getString
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week

class VulcanHebeLuckyNumber(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeLuckyNumber"
    }

    init {
        apiGet(
            TAG,
            VULCAN_HEBE_ENDPOINT_LUCKY_NUMBER,
            query = mapOf(
                "constituentId" to data.studentConstituentId.toString(),
                "day" to Date.getToday().stringY_m_d
            )
        ) { lucky: JsonObject?, _ ->
            // sync tomorrow if lucky number set or is weekend or afternoon
            var nextSync = Date.getToday().stepForward(0, 0, 1).inMillis
            if (lucky == null) {
                if (Date.getToday().weekDay <= Week.FRIDAY && Time.getNow().hour < 12) {
                    // working days morning, sync always
                    nextSync = SYNC_ALWAYS
                }
            }
            else {
                val luckyNumberDate = Date.fromY_m_d(lucky.getString("Day")) ?: Date.getToday()
                val luckyNumber = lucky.getInt("Number") ?: -1
                val luckyNumberObject = LuckyNumber(
                    profileId = profileId,
                    date = luckyNumberDate,
                    number = luckyNumber
                )

                data.luckyNumberList.add(luckyNumberObject)
                data.metadataList.add(
                    Metadata(
                        profileId,
                        MetadataType.LUCKY_NUMBER,
                        luckyNumberObject.date.value.toLong(),
                        true,
                        profile?.empty ?: false
                    )
                )
            }

            data.setSyncNext(ENDPOINT_VULCAN_HEBE_LUCKY_NUMBER, syncAt = nextSync)
            onSuccess(ENDPOINT_VULCAN_HEBE_LUCKY_NUMBER)
        }
    }
}
