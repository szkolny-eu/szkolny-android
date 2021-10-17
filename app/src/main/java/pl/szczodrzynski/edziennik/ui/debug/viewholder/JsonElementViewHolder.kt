/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-12.
 */

package pl.szczodrzynski.edziennik.ui.debug.viewholder

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.databinding.LabItemElementBinding
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.debug.LabJsonAdapter
import pl.szczodrzynski.edziennik.ui.debug.models.LabJsonElement
import pl.szczodrzynski.edziennik.ui.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.utils.Themes

class JsonElementViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        val b: LabItemElementBinding = LabItemElementBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<LabJsonElement, LabJsonAdapter> {
    companion object {
        private const val TAG = "JsonObjectViewHolder"
    }

    @SuppressLint("SetTextI18n")
    override fun onBind(activity: AppCompatActivity, app: App, item: LabJsonElement, position: Int, adapter: LabJsonAdapter) {
        val contextWrapper = ContextThemeWrapper(activity, Themes.appTheme)

        b.root.setPadding(item.level * 8.dp + 8.dp, 8.dp, 8.dp, 8.dp)

        b.type.text = when (item.jsonElement) {
            is JsonPrimitive -> when {
                item.jsonElement.isNumber -> "Number"
                item.jsonElement.isString -> "String"
                item.jsonElement.isBoolean -> "Boolean"
                else -> "Primitive"
            }
            is JsonNull -> "null"
            else -> null
        }

        val colorSecondary = android.R.attr.textColorSecondary.resolveAttr(activity)
        b.key.text = listOf(
                item.key
                    .substringAfterLast(":")
                    .asColoredSpannable(colorSecondary),
                ": ",
                item.jsonElement.toString().asItalicSpannable()
        ).concat("")
    }
}
