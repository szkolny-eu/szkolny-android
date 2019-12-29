package pl.szczodrzynski.edziennik.data.db.modules.grades;

import androidx.room.Entity;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "gradeCategories",
        primaryKeys = {"profileId", "categoryId", "type"})
public class GradeCategory {
    public int profileId;

    public long categoryId;
    public float weight;
    public int color;
    public String text;
    public List<String> columns;
    public float valueFrom = 0;
    public float valueTo = 0;

    /**
     * A general purpose category type.
     *
     * The Grade category is used only in API to cache the e-register's categories.
     */
    public int type = 0;

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_NORMAL_COMMENT = 1;
    public static final int TYPE_BEHAVIOUR = 2;
    public static final int TYPE_BEHAVIOUR_COMMENT = 3;
    public static final int TYPE_DESCRIPTIVE = 4;
    public static final int TYPE_TEXT = 5;
    public static final int TYPE_POINT = 6;

    public GradeCategory(int profileId, long categoryId, float weight, int color, String text) {
        this.profileId = profileId;
        this.categoryId = categoryId;
        this.weight = weight;
        this.color = color;
        this.text = text;
        this.columns = new ArrayList<>();
    }
    public GradeCategory setValueRange(float from, float to) {
        this.valueFrom = from;
        this.valueTo = to;
        return this;
    }
    public GradeCategory addColumn(String text) {
        columns.add(text);
        return this;
    }
    public GradeCategory addColumns(List<String> list) {
        columns.addAll(list);
        return this;
    }

    public static GradeCategory search(List<GradeCategory> gradeCategoryList, long categoryId) {
        for (GradeCategory gradeCategory: gradeCategoryList) {
            if (gradeCategory.categoryId == categoryId)
                return gradeCategory;
        }
        return null;
    }
}
