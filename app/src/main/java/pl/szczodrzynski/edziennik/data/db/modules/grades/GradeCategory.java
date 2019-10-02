package pl.szczodrzynski.edziennik.data.db.modules.grades;

import java.util.ArrayList;
import java.util.List;

import androidx.room.Entity;

@Entity(tableName = "gradeCategories",
        primaryKeys = {"profileId", "categoryId"})
public class GradeCategory {
    public int profileId;

    public long categoryId;
    public float weight;
    public int color;
    public String text;
    public List<String> columns;
    public float valueFrom = 0;
    public float valueTo = 0;

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
