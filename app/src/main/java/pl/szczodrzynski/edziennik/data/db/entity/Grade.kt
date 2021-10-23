/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-24.
 */
package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index

@Entity(tableName = "grades",
        primaryKeys = ["profileId", "gradeId"],
        indices = [
            Index(value = ["profileId"])
        ])
open class Grade(
        val profileId: Int,
        @ColumnInfo(name = "gradeId")
        val id: Long,
        @ColumnInfo(name = "gradeName")
        var name: String,
        @ColumnInfo(name = "gradeType")
        var type: Int,

        @ColumnInfo(name = "gradeValue")
        var value: Float,
        @ColumnInfo(name = "gradeWeight")
        var weight: Float,
        @ColumnInfo(name = "gradeColor")
        var color: Int,

        @ColumnInfo(name = "gradeCategory")
        var category: String?,
        @ColumnInfo(name = "gradeDescription")
        var description: String?,
        @ColumnInfo(name = "gradeComment")
        var comment: String?,

        @ColumnInfo(name = "gradeSemester")
        val semester: Int,
        val teacherId: Long,
        val subjectId: Long,
        var addedDate: Long = System.currentTimeMillis()
) : Keepable(), Noteable {
    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_SEMESTER1_PROPOSED = 1
        const val TYPE_SEMESTER1_FINAL = 2
        const val TYPE_SEMESTER2_PROPOSED = 3
        const val TYPE_SEMESTER2_FINAL = 4
        const val TYPE_YEAR_PROPOSED = 5
        const val TYPE_YEAR_FINAL = 6
        const val TYPE_POINT_AVG = 10
        const val TYPE_POINT_SUM = 20
        const val TYPE_DESCRIPTIVE = 30
        const val TYPE_DESCRIPTIVE_TEXT = 31
        const val TYPE_TEXT = 40
    }

    @ColumnInfo(name = "gradeValueMax")
    var valueMax: Float? = null
    @ColumnInfo(name = "gradeClassAverage")
    var classAverage: Float? = null

    /**
     * Applies for historical grades. It's the new/replacement grade's ID.
     */
    @ColumnInfo(name = "gradeParentId")
    var parentId: Long? = null
    /**
     * Applies for current grades. If the grade was worse and this is the improved one.
     */
    @ColumnInfo(name = "gradeIsImprovement")
    var isImprovement = false

    @Ignore
    var showAsUnseen = false

    val isImproved
        get() = parentId ?: -1L != -1L

    override fun getNoteType() = Note.OwnerType.GRADE
}

