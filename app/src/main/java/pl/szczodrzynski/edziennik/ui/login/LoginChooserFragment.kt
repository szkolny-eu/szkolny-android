/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.login

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
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.core.manager.AvailabilityManager.Error.Type
import pl.szczodrzynski.edziennik.data.enums.LoginMode
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.databinding.LoginChooserFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.resolveColor
import pl.szczodrzynski.edziennik.ext.setText
import pl.szczodrzynski.edziennik.ext.setTintColor
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.ui.dialogs.sync.RegisterUnavailableDialog
import pl.szczodrzynski.edziennik.ui.feedback.FeedbackActivity
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import pl.szczodrzynski.edziennik.utils.html.BetterHtml
import pl.szczodrzynski.edziennik.utils.models.Date
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
    private val manager
        get() = app.permissionManager
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
        if (!manager.isNotificationPermissionGranted) {
            manager.requestNotificationsPermission(activity, 0, false){}
        }

        b.versionText.setText(
            R.string.login_chooser_version_format,
            app.buildManager.versionName,
            Date.fromMillis(app.buildManager.buildTimestamp).stringY_m_d
        )
        b.versionText.onClick {
            app.buildManager.showVersionDialog(activity)
            if (!App.devMode)
                return@onClick
            if (adapter.items.firstOrNull { it is LoginInfo.Register && it.loginType == LoginType.TEMPLATE } != null)
                return@onClick
            adapter.items.add(
                index = 0,
                element = LoginInfo.Register(
                    loginType = LoginType.TEMPLATE,
                    registerName = R.string.menu_lab,
                    registerLogo = R.drawable.face_2,
                    loginModes = listOf(
                        LoginInfo.Mode(
                            loginMode = LoginMode.PODLASIE_API,
                            name = 0,
                            icon = 0,
                            guideText = 0,
                            credentials = listOf(),
                            errorCodes = mapOf()
                        )
                    )
                )
            )
            adapter.notifyItemInserted(0)
            b.list.smoothScrollToPosition(0)
        }

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

            adapter.items.removeAll { it !is LoginInfo.Register }
            adapter.items.add(
                    LoginInfo.Register(
                            loginType = LoginType.DEMO,
                            registerName = R.string.eggs,
                            registerLogo = R.drawable.face_1,
                            loginModes = listOf(
                                    LoginInfo.Mode(
                                            loginMode = LoginMode.PODLASIE_API,
                                            name = 0,
                                            icon = 0,
                                            guideText = 0,
                                            credentials = listOf(),
                                            errorCodes = mapOf()
                                    )
                            )
                    )
            )
            adapter.notifyDataSetChanged()
            b.list.smoothScrollToPosition(adapter.items.size - 1)
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
        if (loginType.loginType == LoginType.DEMO) {
            nav.navigate(R.id.loginEggsFragment, null, activity.navOptions)
            return
        }

        if (loginType.loginType == LoginType.TEMPLATE) {
            nav.navigate(R.id.labFragment, null, activity.navOptions)
            return
        }

        if (!app.config.privacyPolicyAccepted) {
            SimpleDialog<Unit>(activity) {
                title(R.string.privacy_policy)
                message(BetterHtml.fromHtml(activity, R.string.privacy_policy_dialog_html))
                positive(R.string.i_agree) {
                    app.config.privacyPolicyAccepted = true
                    onLoginModeClicked(loginType, loginMode)
                }
                negative(R.string.i_disagree)
            }.show()
            return
        }

        launch {
            if (!checkAvailability(loginType.loginType))
                return@launch

            if (loginMode.isTesting || loginMode.isDevOnly) {
                SimpleDialog<Unit>(activity) {
                    title(R.string.login_chooser_testing_title)
                    message(R.string.login_chooser_testing_text)
                    positive(R.string.ok) {
                        navigateToLoginMode(loginType, loginMode)
                    }
                    negative(R.string.cancel)
                }.show()
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

    private suspend fun checkAvailability(loginType: LoginType): Boolean {
        val error = withContext(Dispatchers.IO) {
            app.availabilityManager.check(loginType)
        } ?: return true

        return when (error.type) {
            Type.NOT_AVAILABLE -> {
                RegisterUnavailableDialog(activity, error.status!!).show()
                false
            }
            Type.API_ERROR -> {
                activity.errorSnackbar.addError(error.apiError!!).show()
                false
            }
            Type.NO_API_ACCESS -> {
                Toast.makeText(activity, R.string.error_no_api_access, Toast.LENGTH_SHORT).show()
                true
            }
        }
    }
}
