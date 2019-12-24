/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-23
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_START
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS

class EdudziennikWebStart(override val data: DataEdudziennik,
                          val onSuccess: () -> Unit) : EdudziennikWeb(data) {
    companion object {
        private const val TAG = "EdudziennikWebStart"
    }

    init {
        webGet(TAG, data.studentEndpoint + "start") { text ->
            val doc = Jsoup.parse(text)

            EdudziennikWebStartInfo(data, text)
            EdudziennikWebStartGrades(data, doc)

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_START, SYNC_ALWAYS)
            onSuccess()
        }
    }
}
