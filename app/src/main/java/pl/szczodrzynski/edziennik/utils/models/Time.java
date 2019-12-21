package pl.szczodrzynski.edziennik.utils.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;

public class Time implements Comparable<Time> {
    public int hour = 0;
    public int minute = 0;
    public int second = 0;

    public Time()
    {
        this(0, 0, 0);
    }

    public Time(int hour, int minute, int second)
    {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }

    public Time clone() {
        return new Time(this.hour, this.minute, this.second);
    }

    public Time parseFromYmdHm(String dateTime)
    {
        this.hour = Integer.parseInt(dateTime.substring(8, 10));
        this.minute = Integer.parseInt(dateTime.substring(10, 12));
        this.second = 0;
        return this;
    }

    public static Time fromYmdHm(String dateTime)
    {
        return new Time(Integer.parseInt(dateTime.substring(8, 10)), Integer.parseInt(dateTime.substring(10, 12)), 0);
    }

    public long combineWith(Date date) {
        if (date == null) {
            return getInMillis();
        }
        Calendar c = Calendar.getInstance();
        c.set(date.year, date.month-1, date.day, this.hour, this.minute, this.second);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public Time stepForward(int hours, int minutes, int seconds) {
        Calendar c = Calendar.getInstance();
        c.set(2000, 0, 0, hour, minute, second);
        c.setTimeInMillis(c.getTimeInMillis() + seconds*1000 + minutes*60*1000 + hours*60*60*1000);
        this.hour = c.get(Calendar.HOUR_OF_DAY);
        this.minute = c.get(Calendar.MINUTE);
        this.second = c.get(Calendar.SECOND);
        return this;
    }

    /**
     * HHMMSS
     */
    public static Time fromHms(String time)
    {
        try {
            return new Time(Integer.parseInt(time.substring(0, 2)), Integer.parseInt(time.substring(2, 4)), Integer.parseInt(time.substring(4, 6)));
        }
        catch (Exception e) {
            e.printStackTrace();
            return new Time(0, 0, 0);
        }
    }

    /**
     * HH:MM
     */
    public static Time fromH_m(String time)
    {
        try {
            return new Time(Integer.parseInt(time.substring(0, 2)), Integer.parseInt(time.substring(3, 5)), 0);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new Time(0, 0, 0);
        }
    }

    /**
     * HH:MM:SS
     */
    public static Time fromH_m_s(String time)
    {
        try {
            return new Time(Integer.parseInt(time.substring(0, 2)), Integer.parseInt(time.substring(3, 5)), Integer.parseInt(time.substring(6, 8)));
        }
        catch (Exception e) {
            e.printStackTrace();
            return new Time(0, 0, 0);
        }
    }

    public static Time fromValue(int value) {
        int hours = value / 10000;
        int minutes = (value - hours * 10000) / 100;
        int seconds = (value - hours * 10000 - minutes * 100);
        return new Time(hours, minutes, seconds);
    }

    public long getInMillis() {
        Calendar c = Calendar.getInstance();
        c.set(2000, 0, 1, hour, minute, second);
        //Log.d("Time", "Millis "+c.getTimeInMillis());
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
    public static Time fromMillis(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return new Time(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
    }

    public long getInUnix() {
        return getInMillis() / 1000;
    }

    public int getValue()
    {
        return hour * 10000 + minute * 100 + second;
    }

    public String getStringValue()
    {
        return (hour < 10 ? "0" : "")+ hour +(minute < 10 ? "0" : "")+ minute +(second < 10 ? "0" : "")+ second;
    }

    public String getStringHM()
    {
        if (hour < 0) {
            return "";
        }
        return hour +":"+(minute < 10 ? "0" : "")+ minute;
    }
    public String getStringH_M()
    {
        if (hour < 0) {
            return "";
        }
        return hour +"-"+(minute < 10 ? "0" : "")+ minute;
    }
    public String getStringHMS()
    {
        return hour +":"+(minute < 10 ? "0" : "")+ minute +":"+(second < 10 ? "0" : "")+ second;
    }

    public static Time getNow()
    {
        Calendar cal = Calendar.getInstance();
        return new Time(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
    }

    public static Time diff(Time t1, Time t2) {
        long t1millis = t1.getInMillis();
        long t2millis = t2.getInMillis();
        int multiplier = (t1millis > t2millis ? 1 : -1);
        Time diff = Time.fromMillis((t1millis - t2millis)*multiplier);
        // diff.hour -= 1;
        return diff;
    }

    public static Time sum(Time t1, Time t2) {
        long t1millis = t1.getInMillis();
        long t2millis = t2.getInMillis();
        Time sum = Time.fromMillis((t1millis + t2millis));
        // sum.hour += 1;
        return sum;
    }

    public static boolean inRange(Time startTime, Time endTime)
    {
        return inRange(startTime, endTime, getNow());
    }

    public static boolean inRange(Time startTime, Time endTime, Time currentTime)
    {
        return (currentTime.getValue() >= startTime.getValue() && currentTime.getValue() <= endTime.getValue());
    }

    @Override
    public int compareTo(@NonNull Time o) {
        return this.getValue() - o.getValue();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof Time && this.getValue() == ((Time) obj).getValue();
    }

    @Override
    public String toString() {
        return "Time{" +
                "hour=" + hour +
                ", minute=" + minute +
                ", second=" + second +
                '}';
    }

    @Override
    public int hashCode() {
        int result = hour;
        result = 31 * result + minute;
        result = 31 * result + second;
        return result;
    }

    public long minus(@NotNull Time other) {
        return getInUnix() - other.getInUnix();
    }
}
