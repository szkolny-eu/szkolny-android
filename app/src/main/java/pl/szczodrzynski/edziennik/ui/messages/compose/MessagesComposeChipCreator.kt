/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-4.
 */

package pl.szczodrzynski.edziennik.ui.messages.compose

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hootsuite.nachos.ChipConfiguration
import com.hootsuite.nachos.NachoTextView
import com.hootsuite.nachos.chip.ChipInfo
import com.hootsuite.nachos.chip.ChipSpan
import com.hootsuite.nachos.chip.ChipSpanChipCreator
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.ext.asSpannable
import pl.szczodrzynski.edziennik.ext.concat
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ext.toColorStateList
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.ui.messages.MessagesUtils
import pl.szczodrzynski.edziennik.utils.Colors
import pl.szczodrzynski.navlib.elevateSurface
import kotlin.collections.List
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.listOf
import kotlin.collections.listOfNotNull
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.collections.sortedBy

class MessagesComposeChipCreator(
    private val activity: AppCompatActivity,
    private val nacho: NachoTextView,
    private val teacherList: List<Teacher>,
) : ChipSpanChipCreator() {

    override fun createChip(context: Context, text: CharSequence, data: Any?): ChipSpan? {
        if (data == null || data !is Teacher)
            return null
        if (data.id !in -24L..0L) {
            nacho.allChips.forEach {
                if (it.data == data) {
                    Toast.makeText(
                        context,
                        R.string.messages_compose_recipient_exists,
                        Toast.LENGTH_SHORT
                    ).show()
                    return null
                }
            }
            val chipSpan = ChipSpan(
                context,
                data.fullName,
                BitmapDrawable(context.resources, data.image),
                data
            )
            chipSpan.setIconBackgroundColor(Colors.stringToMaterialColor(data.fullName))
            return chipSpan
        }

        val type = (data.id * -1).toInt()

        val textColorPrimary = android.R.attr.textColorPrimary.resolveAttr(context)
        val textColorSecondary = android.R.attr.textColorSecondary.resolveAttr(context)

        val sortByCategory = type in listOf(
            Teacher.TYPE_PARENTS_COUNCIL,
            Teacher.TYPE_EDUCATOR,
            Teacher.TYPE_STUDENT
        )

        val adapter = nacho.adapter as? MessagesComposeSuggestionAdapter ?: return null
        val teachers = if (sortByCategory)
            adapter.originalList.sortedBy { it.typeDescription }
        else
            adapter.originalList

        val multiChoiceItems = mutableMapOf<CharSequence, Teacher>()
        val defaultSelectedItems = mutableSetOf<Teacher>()
        teachers.forEach { teacher ->
            if (!teacher.isType(type))
                return@forEach

            val name = teacher.fullName
            val description = when (type) {
                Teacher.TYPE_TEACHER -> null
                Teacher.TYPE_PARENTS_COUNCIL -> teacher.typeDescription
                Teacher.TYPE_SCHOOL_PARENTS_COUNCIL -> null
                Teacher.TYPE_PEDAGOGUE -> null
                Teacher.TYPE_LIBRARIAN -> null
                Teacher.TYPE_SCHOOL_ADMIN -> null
                Teacher.TYPE_SUPER_ADMIN -> null
                Teacher.TYPE_SECRETARIAT -> null
                Teacher.TYPE_PRINCIPAL -> null
                Teacher.TYPE_EDUCATOR -> teacher.typeDescription
                Teacher.TYPE_PARENT -> teacher.typeDescription
                Teacher.TYPE_STUDENT -> teacher.typeDescription
                Teacher.TYPE_SPECIALIST -> null
                else -> teacher.typeDescription
            }
            val displayName = listOfNotNull(
                name.asSpannable(
                    ForegroundColorSpan(textColorPrimary)
                ),
                description?.asSpannable(
                    ForegroundColorSpan(textColorSecondary),
                    AbsoluteSizeSpan(14.dp)
                )
            ).concat("\n")
            multiChoiceItems[displayName] = teacher

            // check the teacher if already added as a recipient
            if (nacho.allChips.firstOrNull { it.data == teacher } != null)
                defaultSelectedItems += teacher
        }

        SimpleDialog<Teacher>(activity) {
            title("Dodaj odbiorców - " + Teacher.typeName(context, type))
            message(R.string.messages_compose_recipients_text_format, Teacher.typeName(activity, type))
            positive(R.string.ok)
            negative(R.string.cancel)
            multi(multiChoiceItems, defaultSelectedItems) { teacher, isChecked ->
                if (isChecked) {
                    val chipInfoList = mutableListOf<ChipInfo>()
                    teacher.image =
                        MessagesUtils.getProfileImage(48, 24, 16, 12, 1, teacher.fullName)
                    chipInfoList.add(ChipInfo(teacher.fullName, teacher))
                    nacho.addTextWithChips(chipInfoList)
                } else {
                    nacho.allChips.forEach {
                        if (it.data == teacher)
                            nacho.chipTokenizer?.deleteChipAndPadding(it, nacho.text)
                    }
                }
            }
        }.show()
        return null
    }

    override fun configureChip(chip: ChipSpan, chipConfiguration: ChipConfiguration) {
        super.configureChip(chip, chipConfiguration)
        chip.setBackgroundColor(elevateSurface(activity, 8).toColorStateList())
        chip.setTextColor(android.R.attr.textColorPrimary.resolveAttr(activity))
    }
}
