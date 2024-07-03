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
import com.mikepenz.iconics.typeface.IIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ui.login.LoginActivity
import pl.szczodrzynski.edziennik.utils.SwipeRefreshLayoutNoIndicator
import pl.szczodrzynski.navlib.bottomsheet.items.IBottomSheetItem
import kotlin.coroutines.CoroutineContext

@Suppress("UNCHECKED_CAST")
abstract class BaseFragment<B : ViewBinding, A : AppCompatActivity>(
    private val inflater: ((inflater: LayoutInflater, parent: ViewGroup?, attachToParent: Boolean) -> B)?,
) : Fragment(), CoroutineScope {

    internal lateinit var app: App
    internal lateinit var activity: A
    internal lateinit var b: B

    private var job = Job()
    final override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        job.cancel()
        job = Job()
        activity = getActivity() as? A ?: return null
        context ?: return null
        app = activity.application as App
        b = this.inflater?.invoke(inflater, container, false)
            ?: inflate(inflater, container, false)
            ?: return null
        return b.root
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded)
            return
        (activity as? MainActivity)?.let(::setupMainActivity)
        (activity as? LoginActivity)?.let(::setupLoginActivity)
        // let the UI transition for a moment
        startCoroutineTimer(100L) {
            if (!isAdded)
                return@startCoroutineTimer
            onViewCreated(savedInstanceState)
            (activity as? MainActivity)?.gainAttention()
            (activity as? MainActivity)?.gainAttentionFAB()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job.cancel()
    }

    open fun inflate(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean,
    ): B? = null

    open fun getRefreshLayout(): SwipeRefreshLayoutNoIndicator? = null
    open fun getFab(): Pair<Int, IIcon>? = null
    open fun getMarkAsReadType(): MetadataType? = null
    open fun getBottomSheetItems() = listOf<IBottomSheetItem<*>>()

    open suspend fun onViewCreated(savedInstanceState: Bundle?) {}

    open suspend fun onFabClick() {}
}
