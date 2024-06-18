package pl.szczodrzynski.navlib.bottomsheet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlin.reflect.KClass

class ViewHolderProvider {
    private val viewHolderFactories = hashMapOf<Int, Pair<Int, (View) -> RecyclerView.ViewHolder>>()

    fun provideViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val (layoutId: Int, f: (View) -> RecyclerView.ViewHolder) = viewHolderFactories[viewType]!!
        val view = LayoutInflater.from(viewGroup.getContext()).inflate(layoutId, viewGroup, false)
        return f(view)
    }

    fun registerViewHolderFactory(viewType: Int, layoutId: Int, viewHolderFactory: (View) -> RecyclerView.ViewHolder) {
        viewHolderFactories[viewType] = Pair(layoutId, viewHolderFactory)
    }
}