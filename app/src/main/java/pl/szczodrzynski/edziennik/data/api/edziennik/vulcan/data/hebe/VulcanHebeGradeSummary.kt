/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-22.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_GRADE_SUMMARY
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_GRADE_SUMMARY
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.DAY
import pl.szczodrzynski.edziennik.ext.getString
import pl.szczodrzynski.edziennik.utils.Utils

class VulcanHebeGradeSummary(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeGradeSummary"
    }

    init {
        val entries = mapOf(
            "Entry_1" to
                    if (data.studentSemesterNumber == 1)
                        Grade.TYPE_SEMESTER1_PROPOSED
                    else Grade.TYPE_SEMESTER2_PROPOSED,
            "Entry_2" to
                    if (data.studentSemesterNumber == 1)
                        Grade.TYPE_SEMESTER1_FINAL
                    else Grade.TYPE_SEMESTER2_FINAL
        )

        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_GRADE_SUMMARY,
            HebeFilterType.BY_PUPIL,
            lastSync = lastSync
        ) { list, _ ->
            list.forEach { grade ->
                val subjectId = getSubjectId(grade, "Subject") ?: return@forEach
                val addedDate = getDateTime(grade, "DateModify")

                entries.onEach { (key, type) ->
                    val id = subjectId * -100 - type
                    val entry = grade.getString(key) ?: return@onEach
                    val value = Utils.getGradeValue(entry)
                    val color = Utils.getVulcanGradeColor(entry)

                    val gradeObject = Grade(
                        profileId = profileId,
                        id = id,
                        name = entry,
                        type = type,
                        value = value,
                        weight = 0f,
                        color = color,
                        category = "",
                        description = null,
                        comment = null,
                        semester = data.studentSemesterNumber,
                        teacherId = -1,
                        subjectId = subjectId,
                        addedDate = addedDate,
                        code = null
                    )

                    data.gradeList.add(gradeObject)
                    data.metadataList.add(
                        Metadata(
                            profileId,
                            MetadataType.GRADE,
                            id,
                            profile?.empty ?: true,
                            profile?.empty ?: true
                        )
                    )
                }
            }

            data.setSyncNext(ENDPOINT_VULCAN_HEBE_GRADE_SUMMARY, 1 * DAY)
            onSuccess(ENDPOINT_VULCAN_HEBE_GRADE_SUMMARY)
        }
    }
}
