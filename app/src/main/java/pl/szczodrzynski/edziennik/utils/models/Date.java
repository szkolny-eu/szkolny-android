package pl.szczodrzynski.edziennik.utils.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Date implements Comparable<Date> {
    public int year = 0;
    public int month = 0;
    public int day = 0;

    public Date()
    {
        this(2000, 0, 0);
    }

    public Date(int year, int month, int day)
    {
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

    public long combineWith(Time time) {
        if (time == null) {
            return getInMillis();
        }
        Calendar c = Calendar.getInstance();
        c.set(this.year, this.month-1, this.day, time.hour, time.minute, time.second);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static Date fromMillis(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return new Date(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static Date fromCalendar(Calendar c) {
        return new Date(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static long getNowInMillis() {
        return Calendar.getInstance().getTimeInMillis();
    }

    public int getWeekDay()
    {
        return Week.getWeekDayFromDate(this);
    }

    public long getInMillis() {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public long getInUnix() {
        return getInMillis() / 1000;
    }

    public Date stepForward(int years, int months, int days)
    {
        Calendar c = Calendar.getInstance();
        int newMonth = month + months;
        if (newMonth > 12) {
            newMonth = 1;
            years++;
        }
        c.set(year+years, newMonth - 1, day);
        c.setTimeInMillis(c.getTimeInMillis() + days*24*60*60*1000);
        this.year = c.get(Calendar.YEAR);
        this.month = c.get(Calendar.MONTH) + 1;
        this.day = c.get(Calendar.DAY_OF_MONTH);
        return this;
    }

    public Date parseFromYmd(String dateTime)
    {
        this.year = Integer.parseInt(dateTime.substring(0, 4));
        this.month = Integer.parseInt(dateTime.substring(4, 6));
        this.day = Integer.parseInt(dateTime.substring(6, 8));
        return this;
    }

    public static Date fromYmd(String dateTime)
    {
        return new Date(Integer.parseInt(dateTime.substring(0, 4)), Integer.parseInt(dateTime.substring(4, 6)), Integer.parseInt(dateTime.substring(6, 8)));
    }

    public static Date fromY_m_d(String dateTime)
    {
        return new Date(Integer.parseInt(dateTime.substring(0, 4)), Integer.parseInt(dateTime.substring(5, 7)), Integer.parseInt(dateTime.substring(8, 10)));
    }

    public static long fromIso(String dateTime)
    {
        return Date.fromY_m_d(dateTime).combineWith(new Time(Integer.parseInt(dateTime.substring(11, 13)), Integer.parseInt(dateTime.substring(14, 16)), Integer.parseInt(dateTime.substring(17, 19))));
    }

    public static long fromIsoHm(String dateTime)
    {
        return Date.fromY_m_d(dateTime).combineWith(new Time(Integer.parseInt(dateTime.substring(11, 13)), Integer.parseInt(dateTime.substring(14, 16)), 0));
    }

    public int getValue()
    {
        return year * 10000 + month * 100 + day;
    }

    public String getStringValue()
    {
        return Integer.toString(getValue());
    }

    public String getStringYmd()
    {
        return year +(month < 10 ? "0" : "")+ month +(day < 10 ? "0" : "")+ day;
    }

    /**
     * @return 2019-06-02
     */
    public String getStringY_m_d()
    {
        return year +(month < 10 ? "-0" : "-")+ month +(day < 10 ? "-0" : "-")+ day;
    }
    public String getStringDm()
    {
        return day +"."+(month < 10 ? "0" : "")+ month;
    }
    public String getStringDmy()
    {
        return day +"."+(month < 10 ? "0" : "")+ month +"."+ year;
    }

    public String getFormattedString()
    {
        java.util.Date date = new java.util.Date();
        date.setTime(getInMillis());
        DateFormat format = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
        if (year == Date.getToday().year) {
            return format.format(date).replace(", "+year, "").replace(" "+year, "");
        }
        else {
            return format.format(date);
        }
    }
    public String getFormattedStringShort()
    {
        java.util.Date date = new java.util.Date();
        date.setTime(getInMillis());
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        if (year == Date.getToday().year) {
            return format.format(date).replace(", "+year, "").replace(" "+year, "");
        }
        else {
            return format.format(date);
        }
    }

    public boolean isLeap() {
        return ((year & 3) == 0) && ((year % 100) != 0 || (year % 400) == 0);
    }

    public static Date getToday()
    {
        Calendar cal = Calendar.getInstance();
        return new Date(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH));
    }

    public static int diffDays(Date d1, Date d2) {
        return (int)((d1.getInMillis() - d2.getInMillis()) / (24*60*60*1000));
    }

    public static boolean isToday(Date date)
    {
        return equals(date, getToday());
    }
    public static boolean isToday(String dateStr)
    {
        return equals(dateStr, getToday());
    }
    public static boolean equals(Date first, Date second)
    {
        return (first.getValue() == second.getValue());
    }
    public static boolean equals(String first, Date second)
    {
        return equals(new Date().parseFromYmd(first), second);
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
}
