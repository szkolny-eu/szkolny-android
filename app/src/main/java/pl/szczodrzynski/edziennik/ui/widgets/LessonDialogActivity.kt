/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-14.
 */

package pl.szczodrzynski.edziennik.ui.widgets

import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.ext.Intent
import pl.szczodrzynski.edziennik.ext.app
import pl.szczodrzynski.edziennik.ui.timetable.LessonDetailsDialog
import kotlin.coroutines.CoroutineContext

class LessonDialogActivity : AppCompatActivity(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(0))

        app.uiManager.applyTheme(this, noDisplay = true)

        val app = application as App
        launch {
            val lesson = withContext(Dispatchers.IO) {
                val extras = intent?.extras

                val profileId = extras?.getInt("profileId") ?: return@withContext null

                if (extras.getBoolean("separatorItem", false)) {
                    val i = Intent(
                        app, MainActivity::class.java,
                        "fragmentId" to NavTarget.TIMETABLE,
                        "profileId" to profileId,
                        "timetableDate" to extras.getString("timetableDate", null),
                    ).addFlags(FLAG_ACTIVITY_REORDER_TO_FRONT or FLAG_ACTIVITY_NEW_TASK)
                    app.startActivity(i)
                    finish()
                    return@withContext null
                }

                val lessonId = extras.getLong("lessonId")

                app.db.timetableDao().getByIdNow(profileId, lessonId)
            } ?: return@launch

            LessonDetailsDialog(this@LessonDialogActivity, lesson).showModal()
            finish()
        }
    }
}
