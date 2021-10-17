package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.synergia

import org.greenrobot.eventbus.EventBus
import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusSynergia
import pl.szczodrzynski.edziennik.data.api.events.MessageGetEvent
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.data.db.full.MessageRecipientFull
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.ext.singleOrNull
import pl.szczodrzynski.edziennik.ext.swapFirstLastName
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusSynergiaGetMessage(override val data: DataLibrus,
                               private val messageObject: MessageFull,
                               val onSuccess: () -> Unit) : LibrusSynergia(data, null) {
    companion object {
        const val TAG = "LibrusSynergiaGetMessage"
    }

    init {
        val endpoint = when (messageObject.type) {
            Message.TYPE_SENT -> "wiadomosci/1/6/${messageObject.id}/f0"
            else -> "wiadomosci/1/5/${messageObject.id}/f0"
        }

        data.profile?.also { profile ->
            synergiaGet(TAG, endpoint) { text ->
                val doc = Jsoup.parse(text)

                val messageElement = doc.select(".container-message tr")[0].child(1)
                val detailsElement = messageElement.child(1)
                val readElement = messageElement.children().last()

                val body = messageElement.select(".container-message-content").html()

                messageObject.apply {
                    this.body = body

                    clearAttachments()
                    if (messageElement.children().size >= 5) {
                        messageElement.child(3).select("tr").forEachIndexed { i, attachment ->
                            if (i == 0) return@forEachIndexed // Skip the header
                            val filename = attachment.child(0).text().trim()
                            val attachmentId = "wiadomosci\\\\/pobierz_zalacznik\\\\/[0-9]+?\\\\/([0-9]+)\"".toRegex()
                                    .find(attachment.select("img").attr("onclick"))?.get(1)
                                    ?: return@forEachIndexed
                            addAttachment(attachmentId.toLong(), filename, -1)
                        }
                    }
                }

                val messageRecipientList = mutableListOf<MessageRecipientFull>()

                when (messageObject.type) {
                    Message.TYPE_RECEIVED -> {
                        val senderFullName = detailsElement.child(0).select(".left").text()
                        val senderGroupName = "\\[(.+?)]".toRegex().find(senderFullName)?.get(1)?.trim()

                        data.teacherList.singleOrNull { it.id == messageObject.senderId }?.apply {
                            setTeacherType(when (senderGroupName) {
                                /* https://api.librus.pl/2.0/Messages/Role */
                                "Pomoc techniczna Librus", "SuperAdministrator" -> Teacher.TYPE_SUPER_ADMIN
                                "Administrator szkoły" -> Teacher.TYPE_SCHOOL_ADMIN
                                "Dyrektor Szkoły" -> Teacher.TYPE_PRINCIPAL
                                "Nauczyciel" -> Teacher.TYPE_TEACHER
                                "Rodzic", "Opiekun" -> Teacher.TYPE_PARENT
                                "Sekretariat" -> Teacher.TYPE_SECRETARIAT
                                "Uczeń" -> Teacher.TYPE_STUDENT
                                "Pedagog/Psycholog szkolny" -> Teacher.TYPE_PEDAGOGUE
                                "Pracownik biblioteki" -> Teacher.TYPE_LIBRARIAN
                                "Inny specjalista" -> Teacher.TYPE_SPECIALIST
                                "Jednostka Nadrzędna" -> {
                                    typeDescription = "Jednostka Nadrzędna"
                                    Teacher.TYPE_OTHER
                                }
                                "Jednostka Samorządu Terytorialnego" -> {
                                    typeDescription = "Jednostka Samorządu Terytorialnego"
                                    Teacher.TYPE_OTHER
                                }
                                else -> Teacher.TYPE_OTHER
                            })
                        }

                        val readDateText = readElement?.select(".left")?.text()
                        val readDate = when (readDateText.isNotNullNorEmpty()) {
                            true -> Date.fromIso(readDateText)
                            else -> 0
                        }

                        val messageRecipientObject = MessageRecipientFull(
                                profileId = profileId,
                                id = -1,
                                messageId = messageObject.id,
                                readDate = readDate
                        )

                        messageRecipientObject.fullName = profile.accountName
                                ?: profile.studentNameLong

                        messageRecipientList.add(messageRecipientObject)
                    }

                    Message.TYPE_SENT -> {

                        readElement?.select("tr")?.forEachIndexed { i, receiver ->
                            if (i == 0) return@forEachIndexed // Skip the header

                            val receiverFullName = receiver.child(0).text()
                            val receiverName = receiverFullName.split('(')[0].swapFirstLastName()

                            val teacher = data.teacherList.singleOrNull { it.fullName == receiverName }
                            val receiverId = teacher?.id ?: -1

                            val readDate = when (val readDateText = receiver.child(1).text().trim()) {
                                "NIE" -> 0
                                else -> Date.fromIso(readDateText)
                            }

                            val messageRecipientObject = MessageRecipientFull(
                                    profileId = profileId,
                                    id = receiverId,
                                    messageId = messageObject.id,
                                    readDate = readDate
                            )

                            messageRecipientObject.fullName = receiverName

                            messageRecipientList.add(messageRecipientObject)
                        }
                    }
                }

                if (!messageObject.seen) {
                    data.setSeenMetadataList.add(Metadata(
                            messageObject.profileId,
                            Metadata.TYPE_MESSAGE,
                            messageObject.id,
                            true,
                            true
                    ))
                }

                messageObject.recipients = messageRecipientList
                data.messageRecipientList.addAll(messageRecipientList)

                data.messageList.add(messageObject)
                data.messageListReplace = true

                EventBus.getDefault().postSticky(MessageGetEvent(messageObject))
                onSuccess()
            }
        } ?: onSuccess()
    }
}
