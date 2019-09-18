package pl.szczodrzynski.edziennik.datamodels;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;

@Entity(tableName = "grades",
        primaryKeys = {"profileId", "gradeId"},
        indices = {@Index(value = {"profileId"})})
public class Grade {
    public int profileId;

    @ColumnInfo(name = "gradeId")
    public long id;

    @ColumnInfo(name = "gradeCategory")
    public String category;
    @ColumnInfo(name = "gradeColor")
    public int color;
    @ColumnInfo(name = "gradeDescription")
    public String description;
    @ColumnInfo(name = "gradeComment")
    public String comment;
    @ColumnInfo(name = "gradeName")
    public String name;
    @ColumnInfo(name = "gradeValue")
    public float value;
    @ColumnInfo(name = "gradeValueMax")
    public float valueMax;
    @ColumnInfo(name = "gradeWeight")
    public float weight;
    @ColumnInfo(name = "gradeSemester")
    public int semester;
    @ColumnInfo(name = "gradeClassAverage")
    public float classAverage = -1;
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_SEMESTER1_PROPOSED = 1;
    public static final int TYPE_SEMESTER1_FINAL = 2;
    public static final int TYPE_SEMESTER2_PROPOSED = 3;
    public static final int TYPE_SEMESTER2_FINAL = 4;
    public static final int TYPE_YEAR_PROPOSED = 5;
    public static final int TYPE_YEAR_FINAL = 6;
    public static final int TYPE_POINT = 10;
    public static final int TYPE_BEHAVIOUR = 20;
    public static final int TYPE_DESCRIPTIVE = 30;
    public static final int TYPE_TEXT = 40;
    @ColumnInfo(name = "gradeType")
    public int type = TYPE_NORMAL;
    @ColumnInfo(name = "gradePointGrade")
    public boolean pointGrade = false;

    /**
     * Applies for historical grades. It's the new/replacement grade's ID.
     */
    @ColumnInfo(name = "gradeParentId")
    public long parentId = -1;

    /**
     * Applies for current grades. If the grade was worse and this is the improved one.
     */
    @ColumnInfo(name = "gradeIsImprovement")
    public boolean isImprovement = false;

    public long teacherId;
    public long subjectId;

    @Ignore
    public Grade() {}

    public Grade(int profileId, long id, String category, int color, String description, String name, float value, float weight, int semester, long teacherId, long subjectId) {
        this.profileId = profileId;
        this.id = id;
        this.category = category;
        this.color = color;
        this.description = description;
        this.name = name;
        this.value = value;
        this.weight = weight;
        this.semester = semester;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
    }

    /*@Ignore
    public Grade(int profileId, long id, String description, String name, float value, float weight, int semester, long teacherId, long categoryId, long subjectId) {
        this.profileId = profileId;
        this.id = id;
        this.description = description;
        this.name = name;
        this.value = value;
        this.weight = weight;
        this.semester = semester;
        this.teacherId = teacherId;
        //this.categoryId = categoryId;
        this.subjectId = subjectId;
    }*/
}

