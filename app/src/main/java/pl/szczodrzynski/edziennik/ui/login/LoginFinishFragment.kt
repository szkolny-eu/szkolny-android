/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.login

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.databinding.LoginFinishFragmentBinding
import pl.szczodrzynski.edziennik.ext.Intent
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import kotlin.coroutines.CoroutineContext

class LoginFinishFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginFinishFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginFinishFragmentBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginFinishFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val firstRun = !app.config.loginFinished
        app.config.loginFinished = true

        if (!firstRun) {
            b.subTitle.setText(R.string.login_finish_subtitle_not_first_run)
        }

        b.finishButton.onClick {
            val firstProfileId = arguments?.getInt("firstProfileId") ?: 0
            if (firstProfileId == 0) {
                activity.finish()
                return@onClick
            }

            app.profileLoad(firstProfileId) {
                if (firstRun) {
                    activity.startActivity(Intent(
                            activity,
                            MainActivity::class.java,
                            "profileId" to firstProfileId,
                            "fragmentId" to NavTarget.HOME
                    ))
                }
                else {
                    activity.setResult(Activity.RESULT_OK, Intent(
                            null,
                            "profileId" to firstProfileId,
                            "fragmentId" to NavTarget.HOME
                    ))
                }
                activity.finish()
            }
        }
    }
}
