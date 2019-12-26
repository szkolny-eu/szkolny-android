/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-23
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik

import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.models.Feature

const val ENDPOINT_EDUDZIENNIK_WEB_START                = 1000
const val ENDPOINT_EDUDZIENNIK_WEB_TEACHERS             = 1001
const val ENDPOINT_EDUDZIENNIK_WEB_GRADES               = 1011
const val ENDPOINT_EDUDZIENNIK_WEB_TIMETABLE            = 1012
const val ENDPOINT_EDUDZIENNIK_WEB_EXAMS                = 1013
const val ENDPOINT_EDUDZIENNIK_WEB_ATTENDANCE           = 1014
const val ENDPOINT_EDUDZIENNIK_WEB_ANNOUNCEMENTS        = 1015
const val ENDPOINT_EDUDZIENNIK_WEB_LUCKY_NUMBER         = 1030

val EdudziennikFeatures = listOf(
        /* School and team info and subjects */
        Feature(LOGIN_TYPE_EDUDZIENNIK, FEATURE_STUDENT_INFO, listOf(
                ENDPOINT_EDUDZIENNIK_WEB_START to LOGIN_METHOD_EDUDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_EDUDZIENNIK_WEB)),

        /* Teachers */
        Feature(LOGIN_TYPE_EDUDZIENNIK, FEATURE_TEACHERS, listOf(
                ENDPOINT_EDUDZIENNIK_WEB_TEACHERS to LOGIN_METHOD_EDUDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_EDUDZIENNIK_WEB)),

        /* Timetable */
        Feature(LOGIN_TYPE_EDUDZIENNIK, FEATURE_TIMETABLE, listOf(
                ENDPOINT_EDUDZIENNIK_WEB_TIMETABLE to LOGIN_METHOD_EDUDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_EDUDZIENNIK_WEB)),

        /* Grades */
        Feature(LOGIN_TYPE_EDUDZIENNIK, FEATURE_GRADES, listOf(
                ENDPOINT_EDUDZIENNIK_WEB_GRADES to LOGIN_METHOD_EDUDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_EDUDZIENNIK_WEB)),

        /* Agenda */
        Feature(LOGIN_TYPE_EDUDZIENNIK, FEATURE_AGENDA, listOf(
                ENDPOINT_EDUDZIENNIK_WEB_EXAMS to LOGIN_METHOD_EDUDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_EDUDZIENNIK_WEB)),

        /* Attendance */
        Feature(LOGIN_TYPE_EDUDZIENNIK, FEATURE_ATTENDANCE, listOf(
                ENDPOINT_EDUDZIENNIK_WEB_ATTENDANCE to LOGIN_METHOD_EDUDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_EDUDZIENNIK_WEB)),

        /* Announcements */
        Feature(LOGIN_TYPE_EDUDZIENNIK, FEATURE_ANNOUNCEMENTS, listOf(
                ENDPOINT_EDUDZIENNIK_WEB_ANNOUNCEMENTS to LOGIN_METHOD_EDUDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_EDUDZIENNIK_WEB)),

        /* Lucky number */
        Feature(LOGIN_TYPE_EDUDZIENNIK, FEATURE_LUCKY_NUMBER, listOf(
                ENDPOINT_EDUDZIENNIK_WEB_LUCKY_NUMBER to LOGIN_METHOD_EDUDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_EDUDZIENNIK_WEB))
)
