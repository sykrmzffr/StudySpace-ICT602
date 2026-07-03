package com.example.studyspace.service;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.studyspace.data.local.BookingDao;
import com.example.studyspace.data.local.StudyDatabase;
import com.example.studyspace.data.model.Booking;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting background sync of pending bookings");
        
        StudyDatabase db = StudyDatabase.getDatabase(getApplicationContext());
        BookingDao dao = db.bookingDao();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        List<Booking> pendingBookings = dao.getPendingBookings();

        if (pendingBookings == null || pendingBookings.isEmpty()) {
            Log.d(TAG, "No pending bookings to sync");
            return Result.success();
        }

        try {
            for (Booking booking : pendingBookings) {
                // Ensure we don't upload the Room-specific flag
                booking.setPendingSync(false);
                
                // Upload to Firestore synchronously using Tasks.await
                Tasks.await(firestore.collection("bookings")
                        .document(booking.getBookingId())
                        .set(booking));
                        
                // Update local DB to mark as synced
                dao.insertBooking(booking);
                Log.d(TAG, "Successfully synced booking: " + booking.getBookingId());
            }
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error syncing bookings", e);
            return Result.retry();
        }
    }
}
