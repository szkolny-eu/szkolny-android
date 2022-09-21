/*
 * Copyright (c) Kuba Szczodrzyński 2022-9-16.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_MESSAGEBOX
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_MESSAGE_BOXES
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.ext.DAY
import pl.szczodrzynski.edziennik.ext.getString

class VulcanHebeMessageBoxes(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeMessageBoxes"
    }

    init {
        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_MESSAGEBOX,
            lastSync = lastSync
        ) { list, _ ->
            var found = false
            for (messageBox in list) {
                val name = messageBox.getString("Name") ?: continue
                val studentName = profile?.studentNameLong ?: continue
                if (!name.contains(studentName))
                    continue

                data.messageBoxKey = messageBox.getString("GlobalKey")
                data.messageBoxName = name
                found = true
                break
            }
            if (!found && list.isNotEmpty()) {
                list.firstOrNull()?.let { messageBox ->
                    data.messageBoxKey = messageBox.getString("GlobalKey")
                    data.messageBoxName = messageBox.getString("Name")
                }
            }
            data.setSyncNext(ENDPOINT_VULCAN_HEBE_MESSAGE_BOXES, 7 * DAY)
            onSuccess(ENDPOINT_VULCAN_HEBE_MESSAGE_BOXES)
        }
    }
}
