/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.web.apidata

import pl.szczodrzynski.edziennik.api.v2.mobidziennik.DataMobidziennik

class MobidziennikApiStudent(val data: DataMobidziennik, rows: List<String>) {
    init { run {
        if (rows.size < 2) {
            return@run
        }

        val student1 = rows[0].split("|")
        val student2 = rows[1].split("|")

        // FROM OLD Mobidziennik API - this information seems to be unused
        /*students.clear();
        String[] student = table.split("\n");
        for (int i = 0; i < student.length; i++) {
            if (student[i].isEmpty()) {
                continue;
            }
            String[] student1 = student[i].split("\\|", Integer.MAX_VALUE);
            String[] student2 = student[++i].split("\\|", Integer.MAX_VALUE);
            students.put(strToInt(student1[0]), new Pair<>(student1, student2));
        }
        Pair<String[], String[]> studentData = students.get(studentId);
        try {
            profile.setAttendancePercentage(Float.parseFloat(studentData.second[1]));
        }
        catch (Exception e) {
            e.printStackTrace();
        }*/
    }}
}