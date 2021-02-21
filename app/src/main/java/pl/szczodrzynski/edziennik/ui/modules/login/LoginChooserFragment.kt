/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.databinding.LoginChooserFragmentBinding
import pl.szczodrzynski.edziennik.ui.dialogs.RegisterUnavailableDialog
import pl.szczodrzynski.edziennik.ui.modules.feedback.FeedbackActivity
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import kotlin.coroutines.CoroutineContext

class LoginChooserFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginChooserFragment"
        // eggs
        var isRotated = false
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginChooserFragmentBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginChooserFragmentBinding.inflate(inflater)
        return b.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return

        val adapter = LoginChooserAdapter(activity, this::onLoginModeClicked)

        LoginInfo.chooserList = LoginInfo.chooserList
                ?: LoginInfo.list.toMutableList()

        // eggs
        if (isRotated) {
            isRotated = false
            LoginFormFragment.wantEggs = false
            LoginInfo.chooserList = LoginInfo.list.toMutableList()
            val anim = RotateAnimation(
                    180f,
                    0f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f
            )
            anim.interpolator = AccelerateDecelerateInterpolator()
            anim.duration = 500
            anim.fillAfter = true
            activity.getRootView().startAnimation(anim)
        }

        adapter.items = LoginInfo.chooserList!!
        b.list.adapter = adapter
        b.list.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SimpleDividerItemDecoration(context))
        }

        b.helpButton.onClick {
            startActivity(Intent(activity, FeedbackActivity::class.java))
        }

        // eggs
        b.footnoteText.onClick {
            if (!LoginFormFragment.wantEggs || isRotated)
                return@onClick

            val text = b.subtitleText.text.toString()
            if (text.endsWith(".."))
                b.subtitleText.text = text.substring(0, text.length - 2)
            else
                b.subtitleText.text = "$text..."
        }
        var clickCount = 0
        val color = R.color.md_blue_500.resolveColor(app)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        val hueOriginal = hsv[0]
        b.subtitleText.onClick {
            if (isRotated)
                return@onClick
            val text = b.subtitleText.text.toString()
            if (text.endsWith("..") && !text.endsWith("...")) {
                clickCount++
            }
            if (clickCount == 5) {
                val anim = ValueAnimator.ofFloat(0f, 1f)
                anim.duration = 5000
                anim.addUpdateListener {
                    hsv[0] = hueOriginal + it.animatedFraction * 3f * 360f
                    hsv[0] = hsv[0] % 360f
                    b.topLogo.drawable.setTintColor(Color.HSVToColor(Color.alpha(color), hsv))
                }
                anim.start()
            }
        }
        b.topLogo.onClick {
            if (clickCount != 5 || isRotated) {
                clickCount = 0
                return@onClick
            }
            isRotated = true
            val anim = RotateAnimation(
                    0f,
                    180f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f
            )
            anim.interpolator = AccelerateDecelerateInterpolator()
            anim.duration = 2000
            anim.fillAfter = true
            activity.getRootView().startAnimation(anim)

            b.list.smoothScrollToPosition(0)
            adapter.items.add(
                    LoginInfo.Register(
                            loginType = 74,
                            internalName = "eggs",
                            registerName = R.string.eggs,
                            registerLogo = R.drawable.face_1,
                            loginModes = listOf(
                                    LoginInfo.Mode(
                                            loginMode = 0,
                                            name = 0,
                                            icon = 0,
                                            guideText = 0,
                                            credentials = listOf(),
                                            errorCodes = mapOf()
                                    )
                            )
                    )
            )
            adapter.notifyItemInserted(adapter.items.size - 1)
        }

        when {
            activity.loginStores.isNotEmpty() -> {
                // we are navigated here from LoginSummary
                b.cancelButton.isVisible = true
                b.cancelButton.onClick { nav.navigateUp() }
            }
            app.config.loginFinished -> {
                // we are navigated here from AppDrawer
                b.cancelButton.isVisible = true
                b.cancelButton.onClick {
                    activity.setResult(Activity.RESULT_CANCELED)
                    activity.finish()
                }
            }
            else -> {
                // there are no profiles
                b.cancelButton.isVisible = false
            }
        }
    }

    private fun onLoginModeClicked(
            loginType: LoginInfo.Register,
            loginMode: LoginInfo.Mode
    ) {
        if (loginType.internalName == "eggs") {
            nav.navigate(R.id.loginEggsFragment, null, activity.navOptions)
            return
        }

        launch {
            if (!checkAvailability(loginType.loginType))
                return@launch

            if (loginMode.isTesting || loginMode.isDevOnly) {
                MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.login_chooser_testing_title)
                        .setMessage(R.string.login_chooser_testing_text)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            navigateToLoginMode(loginType, loginMode)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                return@launch
            }

            navigateToLoginMode(loginType, loginMode)
        }
    }

    private fun navigateToLoginMode(loginType: LoginInfo.Register, loginMode: LoginInfo.Mode) {
        if (loginMode.isPlatformSelection) {
            nav.navigate(R.id.loginPlatformListFragment, Bundle(
                    "loginType" to loginType.loginType,
                    "loginMode" to loginMode.loginMode
            ), activity.navOptions)
            return
        }

        nav.navigate(R.id.loginFormFragment, Bundle(
                "loginType" to loginType.loginType,
                "loginMode" to loginMode.loginMode
        ), activity.navOptions)
    }

    private suspend fun checkAvailability(loginType: Int): Boolean {
        when (loginType) {
            LOGIN_TYPE_LIBRUS -> "librus"
            LOGIN_TYPE_VULCAN -> "vulcan"
            LOGIN_TYPE_IDZIENNIK -> "idziennik"
            LOGIN_TYPE_MOBIDZIENNIK -> "mobidziennik"
            LOGIN_TYPE_PODLASIE -> "podlasie"
            LOGIN_TYPE_EDUDZIENNIK -> "edudziennik"
            else -> null
        }?.let { registerName ->
            var status = app.config.sync.registerAvailability[registerName]
            if (status == null || status.nextCheckAt < currentTimeUnix()) {
                withContext(Dispatchers.IO) {
                    val api = SzkolnyApi(app)
                    api.runCatching(activity) {
                        val availability = getRegisterAvailability()
                        app.config.sync.registerAvailability = availability
                        status = availability[registerName]
                    }
                }
            }

            if (status?.available != true) {
                if (status != null)
                    RegisterUnavailableDialog(activity, status!!)
                return false
            }
        }
        return true
    }
}
