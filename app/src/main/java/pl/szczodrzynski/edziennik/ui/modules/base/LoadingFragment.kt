package pl.szczodrzynski.edziennik.ui.modules.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import pl.szczodrzynski.edziennik.databinding.FragmentLoadingBinding
import pl.szczodrzynski.edziennik.utils.Themes

class LoadingFragment : Fragment() {

    private lateinit var b: FragmentLoadingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (context == null)
            return null
        context!!.theme.applyStyle(Themes.appTheme, true)
        // activity, context and profile is valid
        b = FragmentLoadingBinding.inflate(inflater)
        return b.root
    }
}
