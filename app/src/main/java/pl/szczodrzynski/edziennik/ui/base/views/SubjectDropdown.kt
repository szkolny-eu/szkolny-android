/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-23.
 */

package pl.szczodrzynski.edziennik.ui.base.views

import android.content.Context
import android.content.ContextWrapper
import android.text.InputType
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.Subject
import pl.szczodrzynski.edziennik.ext.crc16
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.utils.TextInputDropDown

class SubjectDropdown : TextInputDropDown {
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
    var showNoSubject = true
    var showCustomSubject = false
    var customSubjectName = ""
    var onSubjectSelected: ((subject: Subject?) -> Unit)? = null
    var onCustomSubjectSelected: ((subjectName: String) -> Unit)? = null

    override fun create(context: Context) {
        super.create(context)
        isEnabled = false
    }

    suspend fun loadItems() {
        val subjects = withContext(Dispatchers.Default) {
            val list = mutableListOf<Item>()

            if (showNoSubject) {
                list += Item(
                        -1L,
                        context.getString(R.string.dialog_event_manual_no_subject),
                        tag = -1L
                )
            }

            if (showCustomSubject) {
                list += Item(
                        -2L,
                        context.getString(R.string.dropdown_subject_custom),
                        tag = -2L
                )
            }

            val subjects = db.subjectDao().getAllNow(profileId)

            list += subjects.map { Item(
                    it.id,
                    it.longName,
                    tag = it
            ) }

            list
        }

        clear().append(subjects)
        isEnabled = true

        setOnChangeListener {
            when (it.tag) {
                -2L -> {
                    // custom subject
                    customNameDialog()
                    false
                }
                -1L -> {
                    // no subject
                    deselect()
                    onSubjectSelected?.invoke(null)
                    false
                }
                is Subject -> {
                    // selected a subject
                    onSubjectSelected?.invoke(it.tag)
                    true
                }
                else -> false
            }
        }
    }

    private fun customNameDialog() {
        activity ?: return
        SimpleDialog<Unit>(activity!!) {
            title("Własny przedmiot")
            input(
                type = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE,
                hint = "Nazwa",
            )
            positive(R.string.ok) {
                customSubjectName = getInput()?.text?.toString() ?: ""
                select(
                    Item(
                        -1L * customSubjectName.crc16(),
                        customSubjectName,
                        tag = customSubjectName
                    )
                )
                onCustomSubjectSelected?.invoke(customSubjectName)
            }
            negative(R.string.cancel)
        }.show()
    }

    /**
     * Select a subject by the [subjectId].
     */
    fun selectSubject(subjectId: Long): Item? {
        if (subjectId == -1L) {
            deselect()
            return null
        }
        return select(subjectId)
    }

    /**
     * Select a subject by the [subjectId] **if it's not selected yet**.
     */
    fun selectDefault(subjectId: Long?): Item? {
        if (subjectId == null || selected != null)
            return null
        return selectSubject(subjectId)
    }

    /**
     * Get the currently selected subject.
     * ### Returns:
     * - null if no valid subject is selected
     * - [Subject] - the selected subject
     * - [String] - a custom subject name entered, if [showCustomSubject] == true
     */
    fun getSelected(): Any? {
        return when (selected?.tag) {
            -1L -> null
            is Subject -> selected?.tag as Subject
            is String -> selected?.tag as String
            else -> null
        }
    }
}
