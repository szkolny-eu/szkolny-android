/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-14
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_LUCKY_NUMBER
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.luckynumber.LuckyNumber
import pl.szczodrzynski.edziennik.getInt
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiLuckyNumber(override val data: DataLibrus,
                           val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiLuckyNumber"
    }

    init {
        apiGet(TAG, "LuckyNumbers") { json ->
            if (json.isJsonNull) {
                profile?.luckyNumberEnabled = false
            } else {
                profile?.also { profile ->
                    profile.luckyNumber = -1
                    profile.luckyNumberDate = Date.getToday()

                    json.getJsonObject("LuckyNumber")?.also { luckyNumber ->
                        profile.luckyNumber = luckyNumber.getInt("LuckyNumber") ?: -1
                        profile.luckyNumberDate = Date.fromY_m_d(luckyNumber.getString("LuckyNumberDay"))
                    }

                    data.luckyNumberList.add(LuckyNumber(
                            profileId,
                            profile.luckyNumberDate ?: Date.getToday(),
                            profile.luckyNumber
                    ))
                }
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_LUCKY_NUMBER, SYNC_ALWAYS)
            onSuccess()
        }
    }
}