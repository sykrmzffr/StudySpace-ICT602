package com.example.studyspace.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.studyspace.R;
import com.example.studyspace.ui.MainActivity;

public class NotificationReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "STUDYSPACE_REMINDERS";

    @Override
    public void onReceive(Context context, Intent intent) {
        String roomName = intent.getStringExtra("ROOM_NAME");
        if (roomName == null) roomName = "your room";

        String type = intent.getStringExtra("NOTIFICATION_TYPE");
        if (type == null) type = "START";

        createNotificationChannel(context);

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);

        String title = "Upcoming Booking";
        String contentText = "Reminder: You have a booking for " + roomName + " starting in 15 minutes!";

        if ("CHECKOUT".equals(type)) {
            title = "Check-out Reminder";
            contentText = "Your booking for " + roomName + " ends in 5 minutes. Please pack up soon!";
        } else if ("CONFIRM".equals(type)) {
            title = "Booking Confirmed!";
            contentText = "You have successfully booked " + roomName + ".";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Booking Reminders";
            String description = "Reminders for upcoming study room bookings";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
