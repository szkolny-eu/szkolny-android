/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-30.
 */

package pl.szczodrzynski.edziennik.ui.template

import android.os.Bundle
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.TemplatePageFragmentBinding
import pl.szczodrzynski.edziennik.ext.getString
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment

class TemplatePageFragment : BaseFragment<TemplatePageFragmentBinding, MainActivity>(
    inflater = TemplatePageFragmentBinding::inflate,
) {

    override var canRefresh = true
        set(value) {
            field = value
            super.canRefresh = value
        }

    override suspend fun onViewCreated(savedInstanceState: Bundle?) {
        b.text.text = "Fragment VIEW READY"
        b.editText.setText(savedInstanceState.getString("editText", "default"))

        b.button.addOnCheckedChangeListener { button, isChecked ->
            canRefresh = isChecked
        }
        b.button.isChecked = canRefresh
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("editText", b.editText.text.toString())
    }
}
