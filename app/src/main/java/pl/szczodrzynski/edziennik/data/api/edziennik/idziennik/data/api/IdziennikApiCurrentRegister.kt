/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-29.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.api

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_API_CURRENT_REGISTER
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.ENDPOINT_IDZIENNIK_API_CURRENT_REGISTER
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikApi
import pl.szczodrzynski.edziennik.data.db.modules.luckynumber.LuckyNumber
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.getInt
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class IdziennikApiCurrentRegister(override val data: DataIdziennik,
                          val onSuccess: () -> Unit) : IdziennikApi(data) {
    companion object {
        private const val TAG = "IdziennikApiCurrentRegister"
    }

    init {
        data.profile?.luckyNumber = -1
        data.profile?.luckyNumberDate = null

        apiGet(TAG, IDZIENNIK_API_CURRENT_REGISTER) { json ->
            if (json !is JsonObject) {
                onSuccess()
                return@apiGet
            }

            var nextSync = System.currentTimeMillis() + 14*DAY*1000

            val settings = json.getJsonObject("ustawienia")?.apply {
                profile?.dateSemester1Start = getString("poczatekSemestru1")?.let { Date.fromY_m_d(it) }
                profile?.dateSemester2Start = getString("koniecSemestru1")?.let { Date.fromY_m_d(it).stepForward(0, 0, 1) }
                profile?.dateYearEnd = getString("koniecSemestru2")?.let { Date.fromY_m_d(it) }
            }

            json.getInt("szczesliwyNumerek")?.let { luckyNumber ->
                val luckyNumberDate = Date.getToday()
                settings.getString("godzinaPublikacjiSzczesliwegoLosu")
                        ?.let { Time.fromH_m(it) }
                        ?.let { publishTime ->
                            val now = Time.getNow()
                            if (publishTime.value < 150000 && now.value < publishTime.value) {
                                nextSync = luckyNumberDate.combineWith(publishTime)
                                luckyNumberDate.stepForward(0, 0, -1) // the lucky number is still for yesterday
                            }
                            else if (publishTime.value >= 150000 && now.value > publishTime.value) {
                                luckyNumberDate.stepForward(0, 0, 1) // the lucky number is already for tomorrow
                                nextSync = luckyNumberDate.combineWith(publishTime)
                            }
                            else if (publishTime.value < 150000) {
                                nextSync = luckyNumberDate
                                        .clone()
                                        .stepForward(0, 0, 1)
                                        .combineWith(publishTime)
                            }
                            else {
                                nextSync = luckyNumberDate.combineWith(publishTime)
                            }
                        }


                val luckyNumberObject = LuckyNumber(
                        data.profileId,
                        Date.getToday(),
                        luckyNumber
                )

                data.luckyNumberList.add(luckyNumberObject)
                data.metadataList.add(
                        Metadata(
                                profileId,
                                Metadata.TYPE_LUCKY_NUMBER,
                                luckyNumberObject.date.value.toLong(),
                                data.profile?.empty ?: false,
                                data.profile?.empty ?: false,
                                System.currentTimeMillis()
                        ))
            }


            data.setSyncNext(ENDPOINT_IDZIENNIK_API_CURRENT_REGISTER, syncAt = nextSync)
            onSuccess()
        }
    }
}
