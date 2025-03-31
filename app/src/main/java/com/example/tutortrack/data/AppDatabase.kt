package com.example.tutortrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tutortrack.data.dao.ClassTypeDao
import com.example.tutortrack.data.dao.SessionDao
import com.example.tutortrack.data.dao.StudentDao
import com.example.tutortrack.data.model.ClassType
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.data.model.Student

// Migration from version 1 to 2: Add paidDate column to sessions table
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE sessions ADD COLUMN paidDate INTEGER DEFAULT NULL")
    }
}

// Migration from version 2 to 3: Add class_types table and update sessions
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create class_types table
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS class_types (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "studentId INTEGER NOT NULL, " +
                "name TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "hourlyRate REAL NOT NULL, " +
                "FOREIGN KEY (studentId) REFERENCES students (id) ON DELETE CASCADE)"
        )
        
        // Create a default class type for each student
        database.execSQL(
            "INSERT INTO class_types (studentId, name, description, hourlyRate) " +
                "SELECT id, 'Default', 'Default class type', hourlyRate FROM students"
        )
        
        // Add classTypeId to sessions table with default values referring to the created class types
        database.execSQL("ALTER TABLE sessions ADD COLUMN classTypeId INTEGER DEFAULT 0")
        database.execSQL(
            "UPDATE sessions SET classTypeId = (" +
                "SELECT class_types.id FROM class_types WHERE class_types.studentId = sessions.studentId LIMIT 1)"
        )
    }
}

// Migration from version 3 to 4: Add parentName and parentContact columns to students table
// and handle removing the email field by recreating the table
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create a new temporary table with the updated schema
        database.execSQL(
            "CREATE TABLE students_new (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "phone TEXT NOT NULL, " +
                "grade TEXT NOT NULL, " +
                "parentName TEXT NOT NULL DEFAULT '', " +
                "parentContact TEXT NOT NULL DEFAULT '', " +
                "notes TEXT NOT NULL DEFAULT '')"
        )
        
        // Copy data from the old table to the new table (excluding email)
        database.execSQL(
            "INSERT INTO students_new (id, name, phone, grade, notes) " +
                "SELECT id, name, phone, grade, notes FROM students"
        )
        
        // Drop the old table
        database.execSQL("DROP TABLE students")
        
        // Rename the new table to the old table name
        database.execSQL("ALTER TABLE students_new RENAME TO students")
    }
}

// Migration from version 4 to 5: Remove description field from class_types table
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create a new temporary table without the description field
        database.execSQL(
            "CREATE TABLE class_types_new (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "studentId INTEGER NOT NULL, " +
                "name TEXT NOT NULL, " +
                "hourlyRate REAL NOT NULL, " +
                "FOREIGN KEY (studentId) REFERENCES students (id) ON DELETE CASCADE)"
        )
        
        // Copy data from the old table to the new table (excluding description)
        database.execSQL(
            "INSERT INTO class_types_new (id, studentId, name, hourlyRate) " +
                "SELECT id, studentId, name, hourlyRate FROM class_types"
        )
        
        // Drop the old table
        database.execSQL("DROP TABLE class_types")
        
        // Rename the new table to the old table name
        database.execSQL("ALTER TABLE class_types_new RENAME TO class_types")
    }
}

@Database(entities = [Student::class, Session::class, ClassType::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun sessionDao(): SessionDao
    abstract fun classTypeDao(): ClassTypeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tutor_track_database"
                )
                .fallbackToDestructiveMigration()
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 