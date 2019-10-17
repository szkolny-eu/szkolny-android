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
                profile?.apply {
                    luckyNumber = -1
                    luckyNumberDate = Date.getToday()

                    json.getJsonObject("LuckyNumber")?.also { luckyNumberEl ->
                        luckyNumber = luckyNumberEl.getInt("LuckyNumber") ?: -1
                        luckyNumberDate = Date.fromY_m_d(luckyNumberEl.getString("LuckyNumberDay"))
                    }

                    data.luckyNumberList.add(LuckyNumber(
                            profileId,
                            luckyNumberDate ?: Date.getToday(),
                            luckyNumber
                    ))
                }
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_LUCKY_NUMBER, SYNC_ALWAYS)
            onSuccess()
        }
    }
}
