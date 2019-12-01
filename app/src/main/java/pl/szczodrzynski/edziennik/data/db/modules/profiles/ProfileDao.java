package pl.szczodrzynski.edziennik.data.db.modules.profiles;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.REGISTRATION_ENABLED;

@Dao
public interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(Profile profile);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAll(List<Profile> profileList);

    @Query("DELETE FROM profiles WHERE profileId = :profileId")
    void remove(int profileId);

    @Query("SELECT profiles.*, loginStores.loginStoreType, loginStores.loginStoreData FROM profiles LEFT JOIN loginStores ON profiles.loginStoreId = loginStores.loginStoreId WHERE profileId = :profileId")
    LiveData<ProfileFull> getById(int profileId);

    @Nullable
    @Query("SELECT profiles.*, loginStores.loginStoreType, loginStores.loginStoreData FROM profiles LEFT JOIN loginStores ON profiles.loginStoreId = loginStores.loginStoreId WHERE profileId = :profileId")
    ProfileFull getFullByIdNow(int profileId);

    @Query("SELECT*  FROM profiles WHERE profileId = :profileId")
    Profile getByIdNow(int profileId);

    @Query("SELECT * FROM profiles WHERE profileId >= 0 ORDER BY profileId")
    LiveData<List<Profile>> getAll();

    @Query("SELECT * FROM profiles WHERE profileId >= 0 ORDER BY profileId")
    List<Profile> getAllNow();

    @Query("SELECT profiles.*, loginStores.loginStoreType, loginStores.loginStoreData FROM profiles LEFT JOIN loginStores ON profiles.loginStoreId = loginStores.loginStoreId WHERE profileId >= 0 ORDER BY profileId")
    LiveData<List<ProfileFull>> getAllFull();

    @Query("SELECT profiles.*, loginStores.loginStoreType, loginStores.loginStoreData FROM profiles LEFT JOIN loginStores ON profiles.loginStoreId = loginStores.loginStoreId WHERE profileId >= 0 ORDER BY profileId")
    List<ProfileFull> getAllFullNow();

    @Query("SELECT profileId FROM profiles WHERE loginStoreId = :loginStoreId ORDER BY profileId")
    List<Integer> getIdsByLoginStoreIdNow(int loginStoreId);

    @Query("SELECT * FROM profiles WHERE syncEnabled = 1 AND archived = 0 AND profileId >= 0 ORDER BY profileId")
    List<Profile> getProfilesForSyncNow();

    @Query("SELECT profileId FROM profiles WHERE syncEnabled = 1 AND archived = 0 AND profileId >= 0 ORDER BY profileId")
    List<Integer> getIdsForSyncNow();

    @Query("SELECT profileId FROM profiles WHERE profileId >= 0 ORDER BY profileId")
    List<Integer> getIdsNow();

    @Query("SELECT profiles.*, loginStores.* FROM teams JOIN profiles USING(profileId) LEFT JOIN loginStores ON profiles.loginStoreId = loginStores.loginStoreId WHERE teamCode = :teamCode AND registration = "+ REGISTRATION_ENABLED +" AND enableSharedEvents = 1")
    List<ProfileFull> getByTeamCodeNowWithRegistration(String teamCode);

    @Query("SELECT profileId FROM profiles WHERE profileId >= 0 ORDER BY profileId ASC LIMIT 1")
    int getFirstId();

    @Query("SELECT profileId FROM profiles WHERE profileId >= 0 ORDER BY profileId DESC LIMIT 1")
    int getLastId();

    @Query("UPDATE profiles SET loginStoreId = :targetId WHERE loginStoreId = :sourceId")
    void changeStoreId(int sourceId, int targetId);

    @Query("UPDATE profiles SET currentSemester = :semester WHERE profileId = :profileId")
    void changeSemester(int profileId, int semester);
}

