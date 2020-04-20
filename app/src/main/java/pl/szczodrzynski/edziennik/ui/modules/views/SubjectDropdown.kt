/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-23.
 */

package pl.szczodrzynski.edziennik.ui.modules.views

import android.content.Context
import android.content.ContextWrapper
import android.text.InputType
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.crc16
import pl.szczodrzynski.edziennik.data.db.AppDb
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
    var onSubjectSelected: ((subjectId: Long?) -> Unit)? = null
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
                    tag = it.id
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
                    onSubjectSelected?.invoke(null)
                    true
                }
                is Long -> {
                    // selected a subject
                    onSubjectSelected?.invoke(it.tag)
                    true
                }
                else -> false
            }
        }
    }

    fun customNameDialog() {
        activity ?: return
        MaterialDialog.Builder(activity!!)
                .title("Własny przedmiot")
                .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE)
                .input("Nazwa", "") { _: MaterialDialog?, input: CharSequence ->
                    customSubjectName = input.toString()
                    select(Item(
                            -1L * customSubjectName.crc16(),
                            customSubjectName,
                            tag = customSubjectName
                    ))
                    onCustomSubjectSelected?.invoke(customSubjectName)
                }
                .show()
    }

    fun selectSubject(subjectId: Long) {
        if (select(subjectId) == null)
            select(Item(
                    subjectId,
                    "nieznany przedmiot ($subjectId)",
                    tag = subjectId
            ))
    }

    fun selectDefault(subjectId: Long?) {
        if (subjectId == null || selected != null)
            return
        selectSubject(subjectId)
    }

    /**
     * Get the currently selected subject.
     * ### Returns:
     * - null if no valid subject is selected
     * - [Long] - the selected subject's ID
     * - [String] - a custom subject name entered, if [showCustomSubject] == true
     */
    fun getSelected(): Any? {
        return when (selected?.tag) {
            -1L -> null
            is Long -> selected?.tag as Long
            is String -> selected?.tag as String
            else -> null
        }
    }
}
