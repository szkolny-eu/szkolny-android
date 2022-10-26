/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-15.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.api

import com.google.gson.JsonArray
import pl.szczodrzynski.edziennik.data.api.ERROR_USOS_API_INCOMPLETE_RESPONSE
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.ENDPOINT_USOS_API_TERMS
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.UsosApi
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.models.Date

class UsosApiTerms(
    override val data: DataUsos,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : UsosApi(data, lastSync) {
    companion object {
        const val TAG = "UsosApiTerms"
    }

    init {
        apiRequest<JsonArray>(
            tag = TAG,
            service = "terms/search",
            params = mapOf(
                "query" to Date.getToday().year.toString(),
            ),
            responseType = ResponseType.ARRAY,
        ) { json, response ->
            if (!processResponse(json)) {
                data.error(TAG, ERROR_USOS_API_INCOMPLETE_RESPONSE, response)
                return@apiRequest
            }

            data.setSyncNext(ENDPOINT_USOS_API_TERMS, 7 * DAY)
            onSuccess(ENDPOINT_USOS_API_TERMS)
        }
    }

    private fun processResponse(json: JsonArray): Boolean {
        val today = Date.getToday()
        for (term in json.asJsonObjectList()) {
            if (!term.getBoolean("is_active", false))
                continue
            val startDate = term.getString("start_date")?.let { Date.fromY_m_d(it) } ?: continue
            val finishDate = term.getString("finish_date")?.let { Date.fromY_m_d(it) } ?: continue
            if (today in startDate..finishDate) {
                profile?.studentSchoolYearStart = startDate.year
                profile?.dateSemester1Start = startDate
                profile?.dateSemester2Start = finishDate
            }
        }
        return true
    }
}
