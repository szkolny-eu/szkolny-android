/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.login

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ui.login.viewholder.PlatformViewHolder
import kotlin.coroutines.CoroutineContext

class LoginPlatformAdapter(
        val activity: AppCompatActivity,
        val onPlatformClick: ((platform: LoginInfo.Platform) -> Unit)? = null
) : RecyclerView.Adapter<PlatformViewHolder>(), CoroutineScope {
    companion object {
        private const val TAG = "LoginPlatformAdapter"
    }

    private val app = activity.applicationContext as App
    // optional: place the manager here

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var items = listOf<LoginInfo.Platform>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlatformViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return PlatformViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: PlatformViewHolder, position: Int) {
        val item = items[position]
        holder.onBind(activity, app, item, position, this)
        onPlatformClick?.let {
            holder.b.root.onClick { _ -> it(item) }
        }
    }

    override fun getItemCount() = items.size
}
