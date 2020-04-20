/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.template.data.web

import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_GRADES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_HOME
import pl.szczodrzynski.edziennik.data.api.edziennik.template.DataTemplate
import pl.szczodrzynski.edziennik.data.api.edziennik.template.ENDPOINT_TEMPLATE_WEB_SAMPLE
import pl.szczodrzynski.edziennik.data.api.edziennik.template.data.TemplateWeb
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS

class TemplateWebSample(override val data: DataTemplate,
                        override val lastSync: Long?,
                        val onSuccess: (endpointId: Int) -> Unit
) : TemplateWeb(data, lastSync) {
    companion object {
        private const val TAG = "TemplateWebSample"
    }

    init {
        webGet(TAG, "/api/v3/getData.php") { json ->
            // here you can access and update any fields of the `data` object

            // ================
            // schedule a sync:

            // not sooner than two days later
            data.setSyncNext(ENDPOINT_TEMPLATE_WEB_SAMPLE, 2 * DAY)
            // in two days OR on explicit "grades" sync
            data.setSyncNext(ENDPOINT_TEMPLATE_WEB_SAMPLE, 2 * DAY, DRAWER_ITEM_GRADES)
            // only if sync is executed on Home view
            data.setSyncNext(ENDPOINT_TEMPLATE_WEB_SAMPLE, syncIn = null, viewId = DRAWER_ITEM_HOME)
            // always, in every sync
            data.setSyncNext(ENDPOINT_TEMPLATE_WEB_SAMPLE, SYNC_ALWAYS)

            onSuccess(ENDPOINT_TEMPLATE_WEB_SAMPLE)
        }
    }
}
