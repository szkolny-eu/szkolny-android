package pl.szczodrzynski.edziennik.ui.modules.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.FragmentLoginChooserBinding
import pl.szczodrzynski.edziennik.onClick
import pl.szczodrzynski.edziennik.ui.modules.feedback.FeedbackActivity

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

        b.fakeLogin.visibility = if (App.devMode) View.VISIBLE else View.GONE
        b.fakeLogin.isChecked = fakeLogin
        b.fakeLogin.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            fakeLogin = isChecked
        }

        b.helpButton.onClick { startActivity(Intent(activity, FeedbackActivity::class.java)) }
    }
}