package com.example.studyspace.ui.detail

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studyspace.R
import com.example.studyspace.data.model.Room
import com.example.studyspace.databinding.ActivityRoomDetailBinding
import com.example.studyspace.utils.NetworkUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * RoomDetailActivity — Shows room info and allows booking.
 * 
 * Tutorial References:
 * - T01 (Input/Output): Getting time slots, showing booking status.
 * - T04 (Online Database): Fetching Room data, Writing Booking data to Firestore.
 * - T06 (Notification): (Hook) Where we'd schedule a reminder before booking.
 */
class RoomDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoomDetailBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var roomId: String? = null
    private var selectedRoom: Room? = null
    private val calendar = Calendar.getInstance()

    // Sample time slots for booking
    private val timeSlots = listOf(
        "08:00 AM - 10:00 AM",
        "10:00 AM - 12:00 PM",
        "12:00 PM - 02:00 PM",
        "02:00 PM - 04:00 PM",
        "04:00 PM - 06:00 PM"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        roomId = intent.getStringExtra("EXTRA_ROOM_ID")
        if (roomId == null) {
            Toast.makeText(this, "Error: Room ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        fetchRoomDetails()
    }

    private fun setupUI() {
        // Back Button
        binding.btnBack.setOnClickListener { finish() }

        // Time Slot Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeSlots)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTimeSlot.adapter = adapter

        // Date Picker (T01)
        updateDateText()
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        // Book Button
        binding.btnBook.setOnClickListener {
            performBooking()
        }

        // Navigate Button (Phase 5)
        binding.btnNavigate.setOnClickListener {
            val mapIntent = android.content.Intent(this, com.example.studyspace.ui.maps.MapsActivity::class.java)
            mapIntent.putExtra("EXTRA_ROOM_NAME", selectedRoom?.name ?: "Room")
            startActivity(mapIntent)
        }
    }

    /**
     * T04 - Fetch the selected room from Firestore
     */
    private fun fetchRoomDetails() {
        binding.progressBar.visibility = View.VISIBLE
        
        firestore.collection("rooms").document(roomId!!)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                if (document.exists()) {
                    selectedRoom = document.toObject(Room::class.java)
                    populateRoomData()
                } else {
                    showSnackbar("Room not found.")
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                showSnackbar("Failed to load room details.")
            }
    }

    private fun populateRoomData() {
        selectedRoom?.let { room ->
            binding.tvRoomName.text = room.name
            binding.tvRoomLocation.text = room.location
            binding.tvCapacity.text = "${room.capacity} People"
            binding.tvFloor.text = "Floor ${room.floor}"

            if (room.isAvailable) {
                binding.chipStatus.text = getString(R.string.status_available)
                binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
            } else {
                binding.chipStatus.text = "Unavailable"
                binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light)
                binding.btnBook.isEnabled = false
            }
        }
    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateText()
        }

        DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    private fun updateDateText() {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = sdf.format(calendar.time)
    }

    /**
     * T04 - Create a booking document in Firestore
     */
    private fun performBooking() {
        if (!NetworkUtils.isConnected(this)) {
            showSnackbar(getString(R.string.error_no_internet))
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            showSnackbar("Please log in to book a room.")
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnBook.isEnabled = false

        val selectedTime = binding.spinnerTimeSlot.selectedItem.toString()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateStr = sdf.format(calendar.time)
        
        // Parse start and end time from the slot string for data model
        val timeParts = selectedTime.split(" - ")
        val startTime = timeParts.getOrNull(0) ?: selectedTime
        val endTime = timeParts.getOrNull(1) ?: ""

        val bookingData = hashMapOf(
            "userId" to currentUser.uid,
            "roomId" to roomId,
            "roomName" to (selectedRoom?.name ?: "Unknown Room"),
            "date" to selectedDateStr,
            "startTime" to startTime,
            "endTime" to endTime,
            "status" to "Confirmed", // In a real app this might be pending
            "checkedIn" to false,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        // Write to "bookings" collection
        firestore.collection("bookings")
            .add(bookingData)
            .addOnSuccessListener { documentRef ->
                binding.progressBar.visibility = View.GONE
                showSnackbar("Room booked successfully!")
                
                // T06 - Schedule Notification Hook
                // In a production app with a backend, a Firebase Cloud Function would listen to 
                // this new document and schedule an FCM message. For a client-only project, 
                // we could use WorkManager here.
                
                binding.root.postDelayed({ finish() }, 1500)
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnBook.isEnabled = true
                showSnackbar("Failed to book room: ${e.message}")
            }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
