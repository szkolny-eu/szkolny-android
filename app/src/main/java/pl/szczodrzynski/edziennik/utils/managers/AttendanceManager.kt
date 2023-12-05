/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-28.
 */

package pl.szczodrzynski.edziennik.utils.managers

import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.szkolny.font.SzkolnyFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.AttendanceType
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import kotlin.coroutines.CoroutineContext

class AttendanceManager(val app: App) : CoroutineScope {

    companion object {
        const val SORTED_BY_ALPHABET = 0
        const val SORTED_BY_HIGHEST = 1
        const val SORTED_BY_LOWEST = 2
    }
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default
    val useSymbols
        get() = app.profile.config.attendance.useSymbols
    val showDifference
        get() = app.profile.config.attendance.showDifference
    val orderBy
        get() = app.profile.config.attendance.orderBy

    fun getTypeShort(baseType: Int): String {
        return when (baseType) {
            Attendance.TYPE_PRESENT -> "ob"
            Attendance.TYPE_PRESENT_CUSTOM -> "   "
            Attendance.TYPE_ABSENT -> "nb"
            Attendance.TYPE_ABSENT_EXCUSED -> "u"
            Attendance.TYPE_RELEASED -> "zw"
            Attendance.TYPE_BELATED -> "sp"
            Attendance.TYPE_BELATED_EXCUSED -> "su"
            Attendance.TYPE_DAY_FREE -> "w"
            else -> "?"
        }
    }

    fun getAttendanceColor(baseType: Int): Int {
        return when (baseType) {
            Attendance.TYPE_PRESENT -> 0xff009688.toInt()
            Attendance.TYPE_PRESENT_CUSTOM -> 0xff64b5f6.toInt()
            Attendance.TYPE_ABSENT -> 0xffff3d00.toInt()
            Attendance.TYPE_ABSENT_EXCUSED -> 0xff76ff03.toInt()
            Attendance.TYPE_RELEASED -> 0xff9e9e9e.toInt()
            Attendance.TYPE_BELATED -> 0xffffc107.toInt()
            Attendance.TYPE_BELATED_EXCUSED -> 0xffffc107.toInt()
            Attendance.TYPE_DAY_FREE -> 0xff43a047.toInt()
            else -> 0xff64b5f6.toInt()
        }
    }
    fun getAttendanceColor(typeObject: AttendanceType): Int {
        return (if (useSymbols) typeObject.typeColor else null)
                ?: if (typeObject.baseType == Attendance.TYPE_PRESENT_CUSTOM || !typeObject.isCounted)
                    typeObject.typeColor ?: 0xff64b5f6.toInt()
                else getAttendanceColor(typeObject.baseType)
    }
    fun getAttendanceColor(attendance: Attendance): Int {
        return (if (useSymbols) attendance.typeColor else null)
                ?: if (attendance.baseType == Attendance.TYPE_PRESENT_CUSTOM || !attendance.isCounted)
                    attendance.typeColor ?: 0xff64b5f6.toInt()
                else getAttendanceColor(attendance.baseType)
    }

    fun getAttendanceIcon(attendance: Attendance): IIcon? = when (attendance.baseType) {
        Attendance.TYPE_PRESENT, Attendance.TYPE_PRESENT_CUSTOM -> CommunityMaterial.Icon.cmd_check
        Attendance.TYPE_ABSENT -> CommunityMaterial.Icon.cmd_close
        Attendance.TYPE_ABSENT_EXCUSED -> CommunityMaterial.Icon3.cmd_progress_close
        Attendance.TYPE_RELEASED -> CommunityMaterial.Icon.cmd_account_arrow_right_outline
        Attendance.TYPE_BELATED -> CommunityMaterial.Icon.cmd_clock_alert_outline
        Attendance.TYPE_BELATED_EXCUSED -> CommunityMaterial.Icon.cmd_clock_check_outline
        Attendance.TYPE_DAY_FREE -> SzkolnyFont.Icon.szf_umbrella_beach_outline
        else -> null
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
