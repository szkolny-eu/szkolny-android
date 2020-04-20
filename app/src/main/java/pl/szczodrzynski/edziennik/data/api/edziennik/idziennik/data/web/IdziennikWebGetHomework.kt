/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-1.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_GET_HOMEWORK
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.EventGetEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.getBoolean
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.getString

class IdziennikWebGetHomework(override val data: DataIdziennik,
                              val event: EventFull,
                              val onSuccess: () -> Unit
) : IdziennikWeb(data, null) {
    companion object {
        private const val TAG = "IdziennikWebGetHomework"
    }

    init {
        webApiGet(TAG, IDZIENNIK_WEB_GET_HOMEWORK, mapOf(
                "idP" to data.registerId,
                "idPD" to event.id
        )) { result ->
            val json = result.getJsonObject("d") ?: run {
                data.error(ApiError(TAG, ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA)
                        .withApiResponse(result))
                return@webApiGet
            }

            val homework = json.getJsonObject("praca") ?: return@webApiGet

            if (homework.getBoolean("zalacznik", false)) {
                event.attachmentIds = mutableListOf(event.id)
                event.attachmentNames = mutableListOf("Załącznik do zadania")
            }
            else {
                event.attachmentIds = mutableListOf()
                event.attachmentNames = mutableListOf()
            }
            event.homeworkBody = homework.getString("tresc")

            data.eventList.add(event)
            data.eventListReplace = true

            EventBus.getDefault().postSticky(EventGetEvent(event))
            onSuccess()
        }
    }
}
