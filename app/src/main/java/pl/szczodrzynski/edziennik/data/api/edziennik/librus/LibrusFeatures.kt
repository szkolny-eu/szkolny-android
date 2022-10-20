/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus

import pl.szczodrzynski.edziennik.data.api.models.Feature
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.LoginMethod
import pl.szczodrzynski.edziennik.data.db.enums.LoginType

const val ENDPOINT_LIBRUS_API_ME                                       = 1001
const val ENDPOINT_LIBRUS_API_SCHOOLS                                  = 1002
const val ENDPOINT_LIBRUS_API_CLASSES                                  = 1003
const val ENDPOINT_LIBRUS_API_VIRTUAL_CLASSES                          = 1004
const val ENDPOINT_LIBRUS_API_UNITS                                    = 1005
const val ENDPOINT_LIBRUS_API_USERS                                    = 1006
const val ENDPOINT_LIBRUS_API_SUBJECTS                                 = 1007
const val ENDPOINT_LIBRUS_API_CLASSROOMS                               = 1008
const val ENDPOINT_LIBRUS_API_LESSONS                                  = 1009
const val ENDPOINT_LIBRUS_API_PUSH_CONFIG                              = 1010
const val ENDPOINT_LIBRUS_API_TIMETABLES                               = 1015
const val ENDPOINT_LIBRUS_API_SUBSTITUTIONS                            = 1016
const val ENDPOINT_LIBRUS_API_NORMAL_GRADE_CATEGORIES                  = 1021
const val ENDPOINT_LIBRUS_API_POINT_GRADE_CATEGORIES                   = 1022
const val ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADE_CATEGORIES             = 1023
const val ENDPOINT_LIBRUS_API_TEXT_GRADE_CATEGORIES                    = 1024
const val ENDPOINT_LIBRUS_API_DESCRIPTIVE_TEXT_GRADE_CATEGORIES        = 1025
const val ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADE_CATEGORIES               = 1026
const val ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADE_COMMENTS                 = 1027
const val ENDPOINT_LIBRUS_API_NORMAL_GRADE_COMMENTS                    = 1030
const val ENDPOINT_LIBRUS_API_NORMAL_GRADES                            = 1031
const val ENDPOINT_LIBRUS_API_POINT_GRADES                             = 1032
const val ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADES                       = 1033
const val ENDPOINT_LIBRUS_API_TEXT_GRADES                              = 1034
const val ENDPOINT_LIBRUS_API_DESCRIPTIVE_TEXT_GRADES                  = 1035
const val ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES                         = 1036
const val ENDPOINT_LIBRUS_API_EVENT_TYPES                              = 1040
const val ENDPOINT_LIBRUS_API_EVENTS                                   = 1041
const val ENDPOINT_LIBRUS_API_HOMEWORK                                 = 1050
const val ENDPOINT_LIBRUS_API_LUCKY_NUMBER                             = 1060
const val ENDPOINT_LIBRUS_API_NOTICE_TYPES                             = 1070
const val ENDPOINT_LIBRUS_API_NOTICES                                  = 1071
const val ENDPOINT_LIBRUS_API_ATTENDANCE_TYPES                         = 1080
const val ENDPOINT_LIBRUS_API_ATTENDANCES                              = 1081
const val ENDPOINT_LIBRUS_API_ANNOUNCEMENTS                            = 1090
const val ENDPOINT_LIBRUS_API_PT_MEETINGS                              = 1100
const val ENDPOINT_LIBRUS_API_TEACHER_FREE_DAY_TYPES                   = 1109
const val ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS                        = 1110
const val ENDPOINT_LIBRUS_API_SCHOOL_FREE_DAYS                         = 1120
const val ENDPOINT_LIBRUS_API_CLASS_FREE_DAYS                          = 1130
const val ENDPOINT_LIBRUS_SYNERGIA_INFO                                = 2010
const val ENDPOINT_LIBRUS_SYNERGIA_GRADES                              = 2020
const val ENDPOINT_LIBRUS_SYNERGIA_HOMEWORK                            = 2030
const val ENDPOINT_LIBRUS_SYNERGIA_MESSAGES_RECEIVED                   = 2040
const val ENDPOINT_LIBRUS_SYNERGIA_MESSAGES_SENT                       = 2050
const val ENDPOINT_LIBRUS_MESSAGES_RECEIVED                            = 3010
const val ENDPOINT_LIBRUS_MESSAGES_SENT                                = 3020
const val ENDPOINT_LIBRUS_MESSAGES_TRASH                               = 3030

