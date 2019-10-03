package pl.szczodrzynski.edziennik.data.db.modules.luckynumber;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.annotation.NonNull;

import pl.szczodrzynski.edziennik.utils.models.Date;

@Entity(tableName = "luckyNumbers",
        primaryKeys = {"profileId", "luckyNumberDate"})
public class LuckyNumber {
    public int profileId;

    @NonNull
    @ColumnInfo(name = "luckyNumberDate")
    public Date date;
    @ColumnInfo(name = "luckyNumber")
    public int number;

    public LuckyNumber(int profileId, @NonNull Date date, int number) {
        this.profileId = profileId;
        this.date = date;
        this.number = number;
    }

}
