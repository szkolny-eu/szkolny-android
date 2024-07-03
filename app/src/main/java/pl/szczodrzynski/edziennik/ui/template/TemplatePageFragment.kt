/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-30.
 */

package pl.szczodrzynski.edziennik.ui.template

import android.os.Bundle
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.TemplatePageFragmentBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment

class TemplatePageFragment : BaseFragment<TemplatePageFragmentBinding, MainActivity>(
    inflater = TemplatePageFragmentBinding::inflate,
) {

    override suspend fun onViewCreated(savedInstanceState: Bundle?) {
        b.text.text = "Fragment VIEW READY"

        b.button.addOnCheckedChangeListener { button, isChecked ->
//            setSwipeToRefresh(isChecked)
        }
    }
}
