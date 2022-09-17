/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.login

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.databinding.LoginActivityBinding
import pl.szczodrzynski.edziennik.ui.error.ErrorSnackbar
import pl.szczodrzynski.edziennik.utils.SwipeRefreshLayoutNoTouch
import kotlin.coroutines.CoroutineContext

class LoginActivity : AppCompatActivity(), CoroutineScope {
    companion object {
        private const val TAG = "LoginActivity"
    }

    private val app: App by lazy { applicationContext as App }
    private lateinit var b: LoginActivityBinding
    lateinit var navOptions: NavOptions
    val nav by lazy { Navigation.findNavController(this, R.id.nav_host_fragment) }
    val errorSnackbar: ErrorSnackbar by lazy { ErrorSnackbar(this) }
    val swipeRefreshLayout: SwipeRefreshLayoutNoTouch by lazy { b.swipeRefreshLayout }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var lastError: ApiError? = null
    val profiles = mutableListOf<LoginSummaryAdapter.Item>()
    val loginStores = mutableListOf<LoginStore>()

    fun getRootView() = b.root

    override fun onBackPressed() {
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
            MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.are_you_sure)
                    .setMessage(R.string.login_cancel_confirmation)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
            return
        }
        nav.navigateUp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme_Light)

        navOptions = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()

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
