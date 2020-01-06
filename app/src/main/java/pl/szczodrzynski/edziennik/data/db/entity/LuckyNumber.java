/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

import pl.szczodrzynski.edziennik.utils.models.Date;

@Entity(tableName = "luckyNumbers",
        primaryKeys = {"profileId", "luckyNumberDate"})
public class LuckyNumber {
    public int profileId;

    @NonNull
    @ColumnInfo(name = "luckyNumberDate", typeAffinity = 3)
    public Date date;
    @ColumnInfo(name = "luckyNumber")
    public int number;

    public LuckyNumber(int profileId, @NonNull Date date, int number) {
        this.profileId = profileId;
        this.date = date;
        this.number = number;
    }

    @Ignore
    public LuckyNumber() {
        this.date = Date.getToday();
    }
}
