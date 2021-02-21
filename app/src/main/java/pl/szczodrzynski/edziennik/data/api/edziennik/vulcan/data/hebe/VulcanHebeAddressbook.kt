package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import androidx.core.util.set
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_ADDRESSBOOK
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_ADDRESSBOOK
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.entity.Teacher.Companion.TYPE_EDUCATOR
import pl.szczodrzynski.edziennik.data.db.entity.Teacher.Companion.TYPE_OTHER
import pl.szczodrzynski.edziennik.data.db.entity.Teacher.Companion.TYPE_PARENT
import pl.szczodrzynski.edziennik.data.db.entity.Teacher.Companion.TYPE_PARENTS_COUNCIL
import pl.szczodrzynski.edziennik.data.db.entity.Teacher.Companion.TYPE_STUDENT
import pl.szczodrzynski.edziennik.data.db.entity.Teacher.Companion.TYPE_TEACHER
import kotlin.text.replace

class VulcanHebeAddressbook(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeAddressbook"
    }

    private fun String.removeUnitName(unitName: String?): String {
        return (unitName ?: data.schoolShort)?.let {
            this.replace("($it)", "").trim()
        } ?: this
    }

    init {
        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_ADDRESSBOOK,
            HebeFilterType.BY_PERSON,
            lastSync = lastSync,
            includeFilterType = false
        ) { list, _ ->
            list.forEach { person ->
                val id = person.getString("Id") ?: return@forEach
                val loginId = person.getString("LoginId") ?: return@forEach

                val idType = id.split("-")
                    .getOrNull(0)
                val idLong = id.split("-")
                    .getOrNull(1)
                    ?.toLongOrNull()
                    ?: return@forEach

                val typeBase = when (idType) {
                    "e" -> TYPE_TEACHER
                    "c" -> TYPE_PARENT
                    "p" -> TYPE_STUDENT
                    else -> TYPE_OTHER
                }

                val name = person.getString("Name") ?: ""
                val surname = person.getString("Surname") ?: ""
                val namePrefix = "$surname $name - "

                val teacher = data.teacherList[idLong] ?: Teacher(
                    data.profileId,
                    idLong,
                    name,
                    surname,
                    loginId
                ).also {
                    data.teacherList[idLong] = it
                }

                person.getJsonArray("Roles")?.asJsonObjectList()?.onEach { role ->
                    var roleText: String? = null
                    val unitName = role.getString("ConstituentUnitSymbol")

                    val personType = when (role.getInt("RoleOrder")) {
                        0 -> { /* Wychowawca */
                            roleText = role.getString("ClassSymbol")
                                ?.removeUnitName(unitName)
                            TYPE_EDUCATOR
                        }
                        1 -> TYPE_TEACHER /* Nauczyciel */
                        2 -> return@onEach /* Pracownik */
                        3 -> { /* Rada rodziców */
                            roleText = role.getString("Address")
                                ?.removeUnitName(unitName)
                                ?.removePrefix(namePrefix)
                                ?.trim()
                            TYPE_PARENTS_COUNCIL
                        }
                        5 -> {
                            roleText = role.getString("RoleName")
                                ?.plus(" - ")
                                ?.plus(
                                    role.getString("Address")
                                        ?.removeUnitName(unitName)
                                        ?.removePrefix(namePrefix)
                                        ?.trim()
                                )
                            TYPE_STUDENT
                        }
                        else -> TYPE_OTHER
                    }

                    teacher.setTeacherType(personType)
                    teacher.typeDescription = roleText
                }

                if (teacher.type == 0)
                    teacher.setTeacherType(typeBase)
            }

            data.setSyncNext(ENDPOINT_VULCAN_HEBE_ADDRESSBOOK, 2 * DAY)
            onSuccess(ENDPOINT_VULCAN_HEBE_ADDRESSBOOK)
        }
    }
}
