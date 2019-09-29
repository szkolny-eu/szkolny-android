/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-29.
 */

package pl.szczodrzynski.edziennik.api.v2

import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_AGENDA
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_ANNOUNCEMENTS
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_ATTENDANCES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_GRADES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_HOME
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_HOMEWORKS
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_MESSAGES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_NOTICES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_TIMETABLE
import pl.szczodrzynski.edziennik.datamodels.Message.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.datamodels.Message.TYPE_SENT
import pl.szczodrzynski.edziennik.messages.MessagesFragment
import pl.szczodrzynski.edziennik.messages.MessagesListFragment

const val FEATURE_ALL = 0
const val FEATURE_TIMETABLE = 1
const val FEATURE_AGENDA = 2
const val FEATURE_GRADES = 3
const val FEATURE_HOMEWORK = 4
const val FEATURE_NOTICES = 5
const val FEATURE_ATTENDANCES = 6
const val FEATURE_MESSAGES_INBOX = 7
const val FEATURE_MESSAGES_OUTBOX = 8
const val FEATURE_ANNOUNCEMENTS = 9

const val FEATURE_STUDENT_INFO = 101
const val FEATURE_STUDENT_NUMBER = 109
const val FEATURE_SCHOOL_INFO = 102
const val FEATURE_CLASS_INFO = 103
const val FEATURE_TEAM_INFO = 104
const val FEATURE_LUCKY_NUMBER = 105
const val FEATURE_TEACHERS = 106
const val FEATURE_SUBJECTS = 107
const val FEATURE_CLASSROOMS = 108

const val FEATURE_MESSAGE_GET = 201

object Features {
    private fun getAllNecessary(): List<Int> = listOf(
            FEATURE_STUDENT_INFO,
            FEATURE_STUDENT_NUMBER,
            FEATURE_SCHOOL_INFO,
            FEATURE_CLASS_INFO,
            FEATURE_TEAM_INFO,
            FEATURE_LUCKY_NUMBER,
            FEATURE_TEACHERS,
            FEATURE_SUBJECTS,
            FEATURE_CLASSROOMS)

    private fun getAllFeatures(): List<Int> = listOf(
            FEATURE_TIMETABLE,
            FEATURE_AGENDA,
            FEATURE_GRADES,
            FEATURE_HOMEWORK,
            FEATURE_NOTICES,
            FEATURE_ATTENDANCES,
            FEATURE_MESSAGES_INBOX,
            FEATURE_MESSAGES_OUTBOX,
            FEATURE_ANNOUNCEMENTS)

    fun getAllIds(): List<Int> = getAllFeatures() + getAllNecessary()

    fun getIdsByView(targetId: Int): List<Int> {
        return when (targetId) {
            DRAWER_ITEM_HOME -> getAllFeatures()
            DRAWER_ITEM_TIMETABLE -> listOf(FEATURE_TIMETABLE)
            DRAWER_ITEM_AGENDA -> listOf(FEATURE_AGENDA)
            DRAWER_ITEM_GRADES -> listOf(FEATURE_GRADES)
            DRAWER_ITEM_MESSAGES -> when (MessagesFragment.pageSelection) {
                TYPE_RECEIVED -> listOf(FEATURE_MESSAGES_INBOX)
                TYPE_SENT -> listOf(FEATURE_MESSAGES_OUTBOX)
                else -> listOf(FEATURE_MESSAGES_INBOX, FEATURE_MESSAGES_OUTBOX)
            }
            DRAWER_ITEM_HOMEWORKS -> listOf(FEATURE_HOMEWORK)
            DRAWER_ITEM_NOTICES -> listOf(FEATURE_NOTICES)
            DRAWER_ITEM_ATTENDANCES -> listOf(FEATURE_ATTENDANCES)
            DRAWER_ITEM_ANNOUNCEMENTS -> listOf(FEATURE_ANNOUNCEMENTS)
            else -> getAllFeatures()
        } + getAllNecessary()
    }
}