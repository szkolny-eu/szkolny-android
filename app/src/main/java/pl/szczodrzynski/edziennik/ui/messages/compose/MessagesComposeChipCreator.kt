/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-4.
 */

package pl.szczodrzynski.edziennik.ui.messages.compose

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hootsuite.nachos.ChipConfiguration
import com.hootsuite.nachos.NachoTextView
import com.hootsuite.nachos.chip.ChipInfo
import com.hootsuite.nachos.chip.ChipSpan
import com.hootsuite.nachos.chip.ChipSpanChipCreator
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.messages.MessagesUtils
import pl.szczodrzynski.edziennik.utils.Colors
import pl.szczodrzynski.navlib.elevateSurface

class MessagesComposeChipCreator(
    private val context: Context,
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

        val category = mutableListOf<Teacher>()
        val categoryNames = mutableListOf<CharSequence>()
        val categoryCheckedItems = mutableListOf<Boolean>()
        teachers.forEach { teacher ->
            if (!teacher.isType(type))
                return@forEach

            category += teacher
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
            categoryNames += listOfNotNull(
                name.asSpannable(
                    ForegroundColorSpan(textColorPrimary)
                ),
                description?.asSpannable(
                    ForegroundColorSpan(textColorSecondary),
                    AbsoluteSizeSpan(14.dp)
                )
            ).concat("\n")

            // check the teacher if already added as a recipient
            categoryCheckedItems += nacho.allChips.firstOrNull { it.data == teacher } != null
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Dodaj odbiorców - " + Teacher.typeName(context, type))
            //.setMessage(getString(R.string.messages_compose_recipients_text_format, Teacher.typeName(activity, type)))
            .setPositiveButton("OK", null)
            .setNeutralButton("Anuluj", null)
            .setMultiChoiceItems(
                categoryNames.toTypedArray(),
                categoryCheckedItems.toBooleanArray()
            ) { _, which, isChecked ->
                val teacher = category[which]
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
            .show()
        return null
    }

    override fun configureChip(chip: ChipSpan, chipConfiguration: ChipConfiguration) {
        super.configureChip(chip, chipConfiguration)
        chip.setBackgroundColor(elevateSurface(context, 8).toColorStateList())
        chip.setTextColor(android.R.attr.textColorPrimary.resolveAttr(context))
    }
}
