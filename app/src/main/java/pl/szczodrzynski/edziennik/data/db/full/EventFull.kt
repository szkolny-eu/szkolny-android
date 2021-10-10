/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
 */
package pl.szczodrzynski.edziennik.data.db.full

import androidx.room.Ignore
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.ui.modules.search.Searchable
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class EventFull(
        profileId: Int, id: Long, date: Date, time: Time?,
        topic: String, color: Int?, type: Long,
        teacherId: Long, subjectId: Long, teamId: Long, addedDate: Long = System.currentTimeMillis()
) : Event(
        profileId, id, date, time,
        topic, color, type,
        teacherId, subjectId, teamId, addedDate
), Searchable<EventFull> {
    constructor(event: Event, metadata: Metadata? = null) : this(
            event.profileId, event.id, event.date, event.time,
            event.topic, event.color, event.type,
            event.teacherId, event.subjectId, event.teamId, event.addedDate) {
        event.let {
            addedManually = it.addedManually
            sharedBy = it.sharedBy
            sharedByName = it.sharedByName
            blacklisted = it.blacklisted
            isDownloaded = it.isDownloaded
            homeworkBody = it.homeworkBody
            attachmentIds = it.attachmentIds
            attachmentNames = it.attachmentNames
        }
        metadata?.let {
            seen = it.seen
            notified = it.notified
        }
    }

    var typeName: String? = null
    var typeColor: Int? = null

    var teacherName: String? = null
    var subjectLongName: String? = null
    var subjectShortName: String? = null
    var teamName: String? = null
    var teamCode: String? = null

    @Ignore
    override var searchPriority = 0

    @Ignore
    override var searchHighlightText: CharSequence? = null

    @delegate:Ignore
    override val searchKeywords by lazy {
        listOf(
            listOf(topic, homeworkBody),
            attachmentNames ?: listOf(),
            listOf(subjectLongName),
            listOf(teacherName),
            listOf(sharedByName),
        )
    }

    override fun compareTo(other: Searchable<*>): Int {
        if (other !is EventFull)
            return 0
        return when {
            // ascending sorting
            searchPriority > other.searchPriority -> 1
            searchPriority < other.searchPriority -> -1
            // ascending sorting
            date > other.date -> 1
            date < other.date -> -1
            // ascending sorting
            (time?.value ?: 0) > (other.time?.value ?: 0) -> 1
            (time?.value ?: 0) < (other.time?.value ?: 0) -> -1
            // ascending sorting
            addedDate > other.addedDate -> 1
            addedDate < other.addedDate -> -1
            else -> 0
        }
    }

    // metadata
    var seen = false
    var notified = false

    val eventColor
        get() = color ?: typeColor ?: 0xff2196f3.toInt()
}
