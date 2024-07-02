/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-30.
 */

package pl.szczodrzynski.edziennik.ui.template

import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.TemplatePageFragmentBinding
import pl.szczodrzynski.edziennik.ui.base.lazypager.LazyFragment

class TemplatePageFragment : LazyFragment<TemplatePageFragmentBinding, MainActivity>(
    inflater = TemplatePageFragmentBinding::inflate,
) {

    override fun onPageCreated(): Boolean {
        b.text.text = "Fragment $position"

        b.button.addOnCheckedChangeListener { button, isChecked ->
            setSwipeToRefresh(isChecked)
        }
        return true
    }
}
