package pl.szczodrzynski.edziennik.data.db.modules.teams;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

import java.util.List;

@Entity(tableName = "teams",
        primaryKeys = {"profileId", "teamId"})
public class Team {
    public static final int TYPE_CLASS = 1;
    public static final int TYPE_VIRTUAL = 2;

    public int profileId;

    @ColumnInfo(name = "teamId")
    public long id;

    @ColumnInfo(name = "teamType")
    public int type;
    @ColumnInfo(name = "teamName")
    public String name;
    @ColumnInfo(name = "teamCode")
    public String code;

    @ColumnInfo(name = "teamTeacherId")
    public long teacherId;

    public Team(int profileId, long id, String name, int type, String code, long teacherId) {
        this.profileId = profileId;
        this.id = id;
        this.name = name;
        this.type = type;
        this.code = code;
        this.teacherId = teacherId;
    }

    public static Team getById(List<Team> teams, long id)
    {
        for (Team team: teams) {
            if (team.id == id)
            {
                return team;
            }
        }
        return null;
    }
    public static Team getByName(List<Team> teams, String name)
    {
        for (Team team: teams) {
            if (team.name.equals(name))
            {
                return team;
            }
        }
        return null;
    }
}

