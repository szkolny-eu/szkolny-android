/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-23.
 */

package pl.szczodrzynski.edziennik.ui.modules.views

import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.utils.TextInputDropDown

class TeacherDropdown : TextInputDropDown {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val activity: AppCompatActivity?
        get() {
            var context: Context? = context ?: return null
            if (context is AppCompatActivity) return context
            while (context is ContextWrapper) {
                if (context is AppCompatActivity)
                    return context
                context = context.baseContext
            }
            return null
        }

    lateinit var db: AppDb
    var profileId: Int = 0
    var showNoTeacher = true
    var onTeacherSelected: ((teacherId: Long?) -> Unit)? = null

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
                    tag = it.id
            ) }

            list
        }

        clear().append(teachers)
        isEnabled = true

        setOnChangeListener {
            when (it.tag) {
                -1L -> {
                    // no teacher
                    onTeacherSelected?.invoke(null)
                    true
                }
                is Long -> {
                    // selected a teacher
                    onTeacherSelected?.invoke(it.tag)
                    true
                }
                else -> false
            }
        }
    }

    fun selectTeacher(teacherId: Long) {
        if (select(teacherId) == null)
            select(Item(
                    teacherId,
                    "nieznany nauczyciel ($teacherId)",
                    tag = teacherId
            ))
    }

    fun selectDefault(teacherId: Long?) {
        if (teacherId == null || selected != null)
            return
        selectTeacher(teacherId)
    }

    /**
     * Get the currently selected teacher.
     * ### Returns:
     * - null if no valid teacher is selected
     * - [Long] - the selected teacher's ID
     */
    fun getSelected(): Long? {
        return when (selected?.tag) {
            -1L -> null
            is Long -> selected?.tag as Long
            else -> null
        }
    }
}
