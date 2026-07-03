package com.example.studyspace.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

/**
 * Booking Data Model
 * Tutorial Reference: T03 — Local Database (Room)
 * Tutorial Reference: T04 — Online Database (Firestore)
 *
 * This class acts as both a Firestore document representation
 * and a Room Database entity (table) for offline caching.
 */
@Entity(tableName = "bookings_table")
public class Booking {
    @DocumentId
    @PrimaryKey
    @NonNull
    private String bookingId = "";
    
    private String userId = "";
    private String roomId = "";
    private String roomName = "";
    private String roomLocation = "";
    private String date = "";
    private String startTime = "";
    private String endTime = "";
    private String status = "Pending";
    private boolean checkedIn = false;
    private boolean isPendingSync = false;

    @Ignore
    private Timestamp createdAt;

    public Booking() {}

    public Booking(@NonNull String bookingId, String userId, String roomId, String roomName, String roomLocation, String date, String startTime, String endTime, String status, boolean checkedIn) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomLocation = roomLocation;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.checkedIn = checkedIn;
    }

    @NonNull
    public String getBookingId() { return bookingId; }
    public void setBookingId(@NonNull String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    
    public String getRoomLocation() { return roomLocation; }
    public void setRoomLocation(String roomLocation) { this.roomLocation = roomLocation; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isCheckedIn() { return checkedIn; }
    public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Exclude
    public boolean isPendingSync() { return isPendingSync; }
    @Exclude
    public void setPendingSync(boolean pendingSync) { isPendingSync = pendingSync; }
}
