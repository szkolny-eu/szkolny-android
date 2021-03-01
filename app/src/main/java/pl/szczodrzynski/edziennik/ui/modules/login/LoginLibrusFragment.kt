/*
 * Copyright (c) Kacper Ziubryniewicz 2021-3-1
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.databinding.LoginLibrusFragmentBinding
import kotlin.coroutines.CoroutineContext

class LoginLibrusFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginLibrusFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginLibrusFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginLibrusFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.librus2021HackWorking100PercentLegit.apply {
            setVideoURI(Uri.parse("https://szkolny.eu/librus.mp4"))
            setMediaController(null)
            requestFocus()
            start()
        }
    }
}
