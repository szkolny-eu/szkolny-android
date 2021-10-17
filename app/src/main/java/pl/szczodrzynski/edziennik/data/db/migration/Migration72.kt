/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pl.szczodrzynski.edziennik.ext.crc32

class Migration72 : Migration(71, 72) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE loginStores RENAME to _loginStores;")
        database.execSQL("""CREATE TABLE loginStores(
                loginStoreId INTEGER NOT NULL,
                loginStoreType INTEGER NOT NULL,
                loginStoreMode INTEGER NOT NULL,
                loginStoreData TEXT NOT NULL,
                PRIMARY KEY(loginStoreId))""")

        database.execSQL("""INSERT INTO loginStores
                (loginStoreId, loginStoreType, loginStoreMode, loginStoreData)
                SELECT loginStoreId, loginStoreType, loginStoreMode, loginStoreData
                FROM _loginStores""")

        database.execSQL("DROP TABLE _loginStores;")
        database.execSQL("ALTER TABLE profiles RENAME TO _profiles_old;")

        database.execSQL("""CREATE TABLE profiles (
                profileId INTEGER NOT NULL, name TEXT NOT NULL, subname TEXT, image TEXT DEFAULT NULL,
                studentNameLong TEXT NOT NULL, studentNameShort TEXT NOT NULL, accountName TEXT,
                studentData TEXT NOT NULL, empty INTEGER NOT NULL DEFAULT 1, archived INTEGER NOT NULL DEFAULT 0,
                syncEnabled INTEGER NOT NULL DEFAULT 1, enableSharedEvents INTEGER NOT NULL DEFAULT 1, registration INTEGER NOT NULL DEFAULT 0,
                userCode TEXT NOT NULL DEFAULT '', studentNumber INTEGER NOT NULL DEFAULT -1, studentClassName TEXT DEFAULT NULL,
                studentSchoolYearStart INTEGER NOT NULL, dateSemester1Start TEXT NOT NULL, dateSemester2Start TEXT NOT NULL,
                dateYearEnd TEXT NOT NULL, disabledNotifications TEXT DEFAULT NULL, lastReceiversSync INTEGER NOT NULL DEFAULT 0,
                loginStoreId INTEGER NOT NULL, loginStoreType INTEGER NOT NULL, PRIMARY KEY(profileId))""")

        database.execSQL("""INSERT INTO profiles (profileId, name, subname, image, studentNameLong, studentNameShort, accountName,
                userCode, studentData, empty, archived, syncEnabled, enableSharedEvents, registration, studentNumber, studentSchoolYearStart,
                dateSemester1Start, dateSemester2Start, dateYearEnd, lastReceiversSync, loginStoreId, loginStoreType)
                SELECT profileId, name, subname, image, studentNameLong, studentNameShort, accountNameLong, '' AS userCode, studentData,
                empty, archived, syncEnabled, enableSharedEvents, registration, studentNumber, SUBSTR(dateSemester1Start, 0, 5) AS studentSchoolYearStart,
                dateSemester1Start, dateSemester2Start, dateYearEnd, lastReceiversSync, _profiles_old.loginStoreId, loginStoreType FROM _profiles_old
                JOIN loginStores ON loginStores.loginStoreId = _profiles_old.loginStoreId WHERE profileId >= 0""")

        database.execSQL("DROP TABLE _profiles_old;")

        migrateUserCodes(database)
    }

    private fun migrateMobidziennik(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS _userCodes;")
        database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, serverName TEXT, username TEXT, studentId TEXT);")
        database.execSQL("DELETE FROM _userCodes;")
        database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 1;")
        database.execSQL("UPDATE _userCodes SET serverName = SUBSTR(loginData, instr(loginData, '\"serverName\":\"')+14);")
        database.execSQL("UPDATE _userCodes SET serverName = SUBSTR(serverName, 0, instr(serverName, '\",')+instr(serverName, '\"}')-(instr(serverName, '\"}')*min(instr(serverName, '\",'), 1)));")
        database.execSQL("UPDATE _userCodes SET username = SUBSTR(loginData, instr(loginData, '\"username\":\"')+12);")
        database.execSQL("UPDATE _userCodes SET username = SUBSTR(username, 0, instr(username, '\",')+instr(username, '\"}')-(instr(username, '\"}')*min(instr(username, '\",'), 1)));")
        database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentData, instr(studentData, '\"studentId\":')+12);")
        database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentId, 0, instr(studentId, ',')+instr(studentId, '}')-(instr(studentId, '}')*min(instr(studentId, ','), 1)));")
        database.execSQL("UPDATE _userCodes SET userCode = serverName||\":\"||username||\":\"||studentId;")
        database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);")
    }

    private fun migrateLibrus(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS _userCodes;")
        database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, schoolName TEXT, accountLogin TEXT);")
        database.execSQL("DELETE FROM _userCodes;")
        database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 2;")
        database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(studentData, instr(studentData, '\"schoolName\":\"')+14);")
        database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(schoolName, 0, instr(schoolName, '\",')+instr(schoolName, '\"}')-(instr(schoolName, '\"}')*min(instr(schoolName, '\",'), 1)));")
        database.execSQL("UPDATE _userCodes SET accountLogin = SUBSTR(studentData, instr(studentData, '\"accountLogin\":\"')+16);")
        database.execSQL("UPDATE _userCodes SET accountLogin = SUBSTR(accountLogin, 0, instr(accountLogin, '\",')+instr(accountLogin, '\"}')-(instr(accountLogin, '\"}')*min(instr(accountLogin, '\",'), 1)));")
        database.execSQL("UPDATE _userCodes SET userCode = schoolName||\":\"||accountLogin;")
        database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);")
    }

    private fun migrateIUczniowie(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS _userCodes;")
        database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, schoolName TEXT, username TEXT, registerId TEXT);")
        database.execSQL("DELETE FROM _userCodes;")
        database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 3;")
        database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(loginData, instr(loginData, '\"schoolName\":\"')+14);")
        database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(schoolName, 0, instr(schoolName, '\",')+instr(schoolName, '\"}')-(instr(schoolName, '\"}')*min(instr(schoolName, '\",'), 1)));")
        database.execSQL("UPDATE _userCodes SET username = SUBSTR(loginData, instr(loginData, '\"username\":\"')+12);")
        database.execSQL("UPDATE _userCodes SET username = SUBSTR(username, 0, instr(username, '\",')+instr(username, '\"}')-(instr(username, '\"}')*min(instr(username, '\",'), 1)));")
        database.execSQL("UPDATE _userCodes SET registerId = SUBSTR(studentData, instr(studentData, '\"registerId\":')+13);")
        database.execSQL("UPDATE _userCodes SET registerId = SUBSTR(registerId, 0, instr(registerId, ',')+instr(registerId, '}')-(instr(registerId, '}')*min(instr(registerId, ','), 1)));")
        database.execSQL("UPDATE _userCodes SET userCode = schoolName||\":\"||username||\":\"||registerId;")
        database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);")
    }

    private fun migrateVulcan(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS _userCodes;")
        database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, schoolName TEXT, studentId TEXT);")
        database.execSQL("DELETE FROM _userCodes;")
        database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 4;")
        database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(studentData, instr(studentData, '\"schoolName\":\"')+14);")
        database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(schoolName, 0, instr(schoolName, '\",')+instr(schoolName, '\"}')-(instr(schoolName, '\"}')*min(instr(schoolName, '\",'), 1)));")
        database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentData, instr(studentData, '\"studentId\":')+12);")
        database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentId, 0, instr(studentId, ',')+instr(studentId, '}')-(instr(studentId, '}')*min(instr(studentId, ','), 1)));")
        database.execSQL("UPDATE _userCodes SET userCode = schoolName||\":\"||studentId;")
        database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);")
    }

    private fun migrateEdudziennik(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS _userCodes;")
        database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, schoolName TEXT, email TEXT, studentId TEXT);")
        database.execSQL("DELETE FROM _userCodes;")
        database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 5;")
        database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(studentData, instr(studentData, '\"schoolName\":\"')+14);")
        database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(schoolName, 0, instr(schoolName, '\",')+instr(schoolName, '\"}')-(instr(schoolName, '\"}')*min(instr(schoolName, '\",'), 1)));")
        database.execSQL("UPDATE _userCodes SET email = SUBSTR(loginData, instr(loginData, '\"email\":\"')+9);")
        database.execSQL("UPDATE _userCodes SET email = SUBSTR(email, 0, instr(email, '\",')+instr(email, '\"}')-(instr(email, '\"}')*min(instr(email, '\",'), 1)));")
        database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentData, instr(studentData, '\"studentId\":\"')+13);")
        database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentId, 0, instr(studentId, '\",')+instr(studentId, '\"}')-(instr(studentId, '\"}')*min(instr(studentId, '\",'), 1)));")
        database.query("SELECT profileId, studentId FROM _userCodes;").use { cursor ->
            while (cursor.moveToNext()) {
                val profileId = cursor.getInt(0)
                val crc = cursor.getString(1).crc32()
                database.execSQL("UPDATE _userCodes SET studentId = $crc WHERE profileId = $profileId")
            }
        }
        database.execSQL("UPDATE _userCodes SET userCode = schoolName||\":\"||email||\":\"||studentId;")
        database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);")
        database.execSQL("DROP TABLE _userCodes;")
    }

    private fun migrateUserCodes(database: SupportSQLiteDatabase) {
        migrateMobidziennik(database)
        migrateLibrus(database)
        migrateVulcan(database)
        migrateIUczniowie(database)
        migrateEdudziennik(database)
    }
}
