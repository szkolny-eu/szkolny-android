/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-29. 
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.config.utils.get
import pl.szczodrzynski.edziennik.config.utils.set

class ProfileConfigAttendance(private val config: ProfileConfig) {
    private var mAttendancePageSelection: Int? = null
    var attendancePageSelection: Int
        get() { mAttendancePageSelection = mAttendancePageSelection ?: config.values.get("attendancePageSelection", 1); return mAttendancePageSelection ?: 1 }
        set(value) { config.set("attendancePageSelection", value); mAttendancePageSelection = value }

    private var mUseSymbols: Boolean? = null
    var useSymbols: Boolean
        get() { mUseSymbols = mUseSymbols ?: config.values.get("useSymbols", false); return mUseSymbols ?: false }
        set(value) { config.set("useSymbols", value); mUseSymbols = value }

    private var mGroupConsecutiveDays: Boolean? = null
    var groupConsecutiveDays: Boolean
        get() { mGroupConsecutiveDays = mGroupConsecutiveDays ?: config.values.get("groupConsecutiveDays", true); return mGroupConsecutiveDays ?: true }
        set(value) { config.set("groupConsecutiveDays", value); mGroupConsecutiveDays = value }

    private var mShowPresenceInMonth: Boolean? = null
    var showPresenceInMonth: Boolean
        get() { mShowPresenceInMonth = mShowPresenceInMonth ?: config.values.get("showPresenceInMonth", false); return mShowPresenceInMonth ?: false }
        set(value) { config.set("showPresenceInMonth", value); mShowPresenceInMonth = value }
}
