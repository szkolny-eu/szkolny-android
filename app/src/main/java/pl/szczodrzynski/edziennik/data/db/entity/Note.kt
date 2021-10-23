/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-16.
 */

package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.*
import pl.szczodrzynski.edziennik.ui.search.Searchable
import pl.szczodrzynski.edziennik.utils.html.BetterHtml

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["profileId", "noteOwnerType", "noteOwnerId"]),
    ],
)
data class Note(
    val profileId: Int,

    @PrimaryKey
    @ColumnInfo(name = "noteId")
    val id: Long,

    @ColumnInfo(name = "noteOwnerType")
    val ownerType: OwnerType?,
    @ColumnInfo(name = "noteOwnerId")
    val ownerId: Long?,
    @ColumnInfo(name = "noteReplacesOriginal")
    val replacesOriginal: Boolean = false,

    @ColumnInfo(name = "noteTopic")
    val topic: String?,
    @ColumnInfo(name = "noteBody")
    val body: String,
    @ColumnInfo(name = "noteColor")
    val color: Int?,

    @ColumnInfo(name = "noteSharedBy")
    val sharedBy: String? = null,
    @ColumnInfo(name = "noteSharedByName")
    val sharedByName: String? = null,

    val addedDate: Long = System.currentTimeMillis(),
) : Searchable<Note> {
    enum class OwnerType(
        val isShareable: Boolean,
        val canReplace: Boolean,
    ) {
        ANNOUNCEMENT(isShareable = true, canReplace = false),
        ATTENDANCE(isShareable = false, canReplace = true),
        BEHAVIOR(isShareable = false, canReplace = false),
        DAY(isShareable = true, canReplace = false),
        EVENT(isShareable = true, canReplace = true),
        EVENT_SUBJECT(isShareable = true, canReplace = false),
        GRADE(isShareable = false, canReplace = true),
        LESSON(isShareable = true, canReplace = false),
        LESSON_SUBJECT(isShareable = true, canReplace = false),
        MESSAGE(isShareable = true, canReplace = false),
    }

    @delegate:Ignore
    @delegate:Transient
    val topicHtml by lazy {
        topic?.let {
            BetterHtml.fromHtml(context = null, it, nl2br = true)
        }
    }

    @delegate:Ignore
    @delegate:Transient
    val bodyHtml by lazy {
        BetterHtml.fromHtml(context = null, body, nl2br = true)
    }

    @Ignore
    @Transient
    override var searchPriority = 0

    @Ignore
    @Transient
    override var searchHighlightText: String? = null

    @delegate:Ignore
    @delegate:Transient
    override val searchKeywords by lazy {
        listOf(
            listOf(topicHtml?.toString(), bodyHtml.toString()),
            listOf(sharedByName),
        )
    }

    override fun compareTo(other: Searchable<*>): Int {
        if (other !is Note)
            return 0
        return when {
            // ascending sorting
            searchPriority > other.searchPriority -> 1
            searchPriority < other.searchPriority -> -1
            // descending sorting
            addedDate > other.addedDate -> -1
            addedDate < other.addedDate -> 1
            else -> 0
        }
    }
}
