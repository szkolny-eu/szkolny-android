/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-12.
 */

package pl.szczodrzynski.edziennik.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.databinding.LabPlaygroundBinding
import pl.szczodrzynski.edziennik.ext.asColoredSpannable
import pl.szczodrzynski.edziennik.ext.concat
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ui.base.lazypager.LazyFragment
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class LabPlaygroundFragment : LazyFragment(), CoroutineScope {
    companion object {
        private const val TAG = "LabPlaygroundFragment"
    }

    private lateinit var app: App
    private lateinit var activity: AppCompatActivity
    private lateinit var b: LabPlaygroundBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        activity = (getActivity() as AppCompatActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LabPlaygroundBinding.inflate(inflater)
        return b.root
    }

    override fun onPageCreated(): Boolean {

        Timber.d("textColorSecondary: ${android.R.attr.textColorSecondary.resolveAttr(activity)}")
        b.spanTest1.text = listOf(
            "Text:", "android:textColorSecondary spannable (activity)".asColoredSpannable(
                android.R.attr.textColorSecondary.resolveAttr(activity)
            )
        ).concat(" ")

        b.spanTest2.text = listOf(
            "Text:", "android:textColorSecondary spannable (context)".asColoredSpannable(
                android.R.attr.textColorSecondary.resolveAttr(context)
            )
        ).concat(" ")

        return true
    }
}
