/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-14
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_LUCKY_NUMBER
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.LuckyNumber
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class LibrusApiLuckyNumber(override val data: DataLibrus,
                           override val lastSync: Long?,
                           val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiLuckyNumber"
    }

    init {
        var nextSync = System.currentTimeMillis() + 2*DAY*1000

        apiGet(TAG, "LuckyNumbers") { json ->
            if (json.isJsonNull) {
                //profile?.luckyNumberEnabled = false
            } else {
                json.getJsonObject("LuckyNumber")?.also { luckyNumberEl ->

                    val luckyNumberDate = Date.fromY_m_d(luckyNumberEl.getString("LuckyNumberDay")) ?: Date.getToday()
                    val luckyNumber = luckyNumberEl.getInt("LuckyNumber") ?: -1
                    val luckyNumberObject = LuckyNumber(
                            profileId,
                            luckyNumberDate,
                            luckyNumber
                    )

                    if (luckyNumberDate >= Date.getToday())
                        nextSync = luckyNumberDate.combineWith(Time(15, 0, 0))
                    else
                        nextSync = System.currentTimeMillis() + 6*HOUR*1000

                    data.luckyNumberList.add(luckyNumberObject)
                    data.metadataList.add(
                            Metadata(
                                    profileId,
                                    Metadata.TYPE_LUCKY_NUMBER,
                                    luckyNumberObject.date.value.toLong(),
                                    true,
                                    profile?.empty ?: false,
                                    System.currentTimeMillis()
                            ))
                }
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_LUCKY_NUMBER, syncAt = nextSync)
            onSuccess(ENDPOINT_LIBRUS_API_LUCKY_NUMBER)
        }
    }
}
