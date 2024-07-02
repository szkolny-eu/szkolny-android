/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-2.
 */

package pl.szczodrzynski.edziennik.ui.base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import kotlin.coroutines.CoroutineContext

abstract class BaseFragment<B : ViewBinding, A : AppCompatActivity>(
    private val inflater: (inflater: LayoutInflater, parent: ViewGroup?, attachToParent: Boolean) -> B,
) : Fragment(), CoroutineScope {

    protected lateinit var app: App
    protected lateinit var activity: A
    protected lateinit var b: B

    private val job: Job = Job()
    final override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    @Suppress("UNCHECKED_CAST")
    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        activity = getActivity() as? A ?: return null
        context ?: return null
        app = activity.application as App
        b = this.inflater(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded)
            return
        launch {
            onViewCreated(savedInstanceState)
        }
    }

    open suspend fun onViewCreated(savedInstanceState: Bundle?) {}
}
