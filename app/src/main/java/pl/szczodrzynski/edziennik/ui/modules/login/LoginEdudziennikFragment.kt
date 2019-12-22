/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-23
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.FragmentLoginEdudziennikBinding
import pl.szczodrzynski.edziennik.ui.modules.error.ErrorSnackbar

class LoginEdudziennikFragment : Fragment() {

    private val app by lazy { activity?.application as App? }

    private lateinit var b: FragmentLoginEdudziennikBinding

    private lateinit var nav: NavController
    private lateinit var errorSnackbar: ErrorSnackbar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.also { activity ->
            nav = Navigation.findNavController(activity, R.id.nav_host_fragment)
            errorSnackbar = (activity as LoginActivity).errorSnackbar
        }

        b = FragmentLoginEdudziennikBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    }
}
