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
    var profileId: Int,

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
    val color: Long?,

    @ColumnInfo(name = "noteSharedBy")
    var sharedBy: String? = null,
    @ColumnInfo(name = "noteSharedByName")
    val sharedByName: String? = null,

    val addedDate: Long = System.currentTimeMillis(),
) : Searchable<Note> {
    enum class OwnerType(
        val isShareable: Boolean,
        val canReplace: Boolean,
    ) {
        EVENT(isShareable = true, canReplace = true),
        DAY(isShareable = true, canReplace = false),
        LESSON(isShareable = true, canReplace = true),
        MESSAGE(isShareable = true, canReplace = false),
        EVENT_SUBJECT(isShareable = true, canReplace = false),
        LESSON_SUBJECT(isShareable = true, canReplace = false),
        GRADE(isShareable = false, canReplace = true),
        ATTENDANCE(isShareable = false, canReplace = true),
        BEHAVIOR(isShareable = false, canReplace = false),
        ANNOUNCEMENT(isShareable = true, canReplace = false),
    }

    enum class Color(val value: Long?) {
        NONE(null),
        RED(0xffff1744),
        ORANGE(0xffff9100),
        YELLOW(0xffffea00),
        GREEN(0xff00c853),
        TEAL(0xff00bfa5),
        BLUE(0xff0091ea),
        DARK_BLUE(0xff304ffe),
        PURPLE(0xff6200ea),
        PINK(0xffd500f9),
        BROWN(0xff795548),
        GREY(0xff9e9e9e),
        BLACK(0xff000000),
    }

    val isShared
        get() = sharedBy != null && sharedByName != null
    val canEdit
        get() = !isShared || sharedBy == "self"

    // used when receiving notes
    @Ignore
    var teamCode: String? = null

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
    var isCategoryItem = false

    @Ignore
    @Transient
    override var searchPriority = 0

    @Ignore
    @Transient
    override var searchHighlightText: String? = null

    @delegate:Ignore
    @delegate:Transient
    override val searchKeywords by lazy {
        if (isCategoryItem)
            return@lazy emptyList()
        listOf(
            listOf(topicHtml?.toString(), bodyHtml.toString()),
            listOf(sharedByName),
        )
    }

    override fun compareTo(other: Searchable<*>): Int {
        if (other !is Note)
            return 0
        val order = ownerType?.ordinal ?: -1
        val otherOrder = other.ownerType?.ordinal ?: -1
        return when {
            // custom ascending sorting
            order > otherOrder -> 1
            order < otherOrder -> -1
            // all category elements stay at their original position
            isCategoryItem -> 0
            other.isCategoryItem -> 0
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
