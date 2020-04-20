/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pl.szczodrzynski.edziennik.utils.models.Date

class Migration38 : Migration(37, 38) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val today = Date.getToday()
        val schoolYearStart = if (today.month < 9) today.year - 1 else today.year
        database.execSQL("UPDATE profiles SET dateSemester1Start = '$schoolYearStart-09-01' WHERE dateSemester1Start IS NULL")
        database.execSQL("UPDATE profiles SET dateSemester2Start = '${schoolYearStart + 1}-02-01' WHERE dateSemester2Start IS NULL")
        database.execSQL("UPDATE profiles SET dateYearEnd = '${schoolYearStart + 1}-06-30' WHERE dateYearEnd IS NULL")
    }
}
