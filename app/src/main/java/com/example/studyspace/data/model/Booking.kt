package com.example.studyspace.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

/**
 * Booking Data Model
 * Tutorial Reference: T03 — Local Database (Room)
 * Tutorial Reference: T04 — Online Database (Firestore)
 *
 * This class acts as both a Firestore document representation
 * and a Room Database entity (table) for offline caching.
 */
@Entity(tableName = "bookings_table")
data class Booking(
    @DocumentId
    @PrimaryKey
    var id: String = "",
    var userId: String = "",
    var roomId: String = "",
    var roomName: String = "",
    var date: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var status: String = "Pending",
    var checkedIn: Boolean = false
)
