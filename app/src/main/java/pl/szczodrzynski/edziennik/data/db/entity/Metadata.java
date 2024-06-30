/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import pl.szczodrzynski.edziennik.data.enums.MetadataType;

@Entity(tableName = "metadata",
        indices = {@Index(value = {"profileId", "thingType", "thingId"}, unique = true)}
)
public class Metadata {
    public int profileId;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "metadataId")
    public int id;

    @NonNull
    public MetadataType thingType;
    public long thingId;

    public boolean seen;
    public boolean notified;

    public Metadata(int profileId, @NonNull MetadataType thingType, long thingId, boolean seen, boolean notified) {
        this.profileId = profileId;
        this.thingType = thingType;
        this.thingId = thingId;
        this.seen = seen;
        this.notified = notified;
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "profileId=" + profileId +
                ", id=" + id +
                ", thingType=" + thingType +
                ", thingId=" + thingId +
                ", seen=" + seen +
                ", notified=" + notified +
                '}';
    }
}
