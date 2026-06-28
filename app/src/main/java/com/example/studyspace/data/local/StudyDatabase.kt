package com.example.studyspace.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.studyspace.data.model.Booking

/**
 * StudyDatabase — Room Database Configuration
 * Tutorial Reference: T03 — Local Database (Room)
 *
 * This represents the local SQLite database for offline caching.
 */
@Database(entities = [Booking::class], version = 1, exportSchema = false)
abstract class StudyDatabase : RoomDatabase() {

    abstract fun bookingDao(): BookingDao

    companion object {
        @Volatile
        private var INSTANCE: StudyDatabase? = null

        fun getDatabase(context: Context): StudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "study_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
