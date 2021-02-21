/*
 * Copyright (c) Kuba Szczodrzyński 2020-10-18.
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import coil.api.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.LoginPrizeFragmentBinding
import pl.szczodrzynski.edziennik.onClick
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

class LoginPrizeFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginPrizeFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginPrizeFragmentBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginPrizeFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.button.load("https://szkolny.eu/game/button.png")
        b.button.onClick {
            MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.are_you_sure)
                    .setMessage(R.string.dev_mode_enable_warning)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        app.config.debugMode = true
                        App.devMode = true
                        MaterialAlertDialogBuilder(activity)
                                .setTitle("Restart")
                                .setMessage("Wymagany restart aplikacji")
                                .setPositiveButton(R.string.ok) { _, _ ->
                                    Process.killProcess(Process.myPid())
                                    Runtime.getRuntime().exit(0)
                                    exitProcess(0)
                                }
                                .setCancelable(false)
                                .show()
                    }
                    .setNegativeButton(R.string.no) { _, _ ->
                        app.config.debugMode = false
                        App.devMode = false
                        activity.finish()
                    }
                    .show()
        }
    }
}
