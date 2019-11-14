/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-14.
 */

package pl.szczodrzynski.edziennik.widgets.timetable

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.ui.dialogs.timetable.LessonDetailsDialog
import pl.szczodrzynski.edziennik.utils.Themes
import kotlin.coroutines.CoroutineContext

class LessonDialogActivity : AppCompatActivity(), CoroutineScope {
    companion object {
        private const val TAG = "LessonDialogActivity"
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val shownDialogs = hashSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(0))

        job = Job()

        setTheme(Themes.appThemeNoDisplay)

        val app = application as App
        launch {
            val deferred = async(Dispatchers.Default) {
                val extras = intent?.extras

                val profileId = extras?.getInt("profileId") ?: return@async null

                if (extras.getBoolean("separatorItem", false)) {
                    val i = Intent(app, MainActivity::class.java)
                            .putExtra("fragmentId", MainActivity.DRAWER_ITEM_TIMETABLE)
                            .putExtra("profileId", profileId)
                            .addFlags(FLAG_ACTIVITY_REORDER_TO_FRONT)
                    app.startActivity(i)
                    finish()
                    return@async null
                }

                val lessonId = extras.getLong("lessonId")

                app.db.timetableDao().getByIdNow(profileId, lessonId)
            }
            val lesson = deferred.await()
            lesson?.let {
                LessonDetailsDialog(
                        this@LessonDialogActivity,
                        lesson,
                        onShowListener = { tag ->
                            shownDialogs.add(tag)
                        },
                        onDismissListener = { tag ->
                            shownDialogs.remove(tag)
                            if (shownDialogs.isEmpty())
                                finish()
                        }
                )
            }
        }
    }
}