package com.example.studyspace.service;

import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * StudySpaceFCMService — Stub placeholder for Firebase Cloud Messaging.
 * Will be built in Phase 4 with FCM push notifications (T06).
 *
 * Tutorial Reference: T06 — Notifications
 */
public class StudySpaceFCMService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        // TODO Phase 4: Handle incoming FCM push notifications (T06)
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // TODO Phase 4: Save new FCM token to Firestore for this user (T06)
    }
}
