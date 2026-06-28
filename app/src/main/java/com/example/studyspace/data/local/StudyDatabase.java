package com.example.studyspace.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.studyspace.data.model.Booking;

/**
 * StudyDatabase — Room Database Configuration
 * Tutorial Reference: T03 — Local Database (Room)
 *
 * This represents the local SQLite database for offline caching.
 */
@Database(entities = {Booking.class}, version = 2, exportSchema = false)
public abstract class StudyDatabase extends RoomDatabase {

    public abstract BookingDao bookingDao();

    private static volatile StudyDatabase INSTANCE;

    public static StudyDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (StudyDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            StudyDatabase.class,
                            "study_database"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
