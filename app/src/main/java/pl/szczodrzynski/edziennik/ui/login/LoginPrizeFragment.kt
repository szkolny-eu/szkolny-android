/*
 * Copyright (c) Kuba Szczodrzyński 2020-10-18.
 */

package pl.szczodrzynski.edziennik.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import coil.load
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.LoginPrizeFragmentBinding
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.ui.dialogs.RestartDialog
import kotlin.coroutines.CoroutineContext

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
            SimpleDialog<Unit>(activity) {
                title(R.string.are_you_sure)
                message(R.string.dev_mode_enable_warning)
                positive(R.string.yes) {
                    app.config.devMode = true
                    App.devMode = true
                    RestartDialog(activity).show()
                }
                negative(R.string.no) {
                    app.config.devMode = BuildConfig.DEBUG
                    App.devMode = BuildConfig.DEBUG
                    activity.finish()
                }
            }.show()
        }
    }
}
