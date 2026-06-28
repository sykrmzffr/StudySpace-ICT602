package com.example.studyspace.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.studyspace.data.model.Booking;

import java.util.List;

/**
 * Booking Data Access Object (DAO)
 * Tutorial Reference: T03 — Local Database (Room)
 *
 * Provides methods for the rest of the app to interact with data in the bookings_table.
 */
@Dao
public interface BookingDao {

    // T03 - Insert a booking (replace if it already exists to handle sync updates)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBooking(Booking booking);

    // T03 - Insert multiple bookings (batch sync from Firestore)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Booking> bookings);

    // T03 - Get all bookings for a specific user, ordered by date
    @Query("SELECT * FROM bookings_table WHERE userId = :userId ORDER BY date DESC, startTime DESC")
    LiveData<List<Booking>> getUserBookings(String userId);

    // T03 - Clear old cache
    @Query("DELETE FROM bookings_table WHERE userId = :userId")
    void clearUserBookings(String userId);
}
