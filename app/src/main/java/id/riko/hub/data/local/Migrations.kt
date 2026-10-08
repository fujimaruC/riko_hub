package id.riko.hub.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN estimatedMinutes INTEGER")
            db.execSQL("ALTER TABLE tasks ADD COLUMN recurrence TEXT NOT NULL DEFAULT 'NONE'")
            db.execSQL("ALTER TABLE tasks ADD COLUMN checklistJson TEXT NOT NULL DEFAULT '[]'")
            db.execSQL("ALTER TABLE tasks ADD COLUMN reminderMinutesBefore INTEGER")
            db.execSQL("ALTER TABLE tasks ADD COLUMN generatedFromTaskId INTEGER")
            db.execSQL("ALTER TABLE tasks ADD COLUMN generationPristine INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceAnchorAt INTEGER")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tasks_generatedFromTaskId ON tasks(generatedFromTaskId)")
        }
    }
}
