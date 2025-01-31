/*
 * Copyright (c) Kuba Szczodrzyński 2025-1-31.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.api

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.api.ERROR_USOS_API_INCOMPLETE_RESPONSE
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.ENDPOINT_USOS_API_ECTS_POINTS
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.UsosApi
import pl.szczodrzynski.edziennik.ext.DAY
import pl.szczodrzynski.edziennik.ext.filter

class UsosApiEctsPoints(
    override val data: DataUsos,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : UsosApi(data, lastSync) {
    companion object {
        const val TAG = "UsosApiEctsPoints"
    }

    init {
        apiRequest<JsonObject>(
            tag = TAG,
            service = "courses/user_ects_points",
            responseType = ResponseType.OBJECT,
        ) { json, response ->
            if (!processResponse(json)) {
                data.error(TAG, ERROR_USOS_API_INCOMPLETE_RESPONSE, response)
                return@apiRequest
            }

            data.setSyncNext(ENDPOINT_USOS_API_ECTS_POINTS, 2 * DAY)
            onSuccess(ENDPOINT_USOS_API_ECTS_POINTS)
        }
    }

    private fun processResponse(json: JsonObject): Boolean {
        for ((_, coursePointsEl) in json.entrySet()) {
            if (!coursePointsEl.isJsonObject)
                continue
            for ((courseId, pointsEl) in coursePointsEl.asJsonObject.entrySet()) {
                if (!pointsEl.isJsonPrimitive)
                    continue
                val gradeCategories = data.gradeCategories
                    .filter { it.text == courseId }
                gradeCategories.forEach {
                    it.weight = pointsEl.asString.toFloatOrNull() ?: -1.0f
                }
            }
        }
        return true
    }
}
