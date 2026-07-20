package com.example.studyspace.ui.detail;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studyspace.R;
import com.example.studyspace.data.model.Room;
import com.example.studyspace.databinding.ActivityRoomDetailBinding;
import com.example.studyspace.ui.home.RoomAdapter;
import com.example.studyspace.ui.maps.MapsActivity;
import com.example.studyspace.utils.NetworkUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.studyspace.data.local.StudyDatabase;
import com.example.studyspace.data.model.Booking;
import com.example.studyspace.service.NotificationReceiver;
import com.example.studyspace.service.SyncWorker;
import java.util.UUID;
import java.util.concurrent.Executors;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RoomDetailActivity — Shows room info and allows booking.
 * 
 * Tutorial References:
 * - T01 (Input/Output): Getting time slots, showing booking status.
 * - T04 (Online Database): Fetching Room data, Writing Booking data to Firestore.
 * - T06 (Notification): (Hook) Where we'd schedule a reminder before booking.
 */
public class RoomDetailActivity extends AppCompatActivity {

    private ActivityRoomDetailBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private String roomId = null;
    private Room selectedRoom = null;
    private final Calendar calendar = Calendar.getInstance();

    private final List<String> timeSlots = Arrays.asList(
            "08:00 AM - 09:00 AM",
            "09:00 AM - 10:00 AM",
            "10:00 AM - 11:00 AM",
            "11:00 AM - 12:00 PM",
            "12:00 PM - 01:00 PM",
            "01:00 PM - 02:00 PM",
            "02:00 PM - 03:00 PM",
            "03:00 PM - 04:00 PM",
            "04:00 PM - 05:00 PM",
            "05:00 PM - 06:00 PM"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoomDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        roomId = getIntent().getStringExtra("EXTRA_ROOM_ID");
        if (roomId == null) {
            Toast.makeText(this, "Error: Room ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        fetchRoomDetails();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        updateDateText();
        binding.btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        // Setup for multiple CheckBox selection handled in loadTimeSlots

        binding.btnBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performBooking();
            }
        });

