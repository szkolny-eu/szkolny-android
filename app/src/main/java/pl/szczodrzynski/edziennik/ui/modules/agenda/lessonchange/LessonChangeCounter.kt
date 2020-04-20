package pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchange

import pl.szczodrzynski.edziennik.utils.models.Date
import java.util.*

class LessonChangeCounter(
        val lessonChangeDate: Date,
        var lessonChangeCount: Int
) {
    val startTime: Calendar
        get() = Calendar.getInstance().apply {
                set(lessonChangeDate.year, lessonChangeDate.month - 1, lessonChangeDate.day, 10, 0, 0)
            }

    val endTime: Calendar
        get() = Calendar.getInstance().apply {
            timeInMillis = startTime.timeInMillis + (45 * 60 * 1000)
        }
}
