/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-30.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web

import androidx.room.OnConflictStrategy
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_GET_RECIPIENT_LIST
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.RecipientListGetEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Teacher

class IdziennikWebGetRecipientList(
        override val data: DataIdziennik, val onSuccess: () -> Unit) : IdziennikWeb(data) {
    companion object {
        private const val TAG = "IdziennikWebGetRecipientList"
    }

    init {
        webApiGet(TAG, IDZIENNIK_WEB_GET_RECIPIENT_LIST, mapOf(
                "idP" to data.registerId
        )) { result ->
            val json = result.getJsonObject("d") ?: run {
                data.error(ApiError(TAG, ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA)
                        .withApiResponse(result))
                return@webApiGet
            }

            json.getJsonArray("ListK_Pracownicy")?.asJsonObjectList()?.forEach { recipient ->
                val name = recipient.getString("ImieNazwisko") ?: ": "
                val (fullName, subject) = name.split(": ").let {
                    Pair(it.getOrNull(0), it.getOrNull(1))
                }
                val guid = recipient.getString("Id") ?: ""
                // get teacher by ID or create it
                val teacher = data.getTeacherByFirstLast(fullName ?: " ")
                teacher.loginId = guid
                teacher.setTeacherType(Teacher.TYPE_TEACHER)
                // unset OTHER that is automatically set in IdziennikApiMessages*
                teacher.unsetTeacherType(Teacher.TYPE_OTHER)
                teacher.typeDescription = subject
            }

            json.getJsonArray("ListK_Opiekunowie")?.asJsonObjectList()?.forEach { recipient ->
                val name = recipient.getString("ImieNazwisko") ?: ": "
                val (fullName, parentOf) = Regexes.IDZIENNIK_MESSAGES_RECIPIENT_PARENT.find(name)?.let {
                    Pair(it.groupValues.getOrNull(1), it.groupValues.getOrNull(2))
                } ?: Pair(null, null)
                val guid = recipient.getString("Id") ?: ""
                // get teacher by ID or create it
                val teacher = data.getTeacherByFirstLast(fullName ?: " ")
                teacher.loginId = guid
                teacher.setTeacherType(Teacher.TYPE_PARENT)
                // unset OTHER that is automatically set in IdziennikApiMessages*
                teacher.unsetTeacherType(Teacher.TYPE_OTHER)
                teacher.typeDescription = parentOf
            }

            val event = RecipientListGetEvent(
                    data.profileId,
                    data.teacherList.filter { it.loginId != null }
            )

            profile?.lastReceiversSync = System.currentTimeMillis()

            data.teacherOnConflictStrategy = OnConflictStrategy.REPLACE
            EventBus.getDefault().postSticky(event)
            onSuccess()
        }
    }
}
