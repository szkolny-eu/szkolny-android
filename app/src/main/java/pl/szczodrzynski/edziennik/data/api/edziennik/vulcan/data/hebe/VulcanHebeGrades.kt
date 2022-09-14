/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-20.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.ext.*
import java.text.DecimalFormat
import kotlin.math.roundToInt

class VulcanHebeGrades(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeGrades"
    }

    init {
        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_GRADES,
            HebeFilterType.BY_PUPIL,
            lastSync = lastSync
        ) { list, _ ->
            list.forEach { grade ->
                val id = grade.getLong("Id") ?: return@forEach

                val column = grade.getJsonObject("Column")
                val category = column.getJsonObject("Category")
                val categoryText = category.getString("Name")

                val teacherId = getTeacherId(grade, "Creator") ?: -1
                val subjectId = getSubjectId(column, "Subject") ?: -1

                val description = column.getString("Name")
                val comment = grade.getString("Comment")
                var value = grade.getFloat("Value")
                var weight = column.getFloat("Weight") ?: 0.0f
                val numerator = grade.getFloat("Numerator ")
                val denominator = grade.getFloat("Denominator")
                val addedDate = getDateTime(grade, "DateModify")

                var finalDescription = ""

                var name = when (numerator != null && denominator != null) {
                    true -> {
                        value = numerator / denominator
                        finalDescription += DecimalFormat("#.##").format(numerator) +
                                "/" + DecimalFormat("#.##").format(denominator)
                        weight = 0.0f
                        (value * 100).roundToInt().toString() + "%"
                    }
                    else -> {
                        if (value == null) weight = 0.0f

                        grade.getString("Content") ?: ""
                    }
                }

                comment?.also {
                    if (name == "") name = it
                    else finalDescription = (if (finalDescription == "") "" else " ") + it
                }

                description?.also {
                    finalDescription = (if (finalDescription == "") "" else " - ") + it
                }

                val columnColor = column.getInt("Color") ?: 0
                val color = if (columnColor == 0)
                    when (name) {
                        "1-", "1", "1+" -> 0xffd65757
                        "2-", "2", "2+" -> 0xff9071b3
                        "3-", "3", "3+" -> 0xffd2ab24
                        "4-", "4", "4+" -> 0xff50b6d6
                        "5-", "5", "5+" -> 0xff2cbd92
                        "6-", "6", "6+" -> 0xff91b43c
                        else -> 0xff3D5F9C
                    }.toInt()
                else
                    columnColor

                val gradeObject = Grade(
                    profileId = profileId,
                    id = id,
                    name = name,
                    type = Grade.TYPE_NORMAL,
                    value = value ?: 0.0f,
                    weight = weight,
                    color = color,
                    category = categoryText,
                    description = finalDescription,
                    comment = null,
                    semester = getSemester(column),
                    teacherId = teacherId,
                    subjectId = subjectId,
                    addedDate = addedDate
                )

                data.gradeList.add(gradeObject)
                data.metadataList.add(
                    Metadata(
                        profileId,
                        Metadata.TYPE_GRADE,
                        id,
                        profile?.empty ?: true,
                        profile?.empty ?: true
                    )
                )
            }

            data.setSyncNext(ENDPOINT_VULCAN_HEBE_GRADES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_VULCAN_HEBE_GRADES)
        }
    }
}
