/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.szczodrzynski.edziennik.data.db.entity.Profile

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(profile: Profile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(profileList: List<Profile>)

    @Query("DELETE FROM profiles WHERE profileId = :profileId")
    fun remove(profileId: Int)

    @Query("SELECT profiles.* FROM profiles WHERE profileId = :profileId")
    fun getById(profileId: Int): LiveData<Profile?>

    @Query("SELECT * FROM profiles WHERE profileId = :profileId")
    fun getByIdNow(profileId: Int): Profile?

    @get:Query("SELECT * FROM profiles WHERE profileId >= 0 ORDER BY profileId")
    val all: LiveData<List<Profile>>

    @get:Query("SELECT * FROM profiles WHERE profileId >= 0 ORDER BY profileId")
    val allNow: List<Profile>

    @get:Query("SELECT COUNT(profileId) FROM profiles WHERE profileId >= 0")
    val count: Int

    @Query("SELECT profileId FROM profiles WHERE loginStoreId = :loginStoreId ORDER BY profileId")
    fun getIdsByLoginStoreIdNow(loginStoreId: Int): List<Int>

    @get:Query("SELECT * FROM profiles WHERE syncEnabled = 1 AND archived = 0 AND profileId >= 0 ORDER BY profileId")
    val profilesForFirebaseNow: List<Profile>

    @get:Query("SELECT profileId FROM profiles WHERE syncEnabled = 1 AND archived = 0 AND profileId >= 0 ORDER BY profileId")
    val idsForSyncNow: List<Int>

    @get:Query("SELECT profileId FROM profiles WHERE profileId >= 0 ORDER BY profileId")
    val idsNow: List<Int>

    @Query("SELECT profiles.* FROM teams JOIN profiles USING(profileId) WHERE teamCode = :teamCode AND registration = " + Profile.REGISTRATION_ENABLED + " AND enableSharedEvents = 1")
    fun getByTeamCodeNowWithRegistration(teamCode: String?): List<Profile>

    @get:Query("SELECT profileId FROM profiles WHERE profileId > 0 ORDER BY profileId ASC LIMIT 1")
    val firstId: Int?

    @get:Query("SELECT profileId FROM profiles WHERE profileId > 0 ORDER BY profileId DESC LIMIT 1")
    val lastId: Int?

    @Query("UPDATE profiles SET empty = 0")
    fun setAllNotEmpty()
}
