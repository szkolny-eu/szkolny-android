/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-15.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject
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
    names: Set<String>? = null,
) : UsosApi(data, lastSync) {
    companion object {
        const val TAG = "UsosApiTerms"
    }

    init {
        if (names != null) {
            apiRequest<JsonObject>(
                tag = TAG,
                service = "terms/terms",
                params = mapOf("term_ids" to names.joinToString("|")),
                responseType = ResponseType.OBJECT,
            ) { json, response ->
                if (!processResponse(json.entrySet().map { it.value.asJsonObject })) {
                    data.error(TAG, ERROR_USOS_API_INCOMPLETE_RESPONSE, response)
                    return@apiRequest
                }

                data.setSyncNext(ENDPOINT_USOS_API_TERMS, 2 * DAY)
                onSuccess(ENDPOINT_USOS_API_TERMS)
            }
        } else {
            apiRequest<JsonArray>(
                tag = TAG,
                service = "terms/search",
                params = mapOf("query" to Date.getToday().year.toString()),
                responseType = ResponseType.ARRAY,
            ) { json, response ->
                if (!processResponse(json.asJsonObjectList())) {
                    data.error(TAG, ERROR_USOS_API_INCOMPLETE_RESPONSE, response)
                    return@apiRequest
                }

                data.setSyncNext(ENDPOINT_USOS_API_TERMS, 2 * DAY)
                onSuccess(ENDPOINT_USOS_API_TERMS)
            }
        }
    }

    private fun processResponse(terms: List<JsonObject>): Boolean {
        val profile = profile ?: return false
        val termNames = data.termNames.toMutableMap()
        val today = Date.getToday()
        for (term in terms) {
            val id = term.getString("id")
            val name = term.getLangString("name")
            val orderKey = term.getInt("order_key")
            if (id != null && name != null)
                termNames[id] = "$orderKey$$name"

            if (!term.getBoolean("is_active", false))
                continue
            val startDate = term.getString("start_date")?.let { Date.fromY_m_d(it) } ?: continue
            val finishDate = term.getString("finish_date")?.let { Date.fromY_m_d(it) } ?: continue
            if (today !in startDate..finishDate)
                continue

            if (startDate.month >= 8)
                profile.dateSemester1Start = startDate
            else
                profile.dateSemester2Start = startDate

            if (finishDate.month >= 8)
                profile.dateYearEnd = finishDate
            else
                profile.dateSemester2Start = finishDate
        }
        // update school year start
        profile.studentSchoolYearStart = profile.dateSemester1Start.year
        // update year end date if there is a new year
        if (profile.dateYearEnd <= profile.dateSemester1Start)
            profile.dateYearEnd =
                profile.dateSemester1Start.clone().setYear(profile.dateSemester1Start.year + 1)
        data.termNames = termNames
        return true
    }
}
