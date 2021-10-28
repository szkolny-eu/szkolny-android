/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-16.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration97 : Migration(96, 97) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // notes
        database.execSQL("""CREATE TABLE notes (
            profileId INTEGER NOT NULL,
            noteId INTEGER NOT NULL,
            noteOwnerType TEXT,
            noteOwnerId INTEGER,
            noteReplacesOriginal INTEGER NOT NULL,
            noteTopic TEXT,
            noteBody TEXT NOT NULL,
            noteColor INTEGER,
            noteSharedBy TEXT,
            noteSharedByName TEXT,
            addedDate INTEGER NOT NULL,
            PRIMARY KEY(noteId)
        );""")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_profileId_noteOwnerType_noteOwnerId ON notes (profileId, noteOwnerType, noteOwnerId);")
    }
}
