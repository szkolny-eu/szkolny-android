package pl.szczodrzynski.edziennik.ui.modules.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.FragmentLoginChooserBinding
import pl.szczodrzynski.edziennik.onChange
import pl.szczodrzynski.edziennik.onClick
import pl.szczodrzynski.edziennik.ui.modules.feedback.FeedbackActivity
import pl.szczodrzynski.edziennik.utils.Anim
import kotlin.system.exitProcess


class LoginChooserFragment : Fragment() {
    companion object {
        private const val TAG = "LoginChooserFragment"
        var fakeLogin = false
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: FragmentLoginChooserBinding
    private val nav by lazy { activity.nav }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = FragmentLoginChooserBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.topLogo.onClick {
            if (LoginActivity.thisOneIsTricky <= -1) {
                LoginActivity.thisOneIsTricky = 999
            }
            if (LoginActivity.thisOneIsTricky in 0..7) {
                LoginActivity.thisOneIsTricky++
                if (LoginActivity.thisOneIsTricky == 7) {
                    b.topLogo.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.shake));
                    if (b.devMode.visibility != View.VISIBLE)
                        Anim.expand(b.devMode, 500, null);
                    LoginActivity.thisOneIsTricky = 3
                }
            }
        }
        b.loginMobidziennikLogo.onClick { nav.navigate(R.id.loginMobidziennikFragment, null, LoginActivity.navOptions) }
        b.loginLibrusLogo.onClick { nav.navigate(R.id.loginLibrusFragment, null, LoginActivity.navOptions) }
        b.loginVulcanLogo.onClick { nav.navigate(R.id.loginVulcanFragment, null, LoginActivity.navOptions) }
        b.loginIuczniowieLogo.onClick { nav.navigate(R.id.loginIuczniowieFragment, null, LoginActivity.navOptions) }
        b.loginLibrusJstLogo.onClick { nav.navigate(R.id.loginLibrusJstFragment, null, LoginActivity.navOptions) }
        b.loginEdudziennikLogo.onClick { nav.navigate(R.id.loginEdudziennikFragment, null, LoginActivity.navOptions) }

        when {
            activity.loginStores.isNotEmpty() -> {
                // we are navigated here from LoginSummary
                b.cancelButton.visibility = View.VISIBLE
                b.cancelButton.onClick { nav.navigateUp() }
            }
            app.config.loginFinished -> {
                // we are navigated here from AppDrawer
                b.cancelButton.visibility = View.VISIBLE
                b.cancelButton.onClick {
                    activity.setResult(Activity.RESULT_CANCELED)
                    activity.finish()
                }
            }
            else -> {
                // there are no profiles
                b.cancelButton.visibility = View.GONE
            }
        }

        b.devMode.visibility = if (App.debugMode) View.VISIBLE else View.GONE
        b.devMode.isChecked = app.config.debugMode
        b.devMode.onChange { v, isChecked ->
            if (isChecked) {
                MaterialDialog.Builder(activity)
                        .title(R.string.are_you_sure)
                        .content(R.string.dev_mode_enable_warning)
                        .positiveText(R.string.yes)
                        .negativeText(R.string.no)
                        .onPositive { _: MaterialDialog?, _: DialogAction? ->
                            app.config.debugMode = true
                            App.devMode = true
                            MaterialAlertDialogBuilder(activity)
                                    .setTitle("Restart")
                                    .setMessage("Wymagany restart aplikacji")
                                    .setPositiveButton("OK") { _, _ ->
                                        Process.killProcess(Process.myPid())
                                        Runtime.getRuntime().exit(0)
                                        exitProcess(0)
                                    }
                                    .setCancelable(false)
                                    .show()
                        }
                        .onNegative { _: MaterialDialog?, _: DialogAction? ->
                            app.config.debugMode = false
                            App.devMode = false
                            b.devMode.isChecked = app.config.debugMode
                            b.devMode.jumpDrawablesToCurrentState()
                            Anim.collapse(b.devMode, 1000, null)
                        }
                        .show()
            } else {
                app.config.debugMode = false
                App.devMode = false
                /*if (b.devModeLayout.getVisibility() === View.VISIBLE) {
                    Anim.collapse(b.devModeTitle, 500, null)
                    Anim.collapse(b.devModeLayout, 500, null)
                }*/
            }
        }

        b.fakeLogin.visibility = if (App.devMode) View.VISIBLE else View.GONE
        b.fakeLogin.isChecked = fakeLogin
        b.fakeLogin.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            fakeLogin = isChecked
        }

        b.helpButton.onClick { startActivity(Intent(activity, FeedbackActivity::class.java)) }
    }
}
