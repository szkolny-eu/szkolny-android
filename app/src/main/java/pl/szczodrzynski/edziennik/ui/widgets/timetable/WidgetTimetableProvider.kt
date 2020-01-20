/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-6.
 */

package pl.szczodrzynski.edziennik.ui.widgets.timetable

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.SparseArray
import android.view.View
import android.widget.RemoteViews
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.db.entity.Event.TYPE_HOMEWORK
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.entity.Lesson.Companion.TYPE_NO_LESSONS
import pl.szczodrzynski.edziennik.ui.widgets.LessonDialogActivity
import pl.szczodrzynski.edziennik.ui.widgets.WidgetConfig
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.ItemWidgetTimetableModel
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week
import java.lang.reflect.InvocationTargetException


class WidgetTimetableProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_SYNC_DATA = "ACTION_SYNC_DATA"
        private const val TAG = "WidgetTimetable"

        var timetables: SparseArray<List<ItemWidgetTimetableModel>>? = null

        fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, WidgetTimetableProvider::class.java)
            intent.action = action
            return getPendingSelfIntent(context, intent)
        }

        fun getPendingSelfIntent(context: Context, intent: Intent): PendingIntent {
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        fun drawableToBitmap(drawable: Drawable): Bitmap {

            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            }

            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_SYNC_DATA == intent.action) {
            EdziennikTask.sync().enqueue(context)
        }
        super.onReceive(context, intent)
    }

    private val ignoreCancelled = false

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val thisWidget = ComponentName(context, WidgetTimetableProvider::class.java)

        timetables = SparseArray()

        val app = context.applicationContext as App
        val widgetConfigs = app.config.widgetConfigs

        var bellSyncDiffMillis: Long = 0
        app.config.timetable.bellSyncDiff?.let {
            bellSyncDiffMillis = (it.hour * 60 * 60 * 1000 + it.minute * 60 * 1000 + it.second * 1000).toLong()
            bellSyncDiffMillis *= app.config.timetable.bellSyncMultiplier.toLong()
            bellSyncDiffMillis *= -1
        }

        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        allWidgetIds?.forEach { appWidgetId ->
            val config = widgetConfigs.getJsonObject(appWidgetId.toString())?.let { app.gson.fromJson(it, WidgetConfig::class.java) } ?: return@forEach

            val views = if (config.bigStyle) {
                RemoteViews(context.packageName, if (config.darkTheme) R.layout.widget_timetable_dark_big else R.layout.widget_timetable_big)
            } else {
                RemoteViews(context.packageName, if (config.darkTheme) R.layout.widget_timetable_dark else R.layout.widget_timetable)
            }

            val refreshIntent = Intent(app, WidgetTimetableProvider::class.java)
            refreshIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            val refreshPendingIntent = PendingIntent.getBroadcast(context,
                    0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.widgetTimetableRefresh, refreshPendingIntent)

            views.setOnClickPendingIntent(R.id.widgetTimetableSync, getPendingSelfIntent(context, ACTION_SYNC_DATA))

            views.setImageViewBitmap(R.id.widgetTimetableRefresh, IconicsDrawable(context, CommunityMaterial.Icon2.cmd_refresh)
                    .colorInt(Color.WHITE)
                    .sizeDp(if (config.bigStyle) 24 else 16).toBitmap())

            views.setImageViewBitmap(R.id.widgetTimetableSync, IconicsDrawable(context, CommunityMaterial.Icon.cmd_download_outline)
                    .colorInt(Color.WHITE)
                    .sizeDp(if (config.bigStyle) 24 else 16).toBitmap())

            prepareAppWidget(app, appWidgetId, views, config, bellSyncDiffMillis)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetTimetableListView)
        }
    }

    private fun prepareAppWidget(
            app: App,
            appWidgetId: Int,
            views: RemoteViews,
            widgetConfig: WidgetConfig,
            bellSyncDiffMillis: Long
    ) {
        // get the current bell-synced time
        val now = Time.fromMillis(Time.getNow().inMillis + bellSyncDiffMillis)

        // set the widget transparency
        val mode = PorterDuff.Mode.DST_IN
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // this code seems to crash the launcher on >= P
            val transparency = widgetConfig.opacity  //0...1
            val colorFilter = 0x01000000L * (255f * transparency).toLong()
            try {
                val declaredMethods = Class.forName("android.widget.RemoteViews").declaredMethods
                val len = declaredMethods.size
                if (len > 0) {
                    for (m in 0 until len) {
                        val method = declaredMethods[m]
                        if (method.name == "setDrawableParameters") {
                            method.isAccessible = true
                            method.invoke(views, R.id.widgetTimetableBackground, true, -1, colorFilter.toInt(), mode, -1)
                            method.invoke(views, R.id.widgetTimetableHeader, true, -1, colorFilter.toInt(), mode, -1)
                            break
                        }
                    }
                }
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }

        val unified = widgetConfig.profileId == -1

        // get all profiles or one profile with the specified id
        val profileList = if (unified)
            app.db.profileDao().allNow.filterOutArchived()
        else
            listOfNotNull(app.db.profileDao().getByIdNow(widgetConfig.profileId))

        // no profile was found
        if (profileList.isEmpty()) {
            views.setViewVisibility(R.id.widgetTimetableLoading, View.VISIBLE)
            views.setTextViewText(R.id.widgetTimetableLoading, app.getString(R.string.widget_timetable_profile_doesnt_exist))
            return
        }

        views.setViewVisibility(R.id.widgetTimetableLoading, View.GONE)

        // set lesson search bounds
        val today = Date.getToday()
        val searchEnd = today.clone().stepForward(0, 0, 7)

        var scrollPos = 0

        var profileId: Int? = null
        var displayingDate: Date? = null

        val models = mutableListOf<ItemWidgetTimetableModel>()

        // get all lessons within the search bounds
        val lessonList = app.db.timetableDao().getBetweenDatesNow(today, searchEnd)

        for (profile in profileList) {

            // add a profile separator with its name
            if (unified) {
                val separator = ItemWidgetTimetableModel()
                separator.profileId = profile.id
                separator.bigStyle = widgetConfig.bigStyle
                separator.darkTheme = widgetConfig.darkTheme
                separator.separatorProfileName = profile.name
                models.add(separator)
            }

            // search for lessons to display
            val timetableDate = Date.getToday()
            var checkedDays = 0
            var lessons = lessonList.filter {
                it.profileId == profile.id
                        && it.displayDate == timetableDate
                        /*&& it.displayEndTime > now*/
                        && !(it.isCancelled && ignoreCancelled)
            }
            while ((lessons.isEmpty() || lessons.none {
                        it.type != Lesson.TYPE_NO_LESSONS
                                && (it.displayDate != today
                                || (it.displayDate == today
                                && it.displayEndTime != null
                                && it.displayEndTime!! >= now))
                    }) && checkedDays < 7) {

                timetableDate.stepForward(0, 0, 1)
                lessons = lessonList.filter {
                    it.profileId == profile.id
                            && it.displayDate == timetableDate
                            && !(it.isCancelled && ignoreCancelled)
                }

                if (lessons.isEmpty() && timetableDate.weekDay <= 5)
                    break

                checkedDays++
            }

            // set the displayingDate to show in the header
            if (!unified) {
                if (lessons.isNotEmpty())
                    displayingDate = timetableDate
                profileId = profile.id
                if (lessons.isEmpty()) {
                    views.setViewVisibility(R.id.widgetTimetableListView, View.GONE)
                    views.setViewVisibility(R.id.widgetTimetableNoTimetable, View.VISIBLE)
                }
                if (lessons.size == 1 && lessons[0].type == Lesson.TYPE_NO_LESSONS) {
                    views.setViewVisibility(R.id.widgetTimetableListView, View.GONE)
                    views.setViewVisibility(R.id.widgetTimetableNoLessons, View.VISIBLE)
                }
            }
            else {
                if (lessons.isEmpty()) {
                    val separator = ItemWidgetTimetableModel()
                    separator.profileId = profile.id
                    separator.bigStyle = widgetConfig.bigStyle
                    separator.darkTheme = widgetConfig.darkTheme
                    separator.isNoTimetableItem = true;
                    models.add(separator)
                }
                if (lessons.size == 1 && lessons[0].type == Lesson.TYPE_NO_LESSONS) {
                    val separator = ItemWidgetTimetableModel()
                    separator.profileId = profile.id
                    separator.bigStyle = widgetConfig.bigStyle
                    separator.darkTheme = widgetConfig.darkTheme
                    separator.isNoLessonsItem = true;
                    models.add(separator)
                }
            }

            // get all events for the current date
            val events = app.db.eventDao().getAllByDateNow(profile.id, timetableDate)?.filterNotNull() ?: emptyList()

            lessons.forEachIndexed { pos, lesson ->
                if (lesson.type == TYPE_NO_LESSONS)
                    return@forEachIndexed
                val model = ItemWidgetTimetableModel()

                model.bigStyle = widgetConfig.bigStyle
                model.darkTheme = widgetConfig.darkTheme

                model.profileId = profile.id

                model.lessonId = lesson.id
                model.lessonDate = timetableDate
                model.startTime = lesson.displayStartTime ?: return@forEachIndexed
                model.endTime = lesson.displayEndTime ?: return@forEachIndexed

                // check if the lesson has already passed or it's currently in progress
                if (lesson.displayDate == today) {
                    lesson.displayEndTime?.let { endTime ->
                        model.lessonPassed = now > endTime
                        lesson.displayStartTime?.let { startTime ->
                            model.lessonCurrent = now in startTime..endTime
                        }
                    }
                }

                // set where should the list view scroll to
                if (model.lessonCurrent) {
                    scrollPos = pos
                } else if (model.lessonPassed) {
                    scrollPos = pos + 1
                }

                // set the subject and classroom name
                model.subjectName = lesson.displaySubjectName
                model.classroomName = lesson.displayClassroom

                // set the bell sync to calculate progress in ListProvider
                model.bellSyncDiffMillis = bellSyncDiffMillis

                // make the model aware of the lesson type
                when (lesson.type) {
                    Lesson.TYPE_CANCELLED -> {
                        model.lessonCancelled = true
                    }
                    Lesson.TYPE_CHANGE,
                        Lesson.TYPE_SHIFTED_SOURCE,
                        Lesson.TYPE_SHIFTED_TARGET -> {
                        model.lessonChange = true
                    }
                }

                // add every event on this lesson
                for (event in events) {
                    if (event.startTime == null || event.startTime != lesson.displayStartTime)
                        continue
                    model.eventColors.add(if (event.type == TYPE_HOMEWORK) ItemWidgetTimetableModel.EVENT_COLOR_HOMEWORK else event.getColor())
                }

                models += model
            }
        }

        if (unified) {
            // set the title for an unified widget
            views.setTextViewText(R.id.widgetTimetableTitle, app.getString(R.string.widget_timetable_title_unified))
            views.setViewVisibility(R.id.widgetTimetableSubtitle, View.GONE)
        } else {
            // set the title to present the widget's profile
            views.setTextViewText(R.id.widgetTimetableTitle, profileList[0].name)
            views.setViewVisibility(R.id.widgetTimetableTitle, View.VISIBLE)
            // make the subtitle show current date for these lessons
            displayingDate?.let {
                when (Date.diffDays(it, Date.getToday())) {
                    0 -> views.setTextViewText(R.id.widgetTimetableSubtitle, app.getString(R.string.day_today_format, Week.getFullDayName(it.weekDay)))
                    1 -> views.setTextViewText(R.id.widgetTimetableSubtitle, app.getString(R.string.day_tomorrow_format, Week.getFullDayName(it.weekDay)))
                    else -> views.setTextViewText(R.id.widgetTimetableSubtitle, Week.getFullDayName(it.weekDay) + " " + it.formattedString)
                }
            }
        }

        // intent running when the header is clicked
        val headerIntent = Intent(app, MainActivity::class.java)
        headerIntent.action = "android.intent.action.MAIN"
        if (!unified) {
            // per-profile widget should redirect to it + correct day
            profileId?.let {
                headerIntent.putExtra("profileId", it)
            }
            displayingDate?.let {
                headerIntent.putExtra("timetableDate", it.value)
            }
        }
        headerIntent.putExtra("fragmentId", MainActivity.DRAWER_ITEM_TIMETABLE)
        val headerPendingIntent = PendingIntent.getActivity(app, appWidgetId, headerIntent, 0)
        views.setOnClickPendingIntent(R.id.widgetTimetableHeader, headerPendingIntent)

        timetables!!.put(appWidgetId, models)

        // apply the list service to the list view
        val listIntent = Intent(app, WidgetTimetableService::class.java)
        listIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        listIntent.data = Uri.parse(listIntent.toUri(Intent.URI_INTENT_SCHEME))
        views.setRemoteAdapter(R.id.widgetTimetableListView, listIntent)

        // create an intent used to display the lesson details dialog
        val itemIntent = Intent(app, LessonDialogActivity::class.java)
        itemIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK/* or Intent.FLAG_ACTIVITY_CLEAR_TASK*/)
        val itemPendingIntent = PendingIntent.getActivity(app, appWidgetId, itemIntent, 0)
        views.setPendingIntentTemplate(R.id.widgetTimetableListView, itemPendingIntent)

        if (!unified)
            views.setScrollPosition(R.id.widgetTimetableListView, scrollPos)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val app = context.applicationContext as App
        val widgetConfigs = app.config.widgetConfigs
        appWidgetIds.forEach {
            widgetConfigs.remove(it.toString())
        }
        app.config.widgetConfigs = widgetConfigs
    }
}
