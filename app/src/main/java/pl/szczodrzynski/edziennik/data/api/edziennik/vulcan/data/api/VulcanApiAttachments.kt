/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-6.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api

import pl.szczodrzynski.edziennik.asJsonObjectList
import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_HOMEWORK_ATTACHMENTS
import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_MESSAGES_ATTACHMENTS
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getLong
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.models.Date
import kotlin.reflect.KClass

class VulcanApiAttachments(override val data: DataVulcan,
                           val list: List<*>,
                           val owner: Any?,
                           val ownerClass: KClass<*>,
                           val onSuccess: (list: List<*>) -> Unit
) : VulcanApi(data, null) {
    companion object {
        const val TAG = "VulcanApiAttachments"
    }

    init { run {
        val endpoint = when (ownerClass) {
            MessageFull::class -> VULCAN_API_ENDPOINT_MESSAGES_ATTACHMENTS
            EventFull::class -> VULCAN_API_ENDPOINT_HOMEWORK_ATTACHMENTS
            else -> null
        } ?: return@run

        val idName = when (ownerClass) {
            MessageFull::class -> "IdWiadomosc"
            EventFull::class -> "IdZadanieDomowe"
            else -> null
        } ?: return@run

        val startDate = profile?.getSemesterStart(profile?.currentSemester ?: 1)?.inUnix ?: 0
        val endDate = Date.getToday().stepForward(0, 1, 0).inUnix

        apiGet(TAG, endpoint, parameters = mapOf(
                "DataPoczatkowa" to startDate,
                "DataKoncowa" to endDate,
                "LoginId" to data.studentLoginId,
                "IdUczen" to data.studentId
        )) { json, _ ->

            json.getJsonArray("Data")?.asJsonObjectList()?.forEach { attachment ->
                val id = attachment.getLong("Id") ?: return@forEach
                val itemId = attachment.getLong(idName) ?: return@forEach
                val url = attachment.getString("Url") ?: return@forEach
                val fileName = "${attachment.getString("NazwaPliku")}:$url"

                list.forEach {
                    if (it is MessageFull
                            && it.profileId == profileId
                            && it.id == itemId
                            && it.attachmentIds?.contains(id) != true) {
                        if (it.attachmentIds == null)
                            it.attachmentIds = mutableListOf()
                        if (it.attachmentNames == null)
                            it.attachmentNames = mutableListOf()
                        it.attachmentIds?.add(id)
                        it.attachmentNames?.add(fileName)
                    }

                    if (it is EventFull
                            && it.profileId == profileId
                            && it.id == itemId
                            && it.attachmentIds?.contains(id) != true) {
                        if (it.attachmentIds == null)
                            it.attachmentIds = mutableListOf()
                        if (it.attachmentNames == null)
                            it.attachmentNames = mutableListOf()
                        it.attachmentIds?.add(id)
                        it.attachmentNames?.add(fileName)
                    }

                    if (owner is MessageFull
                            && it is MessageFull
                            && owner.profileId == it.profileId
                            && owner.id == it.id) {
                        owner.attachmentIds = it.attachmentIds
                        owner.attachmentNames = it.attachmentNames
                    }

                    if (owner is EventFull
                            && it is EventFull
                            && owner.profileId == it.profileId
                            && owner.id == it.id) {
                        owner.attachmentIds = it.attachmentIds
                        owner.attachmentNames = it.attachmentNames
                    }
                }
            }

            /*if (owner is MessageFull) {
                list.forEach {
                    (it as? MessageFull)?.let { message ->
                        data.messageList.add(message)
                    }
                }
                data.messageListReplace = true
            }

            if (owner is EventFull) {
                list.forEach {
                    (it as? EventFull)?.let { it1 ->
                        it1.homeworkBody = ""
                        data.eventList.add(it1)
                    }
                }
                data.eventListReplace = true
            }*/

            onSuccess(list)
        }
    }}
}