val LibrusFeatures = listOf(

        Feature(LoginType.LIBRUS, FeatureType.ALWAYS_NEEDED, listOf(
                ENDPOINT_LIBRUS_API_LESSONS to LoginMethod.LIBRUS_API
        )),

        // push config
        Feature(LoginType.LIBRUS, FeatureType.PUSH_CONFIG, listOf(
                ENDPOINT_LIBRUS_API_PUSH_CONFIG to LoginMethod.LIBRUS_API
        )).withShouldSync { data ->
                (data as DataLibrus).isPremium && !data.app.config.sync.tokenLibrusList.contains(data.profileId)
        },





        /**
         * Timetable - using API.
         */
        Feature(LoginType.LIBRUS, FeatureType.TIMETABLE, listOf(
                ENDPOINT_LIBRUS_API_TIMETABLES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_SUBSTITUTIONS to LoginMethod.LIBRUS_API
        )),
        /**
         * Agenda - using API.
         * Events, Parent-teacher meetings, free days (teacher/school/class).
         */
        Feature(LoginType.LIBRUS, FeatureType.AGENDA, listOf(
                ENDPOINT_LIBRUS_API_EVENTS to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_EVENT_TYPES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_PT_MEETINGS to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_TEACHER_FREE_DAY_TYPES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_SCHOOL_FREE_DAYS to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_CLASS_FREE_DAYS to LoginMethod.LIBRUS_API
        )),
        /**
         * Grades - using API.
         * All grades + categories.
         */
        Feature(LoginType.LIBRUS, FeatureType.GRADES, listOf(
                ENDPOINT_LIBRUS_API_NORMAL_GRADE_CATEGORIES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_POINT_GRADE_CATEGORIES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADE_CATEGORIES to LoginMethod.LIBRUS_API,
                // Commented out, because TextGrades/Categories is the same as Grades/Categories
                /* ENDPOINT_LIBRUS_API_TEXT_GRADE_CATEGORIES to LoginMethod.LIBRUS_API, */
                ENDPOINT_LIBRUS_API_DESCRIPTIVE_TEXT_GRADE_CATEGORIES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADE_CATEGORIES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_NORMAL_GRADE_COMMENTS to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADE_COMMENTS to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_NORMAL_GRADES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_POINT_GRADES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_TEXT_GRADES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_DESCRIPTIVE_TEXT_GRADES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES to LoginMethod.LIBRUS_API
        )),
        /**
         * Homework - using API.
         * Sync only if account has premium access.
         */
        /*Feature(LoginType.LIBRUS, FeatureType.HOMEWORK, listOf(
                ENDPOINT_LIBRUS_API_HOMEWORK to LoginMethod.LIBRUS_API
        )).withShouldSync { data ->
                (data as DataLibrus).isPremium
        },*/
        /**
         * Behaviour - using API.
         */
        Feature(LoginType.LIBRUS, FeatureType.BEHAVIOUR, listOf(
                ENDPOINT_LIBRUS_API_NOTICES to LoginMethod.LIBRUS_API
        )),
        /**
         * Attendance - using API.
         */
        Feature(LoginType.LIBRUS, FeatureType.ATTENDANCE, listOf(
                ENDPOINT_LIBRUS_API_ATTENDANCE_TYPES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_ATTENDANCES to LoginMethod.LIBRUS_API
        )),
        /**
         * Announcements - using API.
         */
        Feature(LoginType.LIBRUS, FeatureType.ANNOUNCEMENTS, listOf(
                ENDPOINT_LIBRUS_API_ANNOUNCEMENTS to LoginMethod.LIBRUS_API
        )),





        /**
         * Student info - using API.
         */
        Feature(LoginType.LIBRUS, FeatureType.STUDENT_INFO, listOf(
                ENDPOINT_LIBRUS_API_ME to LoginMethod.LIBRUS_API
        )),
        /**
         * School info - using API.
         */
        Feature(LoginType.LIBRUS, FeatureType.SCHOOL_INFO, listOf(
                ENDPOINT_LIBRUS_API_SCHOOLS to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_UNITS to LoginMethod.LIBRUS_API
        )),
        /**
         * Class info - using API.
         */
        Feature(LoginType.LIBRUS, FeatureType.CLASS_INFO, listOf(
                ENDPOINT_LIBRUS_API_CLASSES to LoginMethod.LIBRUS_API
        )),
        /**
         * Team info - using API.
         */
        Feature(LoginType.LIBRUS, FeatureType.TEAM_INFO, listOf(
                ENDPOINT_LIBRUS_API_VIRTUAL_CLASSES to LoginMethod.LIBRUS_API
        )),
        /**
         * Lucky number - using API.
         */
        Feature(LoginType.LIBRUS, FeatureType.LUCKY_NUMBER, listOf(
                ENDPOINT_LIBRUS_API_LUCKY_NUMBER to LoginMethod.LIBRUS_API
        )).withShouldSync { data -> data.shouldSyncLuckyNumber() },
        /**
         * Teacher list - using API.
         */
        Feature(LoginType.LIBRUS, FeatureType.TEACHERS, listOf(
                ENDPOINT_LIBRUS_API_USERS to LoginMethod.LIBRUS_API
        )),
        /**
         * Subject list - using API.
         */
        Feature(LoginType.LIBRUS, FeatureType.SUBJECTS, listOf(
                ENDPOINT_LIBRUS_API_SUBJECTS to LoginMethod.LIBRUS_API
        )),
        /**
         * Classroom list - using API.
         */
        Feature(LoginType.LIBRUS, FeatureType.CLASSROOMS, listOf(
                ENDPOINT_LIBRUS_API_CLASSROOMS to LoginMethod.LIBRUS_API
        )),

        /**
         * Student info - using synergia scrapper.
         */
        Feature(LoginType.LIBRUS, FeatureType.STUDENT_INFO, listOf(
                ENDPOINT_LIBRUS_SYNERGIA_INFO to LoginMethod.LIBRUS_SYNERGIA
        )),
        /**
         * Student number - using synergia scrapper.
         */
        Feature(LoginType.LIBRUS, FeatureType.STUDENT_NUMBER, listOf(
                ENDPOINT_LIBRUS_SYNERGIA_INFO to LoginMethod.LIBRUS_SYNERGIA
        )),


        /**
         * Grades - using API + synergia scrapper.
         */
        /*Feature(LoginType.LIBRUS, FeatureType.GRADES, listOf(
                ENDPOINT_LIBRUS_API_NORMAL_GC to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_API_NORMAL_GRADES to LoginMethod.LIBRUS_API,
                ENDPOINT_LIBRUS_SYNERGIA_GRADES to LoginMethod.LIBRUS_SYNERGIA
        )),*/
        /*Endpoint(LoginType.LIBRUS, FeatureType.GRADES, listOf(
                ENDPOINT_LIBRUS_SYNERGIA_GRADES to LoginMethod.LIBRUS_SYNERGIA
        )),*/

        /**
         * Homework - using scrapper.
         * Sync only if account has not premium access.
         */
        Feature(LoginType.LIBRUS, FeatureType.HOMEWORK, listOf(
                ENDPOINT_LIBRUS_SYNERGIA_HOMEWORK to LoginMethod.LIBRUS_SYNERGIA
        ))/*.withShouldSync { data ->
                !(data as DataLibrus).isPremium
        }*/,

        /**
         * Messages inbox - using messages website.
         */
        Feature(LoginType.LIBRUS, FeatureType.MESSAGES_INBOX, listOf(
                ENDPOINT_LIBRUS_MESSAGES_RECEIVED to LoginMethod.LIBRUS_MESSAGES
        )),
        /**
         * Messages sent - using messages website.
         */
        Feature(LoginType.LIBRUS, FeatureType.MESSAGES_SENT, listOf(
                ENDPOINT_LIBRUS_MESSAGES_SENT to LoginMethod.LIBRUS_MESSAGES
        ))
)
