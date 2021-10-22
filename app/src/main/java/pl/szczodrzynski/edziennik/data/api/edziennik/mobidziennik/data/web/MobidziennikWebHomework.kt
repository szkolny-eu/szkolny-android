/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-31.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_HOMEWORK
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.EventGetEvent
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.ext.get

class MobidziennikWebHomework(override val data: DataMobidziennik,
                              override val lastSync: Long?,
                              val type: Int = TYPE_CURRENT,
                              val event: EventFull,
                              val onSuccess: (endpointId: Int) -> Unit
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikWebHomework"
        const val TYPE_CURRENT = 0
        const val TYPE_PAST = 1
    }

    init {
        val endpoint = when (type) {
            TYPE_PAST -> "zadaniadomowearchiwalne"
            else -> "zadaniadomowe"
        }
        webGet(TAG, "/mobile/$endpoint") { text ->
            MobidziennikLuckyNumberExtractor(data, text)

            Regexes.MOBIDZIENNIK_MOBILE_HOMEWORK_ROW.findAll(text).forEach { homeworkMatch ->
                val tableRow = homeworkMatch[1].ifBlank { return@forEach }

                /*val items = Regexes.MOBIDZIENNIK_HOMEWORK_ITEM.findAll(tableRow).map { match ->
                    match[1] to match[2].fixWhiteSpaces()
                }.toList()*/

                val id = Regexes.MOBIDZIENNIK_MOBILE_HOMEWORK_ID.find(tableRow)?.get(1)?.toLongOrNull() ?: return@forEach
                if (event.id != id)
                    return@forEach

                //val homeworkBody = Regexes.MOBIDZIENNIK_HOMEWORK_BODY.find(tableRow)?.get(1) ?: ""

                event.attachmentIds = mutableListOf()
                event.attachmentNames = mutableListOf()
                Regexes.MOBIDZIENNIK_MOBILE_HOMEWORK_ATTACHMENT.findAll(tableRow).onEach {
                    event.attachmentIds?.add(it[1].toLongOrNull() ?: return@onEach)
                    event.attachmentNames?.add(it[2])
                }

                event.homeworkBody = ""
                event.isDownloaded = true
            }

            //data.eventList.add(eventObject)
            //data.metadataList.add(
            //        Metadata(
            //                profileId,
            //                Metadata.TYPE_EVENT,
            //                eventObject.id,
            //                profile?.empty ?: false,
            //                profile?.empty ?: false,
            //                System.currentTimeMillis() /* no addedDate here though */
            //        ))

            // not used as an endpoint
            //data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_HOMEWORK, SYNC_ALWAYS)
            data.eventList.add(event)
            data.eventListReplace = true

            EventBus.getDefault().postSticky(EventGetEvent(event))
            onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_HOMEWORK)
        }
    }
}
