/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.web

import com.google.gson.JsonParser
import pl.szczodrzynski.edziennik.api.v2.Regexes
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_CALENDAR
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_NOTICES
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date
import java.util.*

class MobidziennikWebNotices(override val data: DataMobidziennik,
                              val onSuccess: () -> Unit) : MobidziennikWeb(data)  {
    companion object {
        private const val TAG = "MobidziennikWebNotices"
    }

    init {
        // TODO this does no longer work: Mobidziennik changed their mobile page in 2019.09
        data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_NOTICES, SYNC_ALWAYS)
        onSuccess()
        /*webGet(TAG, "/mobile/zachowanie") { text ->
            MobidziennikLuckyNumberExtractor(data, text)

            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_NOTICES, SYNC_ALWAYS)
            onSuccess()
        }*/
    }
}