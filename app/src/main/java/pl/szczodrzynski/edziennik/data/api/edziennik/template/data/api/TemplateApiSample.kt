/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.template.data.api

import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.api.edziennik.template.DataTemplate
import pl.szczodrzynski.edziennik.data.api.edziennik.template.ENDPOINT_TEMPLATE_API_SAMPLE
import pl.szczodrzynski.edziennik.data.api.edziennik.template.data.TemplateApi
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.ext.DAY

class TemplateApiSample(override val data: DataTemplate,
                        override val lastSync: Long?,
                        val onSuccess: (endpointId: Int) -> Unit
) : TemplateApi(data, lastSync) {
    companion object {
        private const val TAG = "TemplateApiSample"
    }

    init {
        apiGet(TAG, "/api/v3/getData.php") { _ ->
            // here you can access and update any fields of the `data` object

            // ================
            // schedule a sync:

            // not sooner than two days later
            data.setSyncNext(ENDPOINT_TEMPLATE_API_SAMPLE, 2 * DAY)
            // in two days OR on explicit "grades" sync
            data.setSyncNext(ENDPOINT_TEMPLATE_API_SAMPLE, 2 * DAY, MainActivity.DRAWER_ITEM_GRADES)
            // only if sync is executed on Home view
            data.setSyncNext(ENDPOINT_TEMPLATE_API_SAMPLE, syncIn = null, viewId = MainActivity.DRAWER_ITEM_HOME)
            // always, in every sync
            data.setSyncNext(ENDPOINT_TEMPLATE_API_SAMPLE, SYNC_ALWAYS)

            onSuccess(ENDPOINT_TEMPLATE_API_SAMPLE)
        }
    }
}
