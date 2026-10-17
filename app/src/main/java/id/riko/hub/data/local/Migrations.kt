package id.riko.hub.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS space_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                syncId TEXT NOT NULL,
                kind TEXT NOT NULL,
                body TEXT NOT NULL,
                journalDate TEXT,
                projectTaskSyncId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER
            )""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_space_notes_syncId ON space_notes(syncId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_space_notes_kind_journalDate_deletedAt ON space_notes(kind, journalDate, deletedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_space_notes_kind_deletedAt ON space_notes(kind, deletedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_space_notes_projectTaskSyncId ON space_notes(projectTaskSyncId)")
        }
    }

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

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS courses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                syncId TEXT NOT NULL,
                title TEXT NOT NULL,
                code TEXT,
                instructor TEXT,
                academicPeriod TEXT,
                color TEXT,
                icon TEXT,
                sortOrder INTEGER NOT NULL,
                archivedAt INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_courses_syncId ON courses(syncId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_courses_archivedAt_sortOrder ON courses(archivedAt, sortOrder)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS course_weekly_slots (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                courseId INTEGER NOT NULL,
                dayOfWeek INTEGER NOT NULL,
                startMinute INTEGER NOT NULL,
                endMinute INTEGER NOT NULL,
                location TEXT,
                FOREIGN KEY(courseId) REFERENCES courses(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )""")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_course_weekly_slots_courseId ON course_weekly_slots(courseId)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS academic_assignments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                syncId TEXT NOT NULL,
                taskId INTEGER NOT NULL,
                courseId INTEGER,
                state TEXT NOT NULL,
                submittedAt INTEGER,
                submissionReference TEXT,
                originalDeadlineAt INTEGER,
                originalHasDeadlineTime INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(taskId) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(courseId) REFERENCES courses(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_academic_assignments_syncId ON academic_assignments(syncId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_academic_assignments_taskId ON academic_assignments(taskId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_academic_assignments_courseId ON academic_assignments(courseId)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS academic_deadline_changes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                syncId TEXT NOT NULL,
                assignmentId INTEGER NOT NULL,
                previousDeadlineAt INTEGER,
                previousHasDeadlineTime INTEGER NOT NULL,
                newDeadlineAt INTEGER,
                newHasDeadlineTime INTEGER NOT NULL,
                changedAt INTEGER NOT NULL,
                FOREIGN KEY(assignmentId) REFERENCES academic_assignments(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_academic_deadline_changes_syncId ON academic_deadline_changes(syncId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_academic_deadline_changes_assignmentId ON academic_deadline_changes(assignmentId)")
        }
    }
}
