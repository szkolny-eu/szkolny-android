/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-31.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages

import androidx.core.util.set
import androidx.room.OnConflictStrategy
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusMessages
import pl.szczodrzynski.edziennik.data.api.events.RecipientListGetEvent
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.ext.*

class LibrusMessagesGetRecipientList(override val data: DataLibrus,
                                     val onSuccess: () -> Unit
) : LibrusMessages(data, null) {
    companion object {
        private const val TAG = "LibrusMessagesGetRecipientList"
    }

    private val listTypes = mutableListOf<Pair<String, String>>()

    init {
        messagesGet(TAG, "Receivers/action/GetTypes", parameters = mapOf(
            "includeClass" to 1
        )) { doc ->
            doc.select("response GetTypes data list ArrayItem")?.forEach {
                val id = it.getElementsByTag("id")?.firstOrNull()?.ownText() ?: return@forEach
                val name = it.getElementsByTag("name")?.firstOrNull()?.ownText() ?: return@forEach
                listTypes += id to name
            }

            getLists()
        }
    }

    private fun getLists() {
        if (listTypes.isEmpty()) {
            finish()
            return
        }
        val type = listTypes.removeAt(0)
        if (type.first == "contactsGroups") {
            getLists()
            return
        }
        messagesGetJson(TAG, "Receivers/action/GetListForType", parameters = mapOf(
                "receiverType" to type.first
        )) { json ->
            val dataEl = json?.getJsonObject("response")?.getJsonObject("GetListForType")?.get("data")
            if (dataEl is JsonObject) {
                val listEl = dataEl.get("ArrayItem")
                if (listEl is JsonArray) {
                    listEl.asJsonObjectList()?.forEach { item ->
                        processElement(item, type.first, type.second)
                    }
                }
                if (listEl is JsonObject) {
                    processElement(listEl, type.first, type.second)
                }
            }

            getLists()
        }
    }

    private fun processElement(element: JsonObject, typeId: String, typeName: String, listName: String? = null) {
        val listEl = element.getJsonObject("list")?.get("ArrayItem")
        if (listEl is JsonArray) {
            listEl.asJsonObjectList()?.let { list ->
                val label = element.getString("label") ?: ""
                list.forEach { item ->
                    processElement(item, typeId, typeName, label)
                }
                return
            }
        }
        if (listEl is JsonObject) {
            val label = element.getString("label") ?: ""
            processElement(listEl, typeId, typeName, label)
            return
        }
        processRecipient(element, typeId, typeName, listName)
    }

    private fun processRecipient(recipient: JsonObject, typeId: String, typeName: String, listName: String? = null) {
        val id = recipient.getLong("id") ?: return
        val label = recipient.getString("label") ?: return

        val fullNameLastFirst: String
        val description: String?
        if (typeId == "parentsCouncil" || typeId == "schoolParentsCouncil") {
            val delimiterIndex = label.lastIndexOf(" - ")
            if (delimiterIndex == -1) {
                fullNameLastFirst = label.fixName()
                description = null
            }
            else {
                fullNameLastFirst = label.substring(0, delimiterIndex).fixName()
                description = label.substring(delimiterIndex+3)
            }
        }
        else {
            fullNameLastFirst = label.fixName()
            description = null
        }

        var typeDescription: String? = null
        val type = when (typeId) {
            "tutors" -> Teacher.TYPE_EDUCATOR
            "teachers" -> Teacher.TYPE_TEACHER
            "classParents" -> Teacher.TYPE_PARENT
            "guardians" -> Teacher.TYPE_PARENT
            "parentsCouncil" -> {
                typeDescription = joinNotNullStrings(": ", listName, description)
                Teacher.TYPE_PARENTS_COUNCIL
            }
            "schoolParentsCouncil" -> {
                typeDescription = joinNotNullStrings(": ", listName, description)
                Teacher.TYPE_SCHOOL_PARENTS_COUNCIL
            }
            "pedagogue" -> Teacher.TYPE_PEDAGOGUE
            "librarian" -> Teacher.TYPE_LIBRARIAN
            "admin" -> Teacher.TYPE_SCHOOL_ADMIN
            "secretary" -> Teacher.TYPE_SECRETARIAT
            "sadmin" -> Teacher.TYPE_SUPER_ADMIN
            else -> {
                typeDescription = typeName
                Teacher.TYPE_OTHER
            }
        }

        // get teacher by fullName AND type or create it
        val teacher = data.teacherList.singleOrNull {
            it.fullNameLastFirst == fullNameLastFirst && ((type != Teacher.TYPE_SCHOOL_ADMIN && type != Teacher.TYPE_PEDAGOGUE) || it.isType(type))
        } ?: Teacher(data.profileId, id).apply {
            if (typeId == "sadmin" && id == 2L) {
                name = "Pomoc"
                surname = "Techniczna LIBRUS"
            }
            else {
                name = fullNameLastFirst
                fullNameLastFirst.splitName()?.let {
                    name = it.second
                    surname = it.first
                }
            }
            data.teacherList[id] = this
        }

        teacher.apply {
            this.loginId = id.toString()
            this.setTeacherType(type)
            if (this.typeDescription.isNullOrBlank())
                this.typeDescription = typeDescription
        }
    }

    private fun finish() {
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
