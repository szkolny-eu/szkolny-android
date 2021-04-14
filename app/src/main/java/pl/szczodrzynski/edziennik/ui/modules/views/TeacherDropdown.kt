/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-23.
 */

package pl.szczodrzynski.edziennik.ui.modules.views

import android.content.Context
import android.util.AttributeSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.utils.TextInputDropDown

class TeacherDropdown : TextInputDropDown {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    lateinit var db: AppDb
    var profileId: Int = 0
    var showNoTeacher = true
    var onTeacherSelected: ((teacher: Teacher?) -> Unit)? = null

    override fun create(context: Context) {
        super.create(context)
        isEnabled = false
    }

    suspend fun loadItems() {
        val teachers = withContext(Dispatchers.Default) {
            val list = mutableListOf<Item>()

            if (showNoTeacher) {
                list += Item(
                        -1L,
                        context.getString(R.string.dialog_event_manual_no_teacher),
                        tag = -1L
                )
            }

            val teachers = db.teacherDao().getAllNow(profileId)

            list += teachers.map { Item(
                    it.id,
                    it.fullName,
                    tag = it
            ) }

            list
        }

        clear().append(teachers)
        isEnabled = true

        setOnChangeListener {
            when (it.tag) {
                -1L -> {
                    // no teacher
                    deselect()
                    onTeacherSelected?.invoke(null)
                    false
                }
                is Teacher -> {
                    // selected a teacher
                    onTeacherSelected?.invoke(it.tag)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Select a teacher by the [teacherId].
     */
    fun selectTeacher(teacherId: Long): Item? {
        if (teacherId == -1L) {
            deselect()
            return null
        }
        return select(teacherId)
    }

    /**
     * Select a teacher by the [teacherId] **if it's not selected yet**.
     */
    fun selectDefault(teacherId: Long?): Item? {
        if (teacherId == null || selected != null)
            return null
        return selectTeacher(teacherId)
    }

    /**
     * Get the currently selected teacher.
     * ### Returns:
     * - null if no valid teacher is selected
     * - [Teacher] - the selected teacher
     */
    fun getSelected(): Teacher? {
        return when (selected?.tag) {
            -1L -> null
            is Teacher -> selected?.tag as Teacher
            else -> null
        }
    }
}
