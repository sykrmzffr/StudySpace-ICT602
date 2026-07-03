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

    // Sample time slots for booking
    private final List<String> timeSlots = Arrays.asList(
            "08:00 AM - 10:00 AM",
            "10:00 AM - 12:00 PM",
            "12:00 PM - 02:00 PM",
            "02:00 PM - 04:00 PM",
            "04:00 PM - 06:00 PM"
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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timeSlots);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTimeSlot.setAdapter(adapter);

        updateDateText();
        binding.btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

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

        binding.spinnerTimeSlot.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                checkCapacity();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
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

            if (!selectedRoom.isAvailable()) {
                binding.chipStatus.setText("Unavailable");
                binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light);
                binding.btnBook.setEnabled(false);
            }
            
            checkCapacity();
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
        checkCapacity();
    }

    private void checkCapacity() {
        if (selectedRoom == null) return;
        
        if (!selectedRoom.isAvailable()) {
            binding.chipStatus.setText("Unavailable");
            binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light);
            binding.btnBook.setEnabled(false);
            binding.btnBook.setText("UNAVAILABLE");
            return;
        }

        String selectedTime = binding.spinnerTimeSlot.getSelectedItem().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDateStr = sdf.format(calendar.getTime());
        
        String[] timeParts = selectedTime.split(" - ");
        String startTime = timeParts.length > 0 ? timeParts[0] : selectedTime;

        binding.btnBook.setEnabled(false);
        binding.btnBook.setText("Checking availability...");

        firestore.collection("bookings")
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("date", selectedDateStr)
                .whereEqualTo("startTime", startTime)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!selectedRoom.isAvailable()) return; // Re-check just in case

                    int bookedCount = snapshot.size();
                    if (bookedCount >= selectedRoom.getCapacity()) {
                        binding.chipStatus.setText("Fully Booked");
                        binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light);
                        binding.btnBook.setEnabled(false);
                        binding.btnBook.setText("FULLY BOOKED");
                    } else {
                        binding.chipStatus.setText("Available (" + (selectedRoom.getCapacity() - bookedCount) + " left)");
                        binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
                        binding.btnBook.setEnabled(true);
                        binding.btnBook.setText("BOOK ROOM");
                    }
                })
                .addOnFailureListener(e -> {
                    showSnackbar("Failed to check availability");
                    binding.btnBook.setEnabled(true);
                    binding.btnBook.setText("BOOK ROOM");
                });
    }

    private void performBooking() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showSnackbar("Please log in to book a room.");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnBook.setEnabled(false);

        String selectedTime = binding.spinnerTimeSlot.getSelectedItem().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDateStr = sdf.format(calendar.getTime());
        
        String[] timeParts = selectedTime.split(" - ");
        String startTime = timeParts.length > 0 ? timeParts[0] : selectedTime;
        String endTime = timeParts.length > 1 ? timeParts[1] : "";

        String roomName = selectedRoom != null ? selectedRoom.getName() : "Unknown Room";
        String roomLocation = selectedRoom != null ? selectedRoom.getLocation() : "";

        if (!NetworkUtils.isConnected(this)) {
            // Offline Mode: Queue booking locally
            String generatedId = UUID.randomUUID().toString();
            Booking offlineBooking = new Booking(generatedId, currentUser.getUid(), roomId, roomName, roomLocation, selectedDateStr, startTime, endTime, "Pending", false);
            offlineBooking.setPendingSync(true);
            offlineBooking.setCreatedAt(Timestamp.now());

            Executors.newSingleThreadExecutor().execute(() -> {
                StudyDatabase.getDatabase(getApplicationContext()).bookingDao().insertBooking(offlineBooking);
                
                // Enqueue SyncWorker
                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();
                OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                        .setConstraints(constraints)
                        .build();
                WorkManager.getInstance(getApplicationContext()).enqueue(syncRequest);

                runOnUiThread(() -> {
                    scheduleNotification(roomName, selectedDateStr, startTime, endTime);
                    binding.progressBar.setVisibility(View.GONE);
                    showSnackbar("Offline: Booking queued for sync.");
                    binding.getRoot().postDelayed(this::finish, 1500);
                });
            });
            return;
        }

        // Online Mode: Normal booking
        Booking newBooking = new Booking("", currentUser.getUid(), roomId, roomName, roomLocation, selectedDateStr, startTime, endTime, "Confirmed", false);
        newBooking.setCreatedAt(Timestamp.now());

        firestore.collection("bookings")
                .add(newBooking)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        newBooking.setBookingId(documentReference.getId());
                        Executors.newSingleThreadExecutor().execute(() -> {
                            StudyDatabase.getDatabase(getApplicationContext()).bookingDao().insertBooking(newBooking);
                        });

                        scheduleNotification(roomName, selectedDateStr, startTime, endTime);
                        binding.progressBar.setVisibility(View.GONE);
                        showSnackbar("Room booked successfully!");
                        
                        binding.getRoot().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 1500);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnBook.setEnabled(true);
                        showSnackbar("Failed to book room: " + e.getMessage());
                    }
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
