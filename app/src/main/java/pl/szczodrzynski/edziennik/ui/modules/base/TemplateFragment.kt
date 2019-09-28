package pl.szczodrzynski.edziennik.ui.modules.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.FragmentTemplateBinding
import pl.szczodrzynski.edziennik.utils.Themes

class TemplateFragment : Fragment() {

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: FragmentTemplateBinding
    private val navController: NavController by lazy { Navigation.findNavController(b.root) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        if (context == null)
            return null
        app = activity.application as App
        context!!.theme.applyStyle(Themes.appTheme, true)
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false)
        // activity, context and profile is valid
        b = FragmentTemplateBinding.inflate(inflater)
        b.refreshLayout.setParent(activity.swipeRefreshLayout)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return


    }
}
