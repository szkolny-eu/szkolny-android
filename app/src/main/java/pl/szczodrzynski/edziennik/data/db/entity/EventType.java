/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.entity;

import android.graphics.Color;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "eventTypes",
        primaryKeys = {"profileId", "eventType"})
public class EventType {
    public int profileId;

    @ColumnInfo(name = "eventType")
    public long id;

    @ColumnInfo(name = "eventTypeName")
    public String name;
    @ColumnInfo(name = "eventTypeColor")
    public int color;

    public EventType(int profileId, long id, String name, int color) {
        this.profileId = profileId;
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public EventType(int profileId, int id, String name, String color) {
        this(profileId, id, name, Color.parseColor(color));
    }
}
