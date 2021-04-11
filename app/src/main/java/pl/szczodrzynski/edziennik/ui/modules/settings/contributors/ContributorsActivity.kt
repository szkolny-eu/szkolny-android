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
import pl.szczodrzynski.edziennik.data.api.edziennik.github.Contributor

class ContributorsActivity() : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app: App = this.application as App

        lifecycleScope.launch {
            val contributors: List<Contributor> = ContributorsData.getContributors(app)
            for(contributor in contributors) {
                Log.e("Response", contributor.login)
            }
        }

        setContentView(R.layout.activity_contributors)
    }
}
