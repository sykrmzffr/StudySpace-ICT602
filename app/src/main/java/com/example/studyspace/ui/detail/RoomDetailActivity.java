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
                mapIntent.putExtra("EXTRA_ROOM_NAME", selectedRoom != null ? selectedRoom.getName() : "Room");
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

            if (selectedRoom.isAvailable()) {
                binding.chipStatus.setText(getString(R.string.status_available));
                binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
            } else {
                binding.chipStatus.setText("Unavailable");
                binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light);
                binding.btnBook.setEnabled(false);
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
    }

    private void performBooking() {
        if (!NetworkUtils.isConnected(this)) {
            showSnackbar(getString(R.string.error_no_internet));
            return;
        }

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

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("userId", currentUser.getUid());
        bookingData.put("roomId", roomId);
        bookingData.put("roomName", selectedRoom != null ? selectedRoom.getName() : "Unknown Room");
        bookingData.put("date", selectedDateStr);
        bookingData.put("startTime", startTime);
        bookingData.put("endTime", endTime);
        bookingData.put("status", "Confirmed");
        bookingData.put("checkedIn", false);
        bookingData.put("createdAt", Timestamp.now());

        firestore.collection("bookings")
                .add(bookingData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
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

    private void showSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }
}
