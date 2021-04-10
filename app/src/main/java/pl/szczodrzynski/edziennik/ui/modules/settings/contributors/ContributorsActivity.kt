package pl.szczodrzynski.edziennik.ui.modules.settings.contributors

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.github.ContributorsData

class ContributorsActivity() : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app: App = this.application as App

        val contributorsData = ContributorsData()
        lifecycleScope.launch {
            val contributorsJSON = contributorsData.getContributors(app)
            Log.e("Response", contributorsJSON.toString())
        }

        setContentView(R.layout.activity_contributors)
    }
}
