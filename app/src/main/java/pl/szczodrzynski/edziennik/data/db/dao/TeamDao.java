/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import pl.szczodrzynski.edziennik.data.db.entity.Team;

@Dao
public abstract class TeamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void add(Team team);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void addAll(List<Team> teamList);

    @Query("DELETE FROM teams WHERE profileId = :profileId")
    public abstract void clear(int profileId);

    @Query("SELECT * FROM teams WHERE profileId = :profileId AND teamId = :id")
    public abstract LiveData<Team> getById(int profileId, long id);

    @Query("SELECT * FROM teams WHERE profileId = :profileId AND teamId = :id")
    public abstract Team getByIdNow(int profileId, long id);

    @Query("SELECT * FROM teams WHERE profileId = :profileId ORDER BY teamType, teamName ASC")
    public abstract LiveData<List<Team>> getAll(int profileId);

    @Query("SELECT * FROM teams WHERE profileId = :profileId ORDER BY teamType, teamName ASC")
    public abstract List<Team> getAllNow(int profileId);

    @Query("SELECT * FROM teams ORDER BY teamType, teamName ASC")
    public abstract List<Team> getAllNow();

    @Query("SELECT * FROM teams WHERE profileId = :profileId AND teamType = 1")
    public abstract LiveData<Team> getClass(int profileId);

    @Query("SELECT * FROM teams WHERE profileId = :profileId AND teamType = 1")
    public abstract Team getClassNow(int profileId);


    @Query("SELECT * FROM teams WHERE profileId = :profileId AND teamCode = :code")
    public abstract Team getByCodeNow(int profileId, String code);

    @Query("SELECT teamCode FROM teams WHERE profileId = :profileId ORDER BY teamType, teamCode ASC")
    public abstract List<String> getAllCodesNow(int profileId);

    @Query("SELECT teamCode FROM teams WHERE profileId = :profileId AND teamId = :teamId")
    public abstract String getCodeByIdNow(int profileId, long teamId);
}
