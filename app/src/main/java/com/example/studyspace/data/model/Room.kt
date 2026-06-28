package com.example.studyspace.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Room Data Model
 * Tutorial Reference: T04 — Online Database (Firestore)
 *
 * Represents a study room in the Firebase Firestore database.
 * The @DocumentId annotation automatically populates the id field
 * with the Firestore document ID when fetching data.
 */
data class Room(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val capacity: Int = 0,
    val floor: Int = 0,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val qrCode: String = "",
    val isAvailable: Boolean = true
)
