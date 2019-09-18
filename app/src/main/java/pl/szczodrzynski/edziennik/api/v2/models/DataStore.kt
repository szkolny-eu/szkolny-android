package pl.szczodrzynski.edziennik.api.v2.models

import android.util.LongSparseArray
import androidx.core.util.forEach
import androidx.core.util.isNotEmpty
import pl.szczodrzynski.edziennik.datamodels.*
import pl.szczodrzynski.edziennik.models.Date

data class DataStore(private val appDb: AppDb, val profileId: Int) {
    val teacherList: LongSparseArray<Teacher> = LongSparseArray()
    val subjectList: LongSparseArray<Subject> = LongSparseArray()
    val teamList = mutableListOf<Team>()
    val lessonList = mutableListOf<Lesson>()
    val lessonChangeList = mutableListOf<LessonChange>()
    val gradeCategoryList = mutableListOf<GradeCategory>()
    val gradeList = mutableListOf<Grade>()
    val eventList = mutableListOf<Event>()
    val eventTypeList = mutableListOf<EventType>()
    val noticeList = mutableListOf<Notice>()
    val attendanceList = mutableListOf<Attendance>()
    val announcementList = mutableListOf<Announcement>()
    val messageList = mutableListOf<Message>()
    val messageRecipientList = mutableListOf<MessageRecipient>()
    val messageRecipientIgnoreList = mutableListOf<MessageRecipient>()
    val metadataList = mutableListOf<Metadata>()
    val messageMetadataList = mutableListOf<Metadata>()

    init {

        clear()
        
        appDb.teacherDao().getAllNow(profileId).forEach { teacher ->
            teacherList.put(teacher.id, teacher)
        }
        appDb.subjectDao().getAllNow(profileId).forEach { subject ->
            subjectList.put(subject.id, subject)
        }

        /*val teacher = teachers.byNameFirstLast("Jan Kowalski") ?: Teacher(1, 1, "", "").let {
            teachers.add(it)
        }*/

    }

    fun clear() {
        teacherList.clear()
        subjectList.clear()
        teamList.clear()
        lessonList.clear()
        lessonChangeList.clear()
        gradeCategoryList.clear()
        gradeList.clear()
        eventTypeList.clear()
        noticeList.clear()
        attendanceList.clear()
        announcementList.clear()
        messageList.clear()
        messageRecipientList.clear()
        messageRecipientIgnoreList.clear()
        metadataList.clear()
        messageMetadataList.clear()
    }

    fun saveData() {
        if (teacherList.isNotEmpty()) {
            val tempList: ArrayList<Teacher> = ArrayList()
            teacherList.forEach { _, teacher ->
                tempList.add(teacher)
            }
            appDb.teacherDao().addAll(tempList)
        }
        if (subjectList.isNotEmpty()) {
            val tempList: ArrayList<Subject> = ArrayList()
            subjectList.forEach { _, subject ->
                tempList.add(subject)
            }
            appDb.subjectDao().addAll(tempList)
        }
        if (teamList.isNotEmpty())
            appDb.teamDao().addAll(teamList)
        if (lessonList.isNotEmpty()) {
            appDb.lessonDao().clear(profileId)
            appDb.lessonDao().addAll(lessonList)
        }
        if (lessonChangeList.isNotEmpty())
            appDb.lessonChangeDao().addAll(lessonChangeList)
        if (gradeCategoryList.isNotEmpty())
            appDb.gradeCategoryDao().addAll(gradeCategoryList)
        if (gradeList.isNotEmpty()) {
            appDb.gradeDao().clear(profileId)
            appDb.gradeDao().addAll(gradeList)
        }
        if (eventList.isNotEmpty()) {
            appDb.eventDao().removeFuture(profileId, Date.getToday())
            appDb.eventDao().addAll(eventList)
        }
        if (eventTypeList.isNotEmpty())
            appDb.eventTypeDao().addAll(eventTypeList)
        if (noticeList.isNotEmpty()) {
            appDb.noticeDao().clear(profileId)
            appDb.noticeDao().addAll(noticeList)
        }
        if (attendanceList.isNotEmpty())
            appDb.attendanceDao().addAll(attendanceList)
        if (announcementList.isNotEmpty())
            appDb.announcementDao().addAll(announcementList)
        if (messageList.isNotEmpty())
            appDb.messageDao().addAllIgnore(messageList)
        if (messageRecipientList.isNotEmpty())
            appDb.messageRecipientDao().addAll(messageRecipientList)
        if (messageRecipientIgnoreList.isNotEmpty())
            appDb.messageRecipientDao().addAllIgnore(messageRecipientIgnoreList)
        if (metadataList.isNotEmpty())
            appDb.metadataDao().addAllIgnore(metadataList)
        if (messageMetadataList.isNotEmpty())
            appDb.metadataDao().setSeen(messageMetadataList)
    }
}