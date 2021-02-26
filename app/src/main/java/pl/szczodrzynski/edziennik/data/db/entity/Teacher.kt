/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.entity

import android.content.Context
import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.fixName
import pl.szczodrzynski.edziennik.getNameInitials
import pl.szczodrzynski.edziennik.join
import java.util.*

@Entity(tableName = "teachers",
        primaryKeys = ["profileId", "teacherId"])
open class Teacher {
    companion object {
        const val TYPE_TEACHER = 0 // 1
        const val TYPE_EDUCATOR = 1 // 2
        const val TYPE_PEDAGOGUE = 2 // 4
        const val TYPE_LIBRARIAN = 3 // 8
        const val TYPE_SECRETARIAT = 4 // 16
        const val TYPE_PRINCIPAL = 5 // 32
        const val TYPE_SCHOOL_ADMIN = 6 // 64
        // not teachers
        const val TYPE_SPECIALIST = 7 // 128
        const val TYPE_SUPER_ADMIN = 10 // 1024
        const val TYPE_STUDENT = 12 // 4096
        const val TYPE_PARENT = 14 // 16384
        const val TYPE_PARENTS_COUNCIL = 15 // 32768
        const val TYPE_SCHOOL_PARENTS_COUNCIL = 16 // 65536
        const val TYPE_OTHER = 24 // 16777216
        const val IS_TEACHER_MASK = 127

        val types: List<Int> by lazy { listOf(
                TYPE_TEACHER,
                TYPE_EDUCATOR,
                TYPE_PEDAGOGUE,
                TYPE_LIBRARIAN,
                TYPE_SECRETARIAT,
                TYPE_PRINCIPAL,
                TYPE_SCHOOL_ADMIN,
                TYPE_SPECIALIST,
                TYPE_SUPER_ADMIN,
                TYPE_STUDENT,
                TYPE_PARENT,
                TYPE_PARENTS_COUNCIL,
                TYPE_SCHOOL_PARENTS_COUNCIL,
                TYPE_OTHER
        ) }

        fun typeName(c: Context, type: Int, typeDescription: String? = null): String {
            val suffix = typeDescription?.let { " ($typeDescription)" } ?: ""
            return when (type) {
                TYPE_TEACHER -> c.getString(R.string.teacher_teacher)
                TYPE_PARENTS_COUNCIL -> c.getString(R.string.teacher_parents_council) + suffix
                TYPE_SCHOOL_PARENTS_COUNCIL -> c.getString(R.string.teacher_school_parents_council)
                TYPE_PEDAGOGUE -> c.getString(R.string.teacher_pedagogue)
                TYPE_LIBRARIAN -> c.getString(R.string.teacher_librarian)
                TYPE_SCHOOL_ADMIN -> c.getString(R.string.teacher_school_admin)
                TYPE_SUPER_ADMIN -> c.getString(R.string.teacher_super_admin)
                TYPE_SECRETARIAT -> c.getString(R.string.teacher_secretariat)
                TYPE_PRINCIPAL -> c.getString(R.string.teacher_principal)
                TYPE_EDUCATOR -> c.getString(R.string.teacher_educator) + suffix
                TYPE_PARENT -> c.getString(R.string.teacher_parent) + suffix
                TYPE_STUDENT -> c.getString(R.string.teacher_student) + suffix
                TYPE_SPECIALIST -> c.getString(R.string.teacher_specialist)
                else -> c.getString(R.string.teacher_other) + suffix
            }
        }
    }

    var profileId: Int

    @ColumnInfo(name = "teacherId")
    var id: Long

    @ColumnInfo(name = "teacherLoginId")
    var loginId: String? = null

    @ColumnInfo(name = "teacherName")
    var name: String? = ""

    @ColumnInfo(name = "teacherSurname")
    var surname: String? = ""

    @ColumnInfo(name = "teacherType")
    var type = 0

    @ColumnInfo(name = "teacherTypeDescription")
    var typeDescription: String? = null

    fun isType(checkingType: Int): Boolean {
        return type and (1 shl checkingType) >= 1
    }

