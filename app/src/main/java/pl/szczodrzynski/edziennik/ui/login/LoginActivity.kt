/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.login

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.databinding.LoginActivityBinding
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.ui.main.ErrorSnackbar
import kotlin.coroutines.CoroutineContext

class LoginActivity : AppCompatActivity(), CoroutineScope {
    companion object {
        private const val TAG = "LoginActivity"
    }

    private val app: App by lazy { applicationContext as App }
    private lateinit var b: LoginActivityBinding
    lateinit var navOptions: NavOptions
    lateinit var navOptionsBuilder: NavOptions.Builder
    val nav by lazy { Navigation.findNavController(this, R.id.nav_host_fragment) }
    val errorSnackbar: ErrorSnackbar by lazy { ErrorSnackbar(this) }
    val swipeRefreshLayout: SwipeRefreshLayout by lazy { b.swipeRefreshLayout }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var lastError: ApiError? = null
    val profiles = mutableListOf<LoginSummaryAdapter.Item>()
    val loginStores = mutableListOf<LoginStore>()

    fun getRootView() = b.root

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // TODO fix deprecation
        val destination = nav.currentDestination ?: run {
            nav.navigateUp()
            return
        }
        if (destination.id == R.id.loginSyncErrorFragment)
            return
        if (destination.id == R.id.loginProgressFragment)
            return
        if (destination.id == R.id.loginSyncFragment)
            return
        if (destination.id == R.id.loginFinishFragment)
            return
        // eggs
        if (destination.id == R.id.loginPrizeFragment) {
            finish()
            return
        }
        if (destination.id == R.id.loginChooserFragment && loginStores.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        if (destination.id == R.id.loginSummaryFragment) {
            SimpleDialog<Unit>(this) {
                title(R.string.are_you_sure)
                message(R.string.login_cancel_confirmation)
                positive(R.string.yes) {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
                negative(R.string.no)
            }.show()
            return
        }
        nav.navigateUp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme_M3)

        navOptionsBuilder = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
        navOptions = navOptionsBuilder.build()

        b = LoginActivityBinding.inflate(layoutInflater)
        setContentView(b.root)
        errorSnackbar.setCoordinator(b.coordinator, b.snackbarAnchor)

        app.buildManager.validateBuild(this)

        launch {
            app.config.loginFinished = app.db.profileDao().count > 0
            if (!app.config.loginFinished) {
                app.config.ui.miniMenuVisible = resources.configuration.smallestScreenWidthDp > 480
            }
        }
    }

    fun error(error: ApiError) { errorSnackbar.addError(error).show(); lastError = error }
}
