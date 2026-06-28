package com.example.studyspace.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.studyspace.data.model.Room
import com.example.studyspace.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // T04 — Firestore instance
    private lateinit var firestore: FirebaseFirestore
    private lateinit var roomAdapter: RoomAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Setup RecyclerView (T02)
        setupRecyclerView()

        // Fetch Data from Firestore (T04)
        fetchRooms()
    }

    private fun setupRecyclerView() {
        roomAdapter = RoomAdapter(emptyList()) { room ->
            // Phase 3 — Navigate to Room Detail Screen
            val intent = android.content.Intent(requireContext(), com.example.studyspace.ui.detail.RoomDetailActivity::class.java).apply {
                putExtra("EXTRA_ROOM_ID", room.id)
            }
            startActivity(intent)
        }
        binding.recyclerViewRooms.adapter = roomAdapter
    }

    /**
     * T04 — Fetch list of study rooms from Firestore in real-time.
     */
    private fun fetchRooms() {
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("rooms")
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, exception ->
                binding.progressBar.visibility = View.GONE

                if (exception != null) {
                    Log.e("HomeFragment", "Error fetching rooms", exception)
                    showSnackbar("Failed to load rooms")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    // Convert Firestore documents to Room objects
                    val rooms = snapshot.toObjects(Room::class.java)
                    roomAdapter.updateData(rooms)
                } else {
                    // Database is empty. Let's seed it for the tutorial!
                    seedDummyRooms()
                }
            }
    }

    /**
     * Helper function to seed the database with initial rooms if empty.
     * T04 — Writing data to Firestore.
     */
    private fun seedDummyRooms() {
        val dummyRooms = listOf(
            Room(name = "Main Library 101", location = "Main Library, Floor 1", capacity = 4, floor = 1, isAvailable = true),
            Room(name = "Main Library 102", location = "Main Library, Floor 1", capacity = 2, floor = 1, isAvailable = false),
            Room(name = "Engineering Hub A", location = "Engineering Building, Floor 2", capacity = 6, floor = 2, isAvailable = true),
            Room(name = "Quiet Zone Beta", location = "Student Center, Floor 3", capacity = 1, floor = 3, isAvailable = true)
        )

        val batch = firestore.batch()
        dummyRooms.forEach { room ->
            val docRef = firestore.collection("rooms").document()
            batch.set(docRef, room)
        }

        batch.commit().addOnSuccessListener {
            showSnackbar("Added sample rooms to database")
        }
    }

    private fun showSnackbar(message: String) {
        view?.let { Snackbar.make(it, message, Snackbar.LENGTH_LONG).show() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