    val isTeacher: Boolean
        get() = type <= IS_TEACHER_MASK

    fun setTeacherType(i: Int) {
        type = type or (1 shl i)
    }

    fun unsetTeacherType(i: Int) {
        type = type and (1 shl i).inv()
    }

    fun getTypeText(c: Context): String {
        val list = mutableListOf<String>()
        types.forEach {
            if (isType(it))
                list += typeName(c, it, typeDescription)
        }
        return list.join(", ")
    }


    @Ignore
    var image: Bitmap? = null

    /**
     * Used in Message composing - searching in AutoComplete bolds
     * the typed part of the full name.
     */
    @Ignore
    var recipientDisplayName: CharSequence? = null
    /**
     * Used in Message composing - determining the priority
     * of search result, based on the search phrase match
     * (beginning of sentence, beginning of word, middle of word).
     */
    @Ignore
    var recipientWeight: Int = 0

    @Ignore
    constructor(profileId: Int, id: Long) {
        this.profileId = profileId
        this.id = id
    }



    @Ignore
    constructor(profileId: Int, id: Long, name: String, surname: String) {
        this.profileId = profileId
        this.id = id
        this.name = name
        this.surname = surname
    }

    constructor(profileId: Int, id: Long, name: String, surname: String, loginId: String?) {
        this.profileId = profileId
        this.id = id
        this.name = name
        this.surname = surname
        this.loginId = loginId
    }

    @Ignore
    constructor(teacher: Teacher) {
        teacher.let {
            this.profileId = it.profileId
            this.id = it.id
            this.loginId = it.loginId
            this.name = it.name
            this.surname = it.surname
            this.type = it.type
            this.typeDescription = it.typeDescription
            this.image = it.image
            this.recipientDisplayName = it.recipientDisplayName
        }
    }

    @delegate:Ignore
    val fullName by lazy { "$name $surname".fixName() }

    @delegate:Ignore
    val fullNameLastFirst by lazy { "$surname $name".fixName() }

    @delegate:Ignore
    val initialsLastFirst by lazy { fullNameLastFirst.getNameInitials() }

    val shortName: String
        get() = (if (name == null || name?.length == 0) "" else name!![0].toString()) + "." + surname

    override fun toString(): String {
        return "Teacher{" +
                "profileId=" + profileId +
                ", id=" + id +
                ", loginId='" + loginId + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", type=" + dumpType() +
                ", typeDescription='" + typeDescription + '\'' +
                '}'
    }

    private fun dumpType(): String {
        val typeList: MutableList<String> = ArrayList()
        if (isType(TYPE_TEACHER)) typeList.add("TYPE_TEACHER")
        if (isType(TYPE_EDUCATOR)) typeList.add("TYPE_EDUCATOR($typeDescription)")
        if (isType(TYPE_PEDAGOGUE)) typeList.add("TYPE_PEDAGOGUE")
        if (isType(TYPE_LIBRARIAN)) typeList.add("TYPE_PEDAGOGUE")
        if (isType(TYPE_SECRETARIAT)) typeList.add("TYPE_SECRETARIAT")
        if (isType(TYPE_PRINCIPAL)) typeList.add("TYPE_PRINCIPAL")
        if (isType(TYPE_SCHOOL_ADMIN)) typeList.add("TYPE_SCHOOL_ADMIN")
        if (isType(TYPE_SPECIALIST)) typeList.add("TYPE_SPECIALIST")
        if (isType(TYPE_SUPER_ADMIN)) typeList.add("TYPE_SUPER_ADMIN")
        if (isType(TYPE_STUDENT)) typeList.add("TYPE_STUDENT")
        if (isType(TYPE_PARENT)) typeList.add("TYPE_PARENT")
        if (isType(TYPE_PARENTS_COUNCIL)) typeList.add("TYPE_PARENTS_COUNCIL")
        if (isType(TYPE_SCHOOL_PARENTS_COUNCIL)) typeList.add("TYPE_SCHOOL_PARENTS_COUNCIL")
        if (isType(TYPE_OTHER)) typeList.add("TYPE_OTHER($typeDescription)")
        return typeList.join(", ")
    }
}
