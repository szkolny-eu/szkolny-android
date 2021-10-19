/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-12.
 */

package pl.szczodrzynski.edziennik.ui.debug

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.debug.models.LabJsonArray
import pl.szczodrzynski.edziennik.ui.debug.models.LabJsonElement
import pl.szczodrzynski.edziennik.ui.debug.models.LabJsonObject
import pl.szczodrzynski.edziennik.ui.debug.viewholder.JsonArrayViewHolder
import pl.szczodrzynski.edziennik.ui.debug.viewholder.JsonElementViewHolder
import pl.szczodrzynski.edziennik.ui.debug.viewholder.JsonObjectViewHolder
import pl.szczodrzynski.edziennik.ui.debug.viewholder.JsonSubObjectViewHolder
import pl.szczodrzynski.edziennik.ui.grades.models.ExpandableItemModel
import pl.szczodrzynski.edziennik.ui.grades.viewholder.BindableViewHolder
import kotlin.coroutines.CoroutineContext

class LabJsonAdapter(
        val activity: AppCompatActivity,
        var onJsonElementClick: ((item: LabJsonElement) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), CoroutineScope {
    companion object {
        private const val TAG = "LabJsonAdapter"
        private const val ITEM_TYPE_OBJECT = 0
        private const val ITEM_TYPE_SUB_OBJECT = 1
        private const val ITEM_TYPE_ARRAY = 2
        private const val ITEM_TYPE_ELEMENT = 3
        const val STATE_CLOSED = 0
        const val STATE_OPENED = 1

        fun expand(item: Any, level: Int): MutableList<Any> {
            val path = when (item) {
                is LabJsonObject -> item.key + ":"
                is LabJsonArray -> item.key + ":"
                else -> ""
            }
            val json = when (item) {
                is LabJsonObject -> item.jsonObject
                is LabJsonArray -> item.jsonArray
                is JsonObject -> item
                is JsonArray -> item
                is JsonPrimitive -> item
                else -> return mutableListOf()
            }

            return when (json) {
                is JsonObject -> json.entrySet().mapNotNull { wrap(path + it.key, it.value, level) }
                is JsonArray -> json.mapIndexedNotNull { index, jsonElement -> wrap(path + index.toString(), jsonElement, level) }
                else -> listOf(LabJsonElement("$path?", json, level))
            }.toMutableList()
        }
        fun wrap(key: String, item: JsonElement, level: Int = 0): Any {
            return when (item) {
                is JsonObject -> LabJsonObject(key, item, level + 1)
                is JsonArray -> LabJsonArray(key, item, level + 1)
                else -> LabJsonElement(key, item, level + 1)
            }
        }
    }

    private val app = activity.applicationContext as App

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var items = mutableListOf<Any>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_TYPE_OBJECT -> JsonObjectViewHolder(inflater, parent)
            ITEM_TYPE_SUB_OBJECT -> JsonSubObjectViewHolder(inflater, parent)
            ITEM_TYPE_ARRAY -> JsonArrayViewHolder(inflater, parent)
            ITEM_TYPE_ELEMENT -> JsonElementViewHolder(inflater, parent)
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is LabJsonObject ->
                if (item.level == 1) ITEM_TYPE_OBJECT
                else ITEM_TYPE_SUB_OBJECT
            is LabJsonArray -> ITEM_TYPE_ARRAY
            is LabJsonElement -> ITEM_TYPE_ELEMENT
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
    }

    private val onClickListener = View.OnClickListener { view ->
        val model = view.getTag(R.string.tag_key_model)
        if (model is LabJsonElement) {
            onJsonElementClick?.invoke(model)
            return@OnClickListener
        }
        if (model !is ExpandableItemModel<*>)
            return@OnClickListener
        expandModel(model, view)
    }

    fun expandModel(model: ExpandableItemModel<*>?, view: View?, notifyAdapter: Boolean = true) {
        model ?: return
        val position = items.indexOf(model)
        if (position == -1)
            return

        view?.findViewById<View>(R.id.dropdownIcon)?.let { dropdownIcon ->
            ObjectAnimator.ofFloat(
                    dropdownIcon,
                    View.ROTATION,
                    if (model.state == STATE_CLOSED) 0f else 180f,
                    if (model.state == STATE_CLOSED) 180f else 0f
            ).setDuration(200).start()
        }

        // hide the preview, show summary
        val preview = view?.findViewById<View>(R.id.previewContainer)
        val summary = view?.findViewById<View>(R.id.summaryContainer)
        preview?.isInvisible = model.state == STATE_CLOSED
        summary?.isInvisible = model.state != STATE_CLOSED

        if (model.state == STATE_CLOSED) {

            val subItems = when {
                //model.items.isEmpty() -> listOf(AttendanceEmpty())
                else -> expand(model, model.level)
            }

            model.state = STATE_OPENED
            items.addAll(position + 1, subItems)
            if (notifyAdapter) notifyItemRangeInserted(position + 1, subItems.size)
        }
        else {
            val start = position + 1
            var end: Int = items.size
            for (i in start until items.size) {
                val model1 = items[i]
                val level = (model1 as? ExpandableItemModel<*>)?.level
                    ?: (model1 as? LabJsonElement)?.level
                    ?: model.level
                if (level <= model.level) {
                    end = i
                    break
                } else {
                    if (model1 is ExpandableItemModel<*> && model1.state == STATE_OPENED) {
                        model1.state = STATE_CLOSED
                    }
                }
            }

            if (end != -1) {
                items.subList(start, end).clear()
                if (notifyAdapter) notifyItemRangeRemoved(start, end - start)
            }

            model.state = STATE_CLOSED
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder !is BindableViewHolder<*, *>)
            return

        val viewType = when (holder) {
            is JsonObjectViewHolder -> ITEM_TYPE_OBJECT
            is JsonSubObjectViewHolder -> ITEM_TYPE_SUB_OBJECT
            is JsonArrayViewHolder -> ITEM_TYPE_ARRAY
            is JsonElementViewHolder -> ITEM_TYPE_ELEMENT
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
        holder.itemView.setTag(R.string.tag_key_view_type, viewType)
        holder.itemView.setTag(R.string.tag_key_position, position)
        holder.itemView.setTag(R.string.tag_key_model, item)

        when {
            holder is JsonObjectViewHolder && item is LabJsonObject -> holder.onBind(activity, app, item, position, this)
            holder is JsonSubObjectViewHolder && item is LabJsonObject -> holder.onBind(activity, app, item, position, this)
            holder is JsonArrayViewHolder && item is LabJsonArray -> holder.onBind(activity, app, item, position, this)
            holder is JsonElementViewHolder && item is LabJsonElement -> holder.onBind(activity, app, item, position, this)
        }

        holder.itemView.setOnClickListener(onClickListener)
    }

    override fun getItemCount() = items.size
}
