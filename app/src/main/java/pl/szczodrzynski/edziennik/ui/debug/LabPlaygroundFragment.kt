/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-12.
 */

package pl.szczodrzynski.edziennik.ui.debug

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.databinding.LabPlaygroundBinding
import pl.szczodrzynski.edziennik.ext.asColoredSpannable
import pl.szczodrzynski.edziennik.ext.concat
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import timber.log.Timber

class LabPlaygroundFragment : BaseFragment<LabPlaygroundBinding, AppCompatActivity>(
    inflater = LabPlaygroundBinding::inflate,
) {

    override fun getScrollingView() = b.root

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
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
    }
}
