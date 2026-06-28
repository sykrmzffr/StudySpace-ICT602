package com.example.studyspace.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * StudySpaceFCMService — Stub placeholder for Firebase Cloud Messaging.
 * Will be built in Phase 4 with FCM push notifications (T06).
 *
 * Tutorial Reference: T06 — Notifications
 */
class StudySpaceFCMService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // TODO Phase 4: Handle incoming FCM push notifications (T06)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO Phase 4: Save new FCM token to Firestore for this user (T06)
    }
}
