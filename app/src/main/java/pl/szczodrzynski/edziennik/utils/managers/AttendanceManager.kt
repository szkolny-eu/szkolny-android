/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-28.
 */

package pl.szczodrzynski.edziennik.utils.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.startCoroutineTimer
import kotlin.coroutines.CoroutineContext

class AttendanceManager(val app: App) : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    fun getTypeShort(baseType: Int): String {
        return when (baseType) {
            Attendance.TYPE_PRESENT -> "ob"
            Attendance.TYPE_PRESENT_CUSTOM -> "ob?"
            Attendance.TYPE_ABSENT -> "nb"
            Attendance.TYPE_ABSENT_EXCUSED -> "u"
            Attendance.TYPE_RELEASED -> "zw"
            Attendance.TYPE_BELATED -> "sp"
            Attendance.TYPE_BELATED_EXCUSED -> "su"
            Attendance.TYPE_DAY_FREE -> "w"
            else -> "?"
        }
    }

    /*    _    _ _____    _____                 _  __ _
         | |  | |_   _|  / ____|               (_)/ _(_)
         | |  | | | |   | (___  _ __   ___  ___ _| |_ _  ___
         | |  | | | |    \___ \| '_ \ / _ \/ __| |  _| |/ __|
         | |__| |_| |_   ____) | |_) |  __/ (__| | | | | (__
          \____/|_____| |_____/| .__/ \___|\___|_|_| |_|\___|
                               | |
                               |*/
    fun markAsSeen(attendance: AttendanceFull) {
        attendance.seen = true
        startCoroutineTimer(500L, 0L) {
            app.db.metadataDao().setSeen(attendance.profileId, attendance, true)
        }
    }
}
