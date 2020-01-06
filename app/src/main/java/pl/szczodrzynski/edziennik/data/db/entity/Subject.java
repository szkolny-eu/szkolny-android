/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

import java.util.List;

import pl.szczodrzynski.edziennik.ExtensionsKt;

@Entity(tableName = "subjects",
        primaryKeys = {"profileId", "subjectId"})
public class Subject {
    public int profileId;

    @ColumnInfo(name = "subjectId")
    public long id;

    @ColumnInfo(name = "subjectLongName")
    public String longName;
    @ColumnInfo(name = "subjectShortName")
    public String shortName;
    @ColumnInfo(name = "subjectColor")
    public int color;

    public Subject(int profileId, long id, String longName, String shortName) {
        this.profileId = profileId;
        this.id = id;
        this.longName = longName;
        this.shortName = shortName;
        this.color = ExtensionsKt.colorFromName(longName);
    }

    @Override
    public String toString() {
        return "Subject{" +
                "profileId=" + profileId +
                ", id=" + id +
                ", longName='" + longName + '\'' +
                ", shortName='" + shortName + '\'' +
                ", color=" + color +
                '}';
    }

    // USED IN IUCZNIOWIE
    public static Subject getById(List<Subject> subjectList, long id)
    {
        for (Subject subject: subjectList) {
            if (subject.id == id)
            {
                return subject;
            }
        }
        return null;
    }
    public static Subject getByName(List<Subject> subjectList, String name)
    {
        for (Subject subject: subjectList) {
            if (subject.longName.equals(name))
            {
                return subject;
            }
        }
        return null;
    }
}
