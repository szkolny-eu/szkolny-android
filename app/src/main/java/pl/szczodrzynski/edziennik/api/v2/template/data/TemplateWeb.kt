/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.api.v2.template.data

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.api.v2.ERROR_REQUEST_FAILURE
import pl.szczodrzynski.edziennik.api.v2.ERROR_RESPONSE_EMPTY
import pl.szczodrzynski.edziennik.api.v2.ERROR_TEMPLATE_WEB_OTHER
import pl.szczodrzynski.edziennik.api.v2.GET
import pl.szczodrzynski.edziennik.api.v2.librus.data.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.currentTimeUnix

open class TemplateWeb(open val data: DataTemplate) {
    companion object {
        private const val TAG = "TemplateWeb"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    /**
     * This will be used by all TemplateWeb* endpoints.
     *
     * You can customize this method's parameters to best fit the implemented e-register.
     * Just make sure that [tag] and [onSuccess] is present.
     */
    fun webGet(tag: String, endpoint: String, method: Int = GET, payload: JsonObject? = null, onSuccess: (json: JsonObject?) -> Unit) {
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