        binding.btnNavigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(RoomDetailActivity.this, MapsActivity.class);
                mapIntent.putExtra("MODE", "ROUTING");
                mapIntent.putExtra("EXTRA_ROOM_NAME", selectedRoom != null ? selectedRoom.getName() : "Room");
                if (selectedRoom != null) {
                    mapIntent.putExtra("EXTRA_ROOM_LAT", selectedRoom.getLat());
                    mapIntent.putExtra("EXTRA_ROOM_LNG", selectedRoom.getLng());
                }
                startActivity(mapIntent);
            }
        });
    }

    private void fetchRoomDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        firestore.collection("rooms").document(roomId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot document) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (document.exists()) {
                            selectedRoom = document.toObject(Room.class);
                            populateRoomData();
                        } else {
                            showSnackbar("Room not found.");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        binding.progressBar.setVisibility(View.GONE);
                        showSnackbar("Failed to load room details.");
                    }
                });
    }

    private void populateRoomData() {
        if (selectedRoom != null) {
            binding.tvRoomName.setText(selectedRoom.getName());
            binding.tvRoomLocation.setText(selectedRoom.getLocation());
            binding.tvCapacity.setText(selectedRoom.getCapacity() + " People");
            binding.tvFloor.setText("Floor " + selectedRoom.getFloor());

            // Set the room-specific header image
            binding.ivDetailRoomImage.setImageResource(
                    RoomAdapter.getRoomDrawable(selectedRoom.getName()));

            if (!selectedRoom.isAvailable()) {
                binding.chipStatus.setText("Unavailable");
                binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light);
                binding.btnBook.setEnabled(false);
            } else {
                loadTimeSlots();
            }
        }
    }

    private void showDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateText();
            }
        };

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void updateDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        binding.tvSelectedDate.setText(sdf.format(calendar.getTime()));
        if (selectedRoom != null && selectedRoom.isAvailable()) {
            loadTimeSlots();
        }
    }

    private void loadTimeSlots() {
        if (selectedRoom == null) return;
        
        binding.layoutTimeSlots.removeAllViews();
        binding.btnBook.setEnabled(false);
        binding.btnBook.setText("Checking availability...");

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDateStr = sdfDate.format(calendar.getTime());

        firestore.collection("bookings")
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("date", selectedDateStr)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!selectedRoom.isAvailable()) return;
                    
                    binding.btnBook.setText("SELECT A TIME");

                    Calendar currentCal = Calendar.getInstance();
                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

                    for (String slot : timeSlots) {
                        String[] parts = slot.split(" - ");
                        String startTimeStr = parts[0];
                        
                        // Count bookings for this slot
                        int bookedCount = 0;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            if (startTimeStr.equals(doc.getString("startTime"))) {
                                bookedCount++;
                            }
                        }

                        int slotsLeft = selectedRoom.getCapacity() - bookedCount;
                        
                        // Check if time has passed
                        boolean isPast = false;
                        try {
                            Calendar slotCal = (Calendar) calendar.clone();
                            java.util.Date parsedTime = timeFormat.parse(startTimeStr);
                            if (parsedTime != null) {
                                Calendar timeCal = Calendar.getInstance();
                                timeCal.setTime(parsedTime);
                                slotCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                                slotCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                                if (slotCal.before(currentCal)) {
                                    isPast = true;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        android.widget.CheckBox cb = new android.widget.CheckBox(this);
                        cb.setId(View.generateViewId());
                        cb.setTag(slot);
                        cb.setPadding(0, 16, 0, 16);

                        if (isPast) {
                            cb.setText(slot + " (Time passed)");
                            cb.setEnabled(false);
                            cb.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
                        } else if (slotsLeft <= 0) {
                            cb.setText(slot + " (Fully booked)");
                            cb.setEnabled(false);
                            cb.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
                        } else {
                            cb.setText(slot + " (" + slotsLeft + " left)");
                            cb.setEnabled(true);
                            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                int checkedCount = getCheckedCount();
                                binding.btnBook.setEnabled(checkedCount > 0);
                                if (checkedCount > 0) {
                                    binding.btnBook.setText("BOOK ROOM (" + checkedCount + " SLOTS)");
                                    binding.chipStatus.setText(checkedCount + " Slots Selected");
                                    binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
                                } else {
                                    binding.btnBook.setText("SELECT A TIME");
                                    binding.chipStatus.setText("Available");
                                    binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_blue_light);
                                }
                            });
                        }

                        binding.layoutTimeSlots.addView(cb);
                    }
                })
                .addOnFailureListener(e -> {
                    showSnackbar("Failed to load time slots");
                    binding.btnBook.setText("ERROR");
                });
    }

    private int getCheckedCount() {
        int count = 0;
        for (int i = 0; i < binding.layoutTimeSlots.getChildCount(); i++) {
            View v = binding.layoutTimeSlots.getChildAt(i);
            if (v instanceof android.widget.CheckBox && ((android.widget.CheckBox) v).isChecked()) {
                count++;
            }
        }
        return count;
    }

    private void performBooking() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showSnackbar("Please log in to book a room.");
            return;
        }

        int checkedCount = getCheckedCount();
        if (checkedCount == 0) {
            showSnackbar("Please select at least one time slot.");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnBook.setEnabled(false);

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDateStr = sdfDate.format(calendar.getTime());
        String roomName = selectedRoom != null ? selectedRoom.getName() : "Unknown Room";
        String roomLocation = selectedRoom != null ? selectedRoom.getLocation() : "";
        
        java.util.List<Booking> newBookings = new java.util.ArrayList<>();
        
        for (int i = 0; i < binding.layoutTimeSlots.getChildCount(); i++) {
            View v = binding.layoutTimeSlots.getChildAt(i);
            if (v instanceof android.widget.CheckBox && ((android.widget.CheckBox) v).isChecked()) {
                String selectedSlot = (String) v.getTag();
                String[] parts = selectedSlot.split(" - ");
                String startTimeStr = parts[0];
                String endTimeStr = parts[1];
                
                String generatedId = UUID.randomUUID().toString();
                Booking b = new Booking(generatedId, currentUser.getUid(), roomId, roomName, roomLocation, selectedDateStr, startTimeStr, endTimeStr, "Pending", false);
                b.setCreatedAt(Timestamp.now());
                newBookings.add(b);
            }
        }

        if (!NetworkUtils.isConnected(this)) {
            // Offline Mode: Queue bookings locally
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                com.example.studyspace.data.local.StudyDatabase db = com.example.studyspace.data.local.StudyDatabase.getDatabase(getApplicationContext());
                for (Booking b : newBookings) {
                    b.setPendingSync(true);
                    db.bookingDao().insertBooking(b);
                }
                
                // Enqueue SyncWorker
                androidx.work.Constraints constraints = new androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build();
                androidx.work.OneTimeWorkRequest syncRequest = new androidx.work.OneTimeWorkRequest.Builder(com.example.studyspace.service.SyncWorker.class)
                        .setConstraints(constraints)
                        .build();
                androidx.work.WorkManager.getInstance(getApplicationContext()).enqueue(syncRequest);

                runOnUiThread(() -> {
                    for (Booking b : newBookings) {
                        scheduleNotification(roomName, selectedDateStr, b.getStartTime(), b.getEndTime());
                    }
                    binding.progressBar.setVisibility(View.GONE);
                    showSnackbar("Offline: " + checkedCount + " booking(s) queued for sync.");
                    binding.getRoot().postDelayed(this::finish, 1500);
                });
            });
            return;
        }

        // Online Mode: Normal booking using WriteBatch
        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
        
        for (Booking b : newBookings) {
            b.setStatus("Confirmed"); // Set as confirmed when online
            DocumentReference docRef = firestore.collection("bookings").document();
            b.setBookingId(docRef.getId());
            batch.set(docRef, b);
        }
        
        batch.commit().addOnSuccessListener(aVoid -> {
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                com.example.studyspace.data.local.StudyDatabase db = com.example.studyspace.data.local.StudyDatabase.getDatabase(getApplicationContext());
                for (Booking b : newBookings) {
                    db.bookingDao().insertBooking(b);
                }
            });
            
            for (Booking b : newBookings) {
                scheduleNotification(roomName, selectedDateStr, b.getStartTime(), b.getEndTime());
            }
            binding.progressBar.setVisibility(View.GONE);
            showSnackbar(checkedCount + " room slot(s) booked successfully!");
            
            binding.getRoot().postDelayed(this::finish, 1500);
        }).addOnFailureListener(e -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnBook.setEnabled(true);
            showSnackbar("Failed to book room: " + e.getMessage());
        });
    }

    private void scheduleNotification(String roomName, String dateStr, String startTime, String endTime) {
        try {
            // Immediate Confirmation Notification
            Intent confirmIntent = new Intent(this, NotificationReceiver.class);
            confirmIntent.putExtra("ROOM_NAME", roomName);
            confirmIntent.putExtra("NOTIFICATION_TYPE", "CONFIRM");
            sendBroadcast(confirmIntent);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
            
            // Start Reminder (15 mins before)
            java.util.Date startDate = sdf.parse(dateStr + " " + startTime);
            if (startDate != null) {
                long startTriggerTime = startDate.getTime() - (15 * 60 * 1000);
                if (startTriggerTime > System.currentTimeMillis()) {
                    setAlarm(startTriggerTime, roomName, "START", 1);
                }
            }

            // End Reminder (5 mins before checkout)
            java.util.Date endDate = sdf.parse(dateStr + " " + endTime);
            if (endDate != null) {
                long endTriggerTime = endDate.getTime() - (5 * 60 * 1000);
                if (endTriggerTime > System.currentTimeMillis()) {
                    setAlarm(endTriggerTime, roomName, "CHECKOUT", 2);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setAlarm(long triggerTime, String roomName, String type, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("ROOM_NAME", roomName);
        intent.putExtra("NOTIFICATION_TYPE", type);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) System.currentTimeMillis() + requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                if (alarmManager != null) alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } else if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }
}
