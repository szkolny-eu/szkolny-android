package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import androidx.core.util.set
import androidx.room.OnConflictStrategy
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.RecipientListGetEvent
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.ext.*

class MobidziennikHtmlGetRecipientList(
    override val data: DataMobidziennik,
    val onSuccess: () -> Unit,
) : MobidziennikWeb(data, null) {
    companion object {
        private const val TAG = "MobidziennikHtmlGetRecipientList"
    }

    init {
        webGet(TAG, "/mobile/dodajwiadomosc") { text ->
            Regexes.MOBIDZIENNIK_MESSAGE_RECIPIENTS_JSON.find(text)?.let { match ->
                val recipientLists = JsonParser.parseString(match[1]).asJsonArray
                recipientLists?.asJsonObjectList()?.forEach { list ->
                    val listType = list.getString("typ")?.toIntOrNull() ?: -1
                    val listName = list.getString("nazwa") ?: ""
                    list.getJsonArray("dane")?.asJsonObjectList()?.forEach { recipient ->
                        if (recipient.getBoolean("lista") == true) {
                            recipient.getJsonArray("dane")?.asJsonObjectList()?.forEach {
                                processRecipient(listType, recipient.getString("nazwa") ?: "", it)
                            }
                        } else
                            processRecipient(listType, listName, recipient)
                    }
                }
            }

            val event = RecipientListGetEvent(
                data.profileId,
                data.teacherList.filter { it.loginId != null },
            )

            profile?.lastReceiversSync = System.currentTimeMillis()

            data.teacherOnConflictStrategy = OnConflictStrategy.REPLACE
            EventBus.getDefault().postSticky(event)
            onSuccess()
        }
    }

    private fun processRecipient(listType: Int, listName: String, recipient: JsonObject) {
        val loginId = recipient.getString("id") ?: return
        val idLong = loginId.replace(Regexes.NOT_DIGITS, "").toLongOrNull() ?: return
        val fullName = recipient.getString("nazwa")?.fixName()

        val teacher = data.teacherList.singleOrNull { fullName != null && it.fullNameLastFirst == fullName }
            ?: data.teacherList[idLong]
            ?: Teacher(data.profileId, idLong).apply {
                name = fullName ?: ""
                fullName?.splitName()?.let {
                    name = it.second
                    surname = it.first
                }
                data.teacherList[idLong] = this
            }

        teacher.apply {
            this.loginId = loginId
            when (listType) {
                1 -> setTeacherType(Teacher.TYPE_PRINCIPAL)
                2 -> setTeacherType(Teacher.TYPE_TEACHER)
                3 -> setTeacherType(Teacher.TYPE_PARENT)
                4 -> setTeacherType(Teacher.TYPE_STUDENT)
                7 -> setTeacherType(Teacher.TYPE_PARENTS_COUNCIL)
                8 -> {
                    setTeacherType(Teacher.TYPE_EDUCATOR)
                    typeDescription = listName
                }
                9 -> setTeacherType(Teacher.TYPE_PEDAGOGUE)
                10 -> setTeacherType(Teacher.TYPE_SPECIALIST)
                else -> when (listName) {
                    "Administratorzy" -> setTeacherType(Teacher.TYPE_SCHOOL_ADMIN)
                    "Sekretarka" -> setTeacherType(Teacher.TYPE_SECRETARIAT)
                    "Wsparcie techniczne mobiDziennik" -> setTeacherType(Teacher.TYPE_SUPER_ADMIN)
                    else -> {
                        setTeacherType(Teacher.TYPE_OTHER)
                        typeDescription = listName
                    }
                }
            }
        }
    }
}
