package pl.szczodrzynski.edziennik.utils.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class Week {
    public static int MONDAY = 0;
    public static int TUESDAY = 1;
    public static int WEDNESDAY = 2;
    public static int THURSDAY = 3;
    public static int FRIDAY = 4;
    public static int SATURDAY = 5;
    public static int SUNDAY = 6;

    public static pl.szczodrzynski.edziennik.utils.models.Date getNearestWeekDayDate(int day)
    {
        Calendar c = Calendar.getInstance();
        int dayDiff = day - getTodayWeekDay();
        if (dayDiff < 0) {
            dayDiff = 7 + dayDiff;
        }
        c.setTimeInMillis(c.getTimeInMillis() + (dayDiff * 24 * 60 * 60 * 1000));
        return new pl.szczodrzynski.edziennik.utils.models.Date(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static pl.szczodrzynski.edziennik.utils.models.Date getWeekStart() {
        pl.szczodrzynski.edziennik.utils.models.Date date = pl.szczodrzynski.edziennik.utils.models.Date.getToday();
        date.stepForward(0, 0, -date.getWeekDay());
        return date;
    }
    public static pl.szczodrzynski.edziennik.utils.models.Date getWeekEnd() {
        pl.szczodrzynski.edziennik.utils.models.Date date = pl.szczodrzynski.edziennik.utils.models.Date.getToday();
        date.stepForward(0, 0, 6-date.getWeekDay());
        return date;
    }

    public static int getWeekDayFromDate(pl.szczodrzynski.edziennik.utils.models.Date date)
    {
        Calendar c = Calendar.getInstance();
        c.set(date.year, date.month - 1, date.day);
        int weekDay = c.get(Calendar.DAY_OF_WEEK);
        weekDay = (weekDay - 2 < 0 ? 6 : weekDay - 2);
        return weekDay;
    }

    public static int getWeekDayFromDate(String dateStr)
    {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        Date date = null;
        try {
            date = simpleDateFormat.parse(dateStr);
        } catch (ParseException e) {
            Timber.e(e);
        }
        c.setTime(date);
        int weekDay = c.get(Calendar.DAY_OF_WEEK);
        weekDay = (weekDay - 2 < 0 ? 6 : weekDay - 2);
        return weekDay;
    }

    public static int getTodayWeekDay()
    {
        Calendar c = Calendar.getInstance();
        int weekDay = c.get(Calendar.DAY_OF_WEEK);
        weekDay = (weekDay - 2 < 0 ? 6 : weekDay - 2);
        return weekDay;
    }

    // stock code incoming...
    public static String getFullDayName(int day) {
        Calendar c = Calendar.getInstance();
        // date doesn't matter - it has to be a Monday
        // I new that first August 2011 is one ;-)
        c.set(2011, 7, 1, 0, 0, 0);
        c.add(Calendar.DAY_OF_MONTH, day);
        return String.format("%tA", c);
    }
}
