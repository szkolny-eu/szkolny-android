package pl.szczodrzynski.edziennik.ui.messages.compose

import android.content.Context
import android.graphics.Typeface.BOLD
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.ext.asSpannable
import pl.szczodrzynski.edziennik.ext.cleanDiacritics
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ui.messages.MessagesUtils.getProfileImage
import java.util.*

class MessagesComposeSuggestionAdapter(
        context: Context,
        val originalList: List<Teacher>
) : ArrayAdapter<Teacher>(context, 0, originalList)
{

    private var teacherList = originalList.toList()
    private val filter by lazy { ArrayFilter() }
    private val comparator by lazy { Comparator { o1: Teacher, o2: Teacher -> o1.recipientWeight - o2.recipientWeight } }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItem = convertView ?: LayoutInflater.from(context).inflate(R.layout.messages_compose_suggestion_item, parent, false)

        val teacher = teacherList[position]
        val name = listItem.findViewById<TextView>(R.id.name)
        val type = listItem.findViewById<TextView>(R.id.type)
        val image = listItem.findViewById<ImageView>(R.id.image)

        if (teacher.image == null)
            teacher.image = getProfileImage(48, 24, 16, 12, 1, teacher.fullName)

        if (teacher.id in -24L..0L) {
            name.text = Teacher.typeName(context, (teacher.id * -1).toInt())
            type.setText(R.string.teachers_browse_category)
            image.setImageBitmap(null)
        } else {
            name.text = teacher.recipientDisplayName ?: teacher.fullName
            type.text = teacher.getTypeText(context)
            image.setImageBitmap(teacher.image)
        }
        return listItem
    }

    override fun getCount(): Int {
        return teacherList.size
    }

    override fun getItem(position: Int): Teacher? {
        return teacherList[position]
    }

    override fun getPosition(item: Teacher?): Int {
        return teacherList.indexOf(item)
    }

    override fun getFilter(): Filter {
        return filter
    }

    inner class ArrayFilter : Filter() {
        override fun performFiltering(prefix: CharSequence?): FilterResults {
            val results = FilterResults()

            if (prefix == null) {
                originalList.filter { it.id in -24L..0L }.let {
                    results.values = it
                    results.count = it.size
                }
            } else if (prefix.isEmpty()) {
                results.values = originalList
                results.count = originalList.size
            } else {
                val prefixString = prefix.toString()

                val list = mutableListOf<Teacher>()

                originalList.forEach { teacher ->
                    teacher.recipientWeight = getMatchWeight(teacher.fullName, prefixString)

                    if (teacher.recipientWeight != 100) {
                        teacher.recipientDisplayName = teacher.fullName.asSpannable(
                                StyleSpan(BOLD),
                                BackgroundColorSpan(R.attr.colorControlHighlight.resolveAttr(context)),
                                substring = prefixString,
                                ignoreCase = true,
                                ignoreDiacritics = true
                        )
                        list += teacher
                    }
                }

                Collections.sort(list, comparator)
                results.values = list
                results.count = list.size
            }
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            results.values?.let { teacherList = it as List<Teacher> }
            if (results.count > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }

    private fun getMatchWeight(name: CharSequence?, prefix: String): Int {
        if (name == null)
            return 100

        val nameClean = name.cleanDiacritics()

        // First match against the whole, non-split value
        if (nameClean.startsWith(prefix, ignoreCase = true) || name.startsWith(prefix, ignoreCase = true)) {
            return 1
        } else {
            // check if prefix matches any of the words
            val words = nameClean.split(" ").toTypedArray() + name.split(" ").toTypedArray()
            for (word in words) {
                if (word.startsWith(prefix, ignoreCase = true)) {
                    return 2
                }
            }
        }
        // finally check if the prefix matches any part of the name
        if (nameClean.contains(prefix, ignoreCase = true) || name.contains(prefix, ignoreCase = true)) {
            return 3
        }

        return 100
    }
}
