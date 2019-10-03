package pl.szczodrzynski.edziennik.data.db.modules.notices;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;

@Entity(tableName = "notices",
        primaryKeys = {"profileId", "noticeId"},
        indices = {@Index(value = {"profileId"})})
public class Notice {
    public int profileId;

    @ColumnInfo(name = "noticeId")
    public long id;

    @ColumnInfo(name = "noticeText")
    public String text;
    @ColumnInfo(name = "noticeSemester")
    public int semester;
    public static final int TYPE_NEUTRAL = 0;
    public static final int TYPE_POSITIVE = 1;
    public static final int TYPE_NEGATIVE = 2;
    @ColumnInfo(name = "noticeType")
    public int type = TYPE_NEUTRAL;

    public float points = 0;
    public String category = null;

    public long teacherId;

    @Ignore
    public Notice() {}

    public Notice(int profileId, long id, String text, int semester, int type, long teacherId) {
        this.profileId = profileId;
        this.id = id;
        this.text = text;
        this.semester = semester;
        this.type = type;
        this.teacherId = teacherId;
    }
}


