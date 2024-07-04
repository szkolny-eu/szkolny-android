package pl.szczodrzynski.edziennik.ui.settings

import android.os.Bundle
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.FragmentProfileManagerBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment

class ProfileManagerFragment : BaseFragment<FragmentProfileManagerBinding, MainActivity>(
    inflater = FragmentProfileManagerBinding::inflate,
) {

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
    }
}
