/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-23.
 */

package pl.szczodrzynski.edziennik.ui.modules.home

import android.widget.TextView
import androidx.core.view.plusAssign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.startCoroutineTimer
import kotlin.coroutines.CoroutineContext

class HomeDummyCard(override val id: Int) : HomeCard, CoroutineScope {
    companion object {
        private const val TAG = "HomeDummyCard"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var timer: Job? = null
    var time = 0

    override fun bind(position: Int, holder: HomeCardAdapter.ViewHolder) { launch {
        holder.root.removeAllViews()
        //holder.setIsRecyclable(false)

        val text = TextView(holder.root.context).apply {
            text = "This is a card #$id"
        }
        holder.root += text

        timer = startCoroutineTimer(repeatMillis = 1000) {
            time++
            text.text = "Coroutine timer at #$id! $time seconds"
        }

        /*val button = MaterialButton(holder.root.context).apply {
            setText("Cancel")
            onClick {
                timer.cancel()
            }
        }
        holder.root += button*/
    }}

    override fun unbind(position: Int, holder: HomeCardAdapter.ViewHolder) {
        timer?.cancel()
        timer = null
    }
}