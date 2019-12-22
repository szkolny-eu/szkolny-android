/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-22
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.currentTimeUnix
import pl.szczodrzynski.edziennik.data.api.ERROR_TEMPLATE_WEB_OTHER
import pl.szczodrzynski.edziennik.data.api.GET
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.models.ApiError

open class EdudziennikApi(open val data: DataEdudziennik) {
    companion object {
        private const val TAG = "TemplateApi"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    /**
     * This will be used by all TemplateApi* endpoints.
     *
     * You can customize this method's parameters to best fit the implemented e-register.
     * Just make sure that [tag] and [onSuccess] is present.
     */
    fun apiGet(tag: String, endpoint: String, method: Int = GET, payload: JsonObject? = null, onSuccess: (json: JsonObject?) -> Unit) {
        val json = JsonObject()
        json.addProperty("foo", "bar")
        json.addProperty("sample", "text")

        if (currentTimeUnix() % 4L == 0L) {
            // let's set a 20% chance of error, just as a test
            data.error(ApiError(tag, ERROR_TEMPLATE_WEB_OTHER)
                    .withApiResponse("404 Not Found - this is the text returned by the API"))
            return
        }

        onSuccess(json)
    }
}
