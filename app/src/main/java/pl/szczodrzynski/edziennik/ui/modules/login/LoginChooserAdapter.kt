/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.modules.grades.models.ExpandableItemModel
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.ui.modules.login.viewholder.ModeViewHolder
import pl.szczodrzynski.edziennik.ui.modules.login.viewholder.RegisterViewHolder
import kotlin.coroutines.CoroutineContext

class LoginChooserAdapter(
        val activity: AppCompatActivity,
        val onModeClick: ((loginType: LoginInfo.Register, loginMode: LoginInfo.Mode) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), CoroutineScope {
    companion object {
        private const val TAG = "LoginChooserAdapter"
        private const val ITEM_TYPE_REGISTER = 0
        private const val ITEM_TYPE_MODE = 1
        const val STATE_CLOSED = 0
        const val STATE_OPENED = 1
    }

    private val app = activity.applicationContext as App
    // optional: place the manager here

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var items = mutableListOf<Any>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_TYPE_REGISTER -> RegisterViewHolder(inflater, parent)
            ITEM_TYPE_MODE -> ModeViewHolder(inflater, parent)
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is LoginInfo.Register -> ITEM_TYPE_REGISTER
            is LoginInfo.Mode -> ITEM_TYPE_MODE
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
    }

    private val onClickListener = View.OnClickListener { view ->
        val model = view.getTag(R.string.tag_key_model)
        if (model is LoginInfo.Register && model.loginModes.size == 1) {
            onModeClick?.invoke(model, model.loginModes.first())
            return@OnClickListener
        }
        if (model is LoginInfo.Mode) {
            val loginInfo = items.firstOrNull {
                it is LoginInfo.Register && it.loginModes.contains(model)
            } as? LoginInfo.Register
                    ?: return@OnClickListener

            onModeClick?.invoke(loginInfo, model)
            return@OnClickListener
        }
        if (model !is LoginInfo.Register)
            return@OnClickListener
        expandModel(model, view)
    }

    private fun expandModel(model: LoginInfo.Register, view: View?, notifyAdapter: Boolean = true) {
        val position = items.indexOf(model)
        if (position == -1)
            return

        if (model.state == STATE_CLOSED) {

            val subItems = model.items

            model.state = STATE_OPENED
            items.addAll(position + 1, subItems)
            if (notifyAdapter) notifyItemRangeInserted(position + 1, subItems.size)
        }
        else {
            val start = position + 1
            var end: Int = items.size
            for (i in start until items.size) {
                val model1 = items[i]
                val level = (model1 as? ExpandableItemModel<*>)?.level ?: 3
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

        holder.itemView.setTag(R.string.tag_key_model, item)

        when {
            holder is RegisterViewHolder && item is LoginInfo.Register -> holder.onBind(activity, app, item, position, this)
            holder is ModeViewHolder && item is LoginInfo.Mode -> holder.onBind(activity, app, item, position, this)
        }

        holder.itemView.setOnClickListener(onClickListener)
    }

    override fun getItemCount() = items.size
}
