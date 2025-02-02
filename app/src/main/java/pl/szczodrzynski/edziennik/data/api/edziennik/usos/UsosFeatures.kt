/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos

import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.models.Feature
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.LoginMethod
import pl.szczodrzynski.edziennik.data.enums.LoginType

const val ENDPOINT_USOS_API_USER            = 7000
const val ENDPOINT_USOS_API_TERMS           = 7010
const val ENDPOINT_USOS_API_COURSES         = 7020
const val ENDPOINT_USOS_API_TIMETABLE       = 7030
const val ENDPOINT_USOS_API_ECTS_POINTS     = 7040
const val ENDPOINT_USOS_API_EXAM_REPORTS    = 7050

val UsosFeatures = listOf(
    /*
    * Student information
    */
    Feature(LoginType.USOS, FeatureType.STUDENT_INFO, listOf(
        ENDPOINT_USOS_API_USER to LoginMethod.USOS_API,
    )),

    /*
    * Terms & courses
    */
    Feature(LoginType.USOS, FeatureType.SCHOOL_INFO, listOf(
        ENDPOINT_USOS_API_TERMS to LoginMethod.USOS_API,
    )),
    Feature(LoginType.USOS, FeatureType.TEAM_INFO, listOf(
        ENDPOINT_USOS_API_COURSES to LoginMethod.USOS_API,
    )),

    /*
     * Timetable
     */
    Feature(LoginType.USOS, FeatureType.TIMETABLE, listOf(
        ENDPOINT_USOS_API_TIMETABLE to LoginMethod.USOS_API,
    )),

    /*
     * Grades
     */
    Feature(LoginType.USOS, FeatureType.GRADES, listOf(
        ENDPOINT_USOS_API_ECTS_POINTS to LoginMethod.USOS_API,
        ENDPOINT_USOS_API_EXAM_REPORTS to LoginMethod.USOS_API,
    )),
)
