package pl.szczodrzynski.edziennik.utils.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.entity.Note;
import pl.szczodrzynski.edziennik.data.db.entity.Noteable;
import pl.szczodrzynski.edziennik.ext.TextExtensionsKt;

public class Date implements Comparable<Date>, Noteable {
    public int year = 0;
    public int month = 0;
    public int day = 0;

    public Date() {
        this(2000, 0, 0);
    }

    public Date(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public Date setYear(int year) {
        this.year = year;
        return this;
    }

    public Date setMonth(int month) {
        this.month = month;
        return this;
    }

    public Date setDay(int day) {
        this.day = day;
        return this;
    }

    public Date clone() {
        return new Date(this.year, this.month, this.day);
    }

    public Date getWeekStart() {
        return clone().stepForward(0, 0, -getWeekDay());
    }

    public Date getWeekEnd() {
        return clone().stepForward(0, 0, 6-getWeekDay());
    }

    public static Date fromYmd(String dateTime) {
        try {
            return new Date(Integer.parseInt(dateTime.substring(0, 4)), Integer.parseInt(dateTime.substring(4, 6)), Integer.parseInt(dateTime.substring(6, 8)));
        }
        catch (Exception e) {
            return new Date(2019, 1, 1);
        }
    }

    public static Date fromMillis(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return new Date(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static Date fromMillisUtc(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        return new Date(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static Date fromCalendar(Calendar c) {
        return new Date(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static long getNowInMillis() {
        return Calendar.getInstance().getTimeInMillis();
    }

    public static Date fromY_m_d(String dateTime) {
        try {
            return new Date(Integer.parseInt(dateTime.substring(0, 4)), Integer.parseInt(dateTime.substring(5, 7)), Integer.parseInt(dateTime.substring(8, 10)));
        }
        catch (Exception e) {
            return new Date(2019, 1, 1);
        }
    }

    public Calendar getAsCalendar() {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    public Calendar getAsCalendar(Time time) {
        if (time == null)
            return getAsCalendar();
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, time.hour, time.minute, time.second);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    public long getInMillis() {
        Calendar c = getAsCalendar();
        return c.getTimeInMillis();
    }

    public long getInMillisUtc() {
        Calendar c = getAsCalendar();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        return c.getTimeInMillis();
    }

    public long getInUnix() {
        return getInMillis() / 1000;
    }

    public static Date fromd_m_Y(String dateTime) {
        try {
            return new Date(Integer.parseInt(dateTime.substring(6, 10)), Integer.parseInt(dateTime.substring(3, 5)), Integer.parseInt(dateTime.substring(0, 2)));
        }
        catch (Exception e) {
            return new Date(2019, 1, 1);
        }
    }

    public static long fromIso(String dateTime) {
        try {
            Calendar c = Calendar.getInstance();
            c.set(Integer.parseInt(dateTime.substring(0, 4)), Integer.parseInt(dateTime.substring(5, 7)) - 1, Integer.parseInt(dateTime.substring(8, 10)), Integer.parseInt(dateTime.substring(11, 13)), Integer.parseInt(dateTime.substring(14, 16)), Integer.parseInt(dateTime.substring(17, 19)));
            c.set(Calendar.MILLISECOND, 0);
            if (dateTime.endsWith("Z")) {
                c.setTimeZone(TimeZone.getTimeZone("UTC"));
            }
            return c.getTimeInMillis();
        }
        catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    public static long fromIsoHm(String dateTime) {
        try {
            return Date.fromY_m_d(dateTime).combineWith(new Time(Integer.parseInt(dateTime.substring(11, 13)), Integer.parseInt(dateTime.substring(14, 16)), 0));
        }
        catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    public static Date fromValue(int value) {
        int year = value / 10000;
        int month = (value - year * 10000) / 100;
        int day = (value - year * 10000 - month * 100);
        return new Date(year, month, day);
    }

    public static Date getToday() {
        Calendar cal = Calendar.getInstance();
        return new Date(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    public static int diffDays(Date d1, Date d2) {
        return Math.round((d1.getInMillis() - d2.getInMillis()) / (24 * 60 * 60 * 1000f));
    }

    public static boolean isToday(Date date) {
        return equals(date, getToday());
    }

    public static boolean isToday(String dateStr) {
        return equals(dateStr, getToday());
    }

    public static boolean equals(Date first, Date second) {
        return (first.getValue() == second.getValue());
    }

    public static boolean equals(String first, Date second) {
        return equals(new Date().parseFromYmd(first), second);
    }

    public long combineWith(Time time) {
        return getAsCalendar(time).getTimeInMillis();
    }

    public int getWeekDay() {
        return Week.getWeekDayFromDate(this);
    }

    @NonNull
    public Date stepForward(int years, int months, int days) {
        this.day += days;
        if (day <= 0) {
            month--;
            if(month <= 0) {
                month += 12;
                year--;
            }
            day += daysInMonth();
        }
        if (day > daysInMonth()) {
            day -= daysInMonth();
            month++;
        }
        this.month += months;
        if(month <= 0) {
            month += 12;
            year--;
        }
        if (month > 12) {
            month -= 12;
            year++;
        }
        this.year += years;
        /*Calendar c = Calendar.getInstance();
        int newMonth = month + months;
        if (newMonth > 12) {
            newMonth = 1;
            years++;
        }
        c.set(year + years, newMonth - 1, day);
        c.setTimeInMillis(c.getTimeInMillis() + days * 24 * 60 * 60 * 1000);
        this.year = c.get(Calendar.YEAR);
        this.month = c.get(Calendar.MONTH) + 1;
        this.day = c.get(Calendar.DAY_OF_MONTH);*/
        return this;
    }

    public Date parseFromYmd(String dateTime) {
        this.year = Integer.parseInt(dateTime.substring(0, 4));
        this.month = Integer.parseInt(dateTime.substring(4, 6));
        this.day = Integer.parseInt(dateTime.substring(6, 8));
        return this;
    }

    public int getValue() {
        return year * 10000 + month * 100 + day;
    }

    public String getStringValue() {
        return Integer.toString(getValue());
    }

    public boolean isLeap() {
        return ((year & 3) == 0) && ((year % 100) != 0 || (year % 400) == 0);
    }

    public int daysInMonth() {
        switch (month) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                return 31;
            case 2: return isLeap() ? 29 : 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
        }
        return 31;
    }

    public String getStringYmd() {
        return year + (month < 10 ? "0" : "") + month + (day < 10 ? "0" : "") + day;
    }

    /**
     * @return 2019-06-02
     */
    @NonNull
    public String getStringY_m_d() {
        return year + (month < 10 ? "-0" : "-") + month + (day < 10 ? "-0" : "-") + day;
    }

    public String getStringDm() {
        return day + "." + (month < 10 ? "0" : "") + month;
    }

    public String getStringDmy() {
        return day + "." + (month < 10 ? "0" : "") + month + "." + year;
    }

    public String getFormattedString() {
        java.util.Date date = new java.util.Date();
        date.setTime(getInMillis());
        DateFormat format = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
        if (year == Date.getToday().year) {
            return format.format(date).replace(", " + year, "").replace(" " + year, "");
        } else {
            return format.format(date);
        }
    }

    public String getFormattedStringShort() {
        java.util.Date date = new java.util.Date();
        date.setTime(getInMillis());
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        if (year == Date.getToday().year) {
            return format.format(date).replace(", " + year, "").replace(" " + year, "");
        } else {
            return format.format(date);
        }
    }

    public static String dayDiffString(Context context, int dayDiff) {
        if (dayDiff > 0) {
            if (dayDiff == 1) {
                return context.getString(R.string.tomorrow);
            }
            else if (dayDiff == 2) {
                return context.getString(R.string.the_day_after);
            }
            return context.getString(R.string.in_format, TextExtensionsKt.plural(context, R.plurals.time_till_days, Math.abs(dayDiff)));
        }
        else if (dayDiff < 0) {
            if (dayDiff == -1) {
                return context.getString(R.string.yesterday);
            }
            else if (dayDiff == -2) {
                return context.getString(R.string.the_day_before);
            }
            return context.getString(R.string.ago_format, TextExtensionsKt.plural(context, R.plurals.time_till_days, Math.abs(dayDiff)));
        }
        return context.getString(R.string.today);
    }

    @Nullable
    public String getRelativeString(Context context, int maxDiff) {
        int diffDays = Date.diffDays(this, Date.getToday());
        if (maxDiff != 0 && Math.abs(diffDays) > maxDiff) {
            return null;
        }
        return dayDiffString(context, diffDays);
    }

    @Override
    public int compareTo(@NonNull Date o) {
        return this.getValue() - o.getValue();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof Date && this.getValue() == ((Date) obj).getValue();
    }

    @Override
    public String toString() {
        return "Date{" +
                "year=" + year +
                ", month=" + month +
                ", day=" + day +
                '}';
    }

    @Override
    public int hashCode() {
        int result = year;
        result = 31 * result + month;
        result = 31 * result + day;
        return result;
    }

    @NonNull
    @Override
    public Note.OwnerType getNoteType() {
        return Note.OwnerType.DAY;
    }

    @Override
    public int getNoteOwnerProfileId() {
        return 0;
    }

    @Override
    public long getNoteOwnerId() {
        return 0;
    }

    @Nullable
    @Override
    public CharSequence getNoteSubstituteText(boolean showNotes) {
        return null;
    }

    @NonNull
    @Override
    public List<Note> getNotes() {
        return new ArrayList();
    }

    @Override
    public void setNotes(@NonNull List<Note> notes) {
    }

    @Override
    public void filterNotes() {
        Noteable.DefaultImpls.filterNotes(this);
    }

    @Override
    public boolean hasNotes() {
        return Noteable.DefaultImpls.hasNotes(this);
    }

    @Override
    public boolean hasReplacingNotes() {
        return Noteable.DefaultImpls.hasReplacingNotes(this);
    }

    @Nullable
    @Override
    public Long getNoteShareTeamId() {
        return Noteable.DefaultImpls.getNoteShareTeamId(this);
    }
}
