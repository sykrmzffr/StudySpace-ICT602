package com.example.studyspace.ui.bookings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.studyspace.data.local.StudyDatabase
import com.example.studyspace.data.model.Booking
import com.example.studyspace.databinding.FragmentMyBookingsBinding
import com.example.studyspace.utils.NetworkUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MyBookingsFragment — View upcoming/past bookings.
 * 
 * Tutorial Reference: T03 — Local Database (Room) - Cache bookings offline
 * Tutorial Reference: T04 — Online Database (Firestore) - Fetch user bookings
 * Tutorial Reference: T07 — Connectivity - Sync data when online
 */
class MyBookingsFragment : Fragment() {

    private var _binding: FragmentMyBookingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var bookingAdapter: BookingAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var database: StudyDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        database = StudyDatabase.getDatabase(requireContext())

        setupRecyclerView()
        
        // Setup pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener {
            syncBookingsFromFirestore()
        }

        // T03: Observe Local Room Database
        // This LiveData automatically updates the UI whenever the local cache changes!
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.bookingDao().getUserBookings(currentUser.uid).observe(viewLifecycleOwner) { localBookings ->
                bookingAdapter.updateData(localBookings)
                binding.tvEmptyState.visibility = if (localBookings.isEmpty()) View.VISIBLE else View.GONE
            }
            
            // T07: Initial sync if online
            if (NetworkUtils.isConnected(requireContext())) {
                binding.swipeRefresh.isRefreshing = true
                syncBookingsFromFirestore()
            } else {
                showSnackbar("Offline Mode: Showing cached bookings")
            }
        } else {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = "Please log in to view your bookings."
        }
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingAdapter { booking ->
            // Phase 5: Start QrCheckInActivity
            val intent = android.content.Intent(requireContext(), com.example.studyspace.ui.qr.QrCheckInActivity::class.java)
            intent.putExtra("EXTRA_BOOKING_ID", booking.id)
            startActivity(intent)
        }
        binding.recyclerViewBookings.adapter = bookingAdapter
    }

    /**
     * T04, T07: Fetch from Firestore and save to Room (Local DB)
     */
    private fun syncBookingsFromFirestore() {
        val currentUser = auth.currentUser ?: return

        if (!NetworkUtils.isConnected(requireContext())) {
            binding.swipeRefresh.isRefreshing = false
            showSnackbar("No internet. Showing offline data.")
            return
        }

        firestore.collection("bookings")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.swipeRefresh.isRefreshing = false
                if (!snapshot.isEmpty) {
                    val firestoreBookings = snapshot.toObjects(Booking::class.java)
                    
                    // T03: Save fetched data to Local Room Database
                    lifecycleScope.launch(Dispatchers.IO) {
                        val dao = database.bookingDao()
                        // Optional: Clear old data first to avoid stale deleted records
                        // dao.clearUserBookings(currentUser.uid)
                        
                        dao.insertAll(firestoreBookings)
                        
                        withContext(Dispatchers.Main) {
                            Log.d("MyBookings", "Successfully synced ${firestoreBookings.size} bookings to Room.")
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                binding.swipeRefresh.isRefreshing = false
                Log.e("MyBookings", "Error fetching from Firestore", e)
                showSnackbar("Failed to sync bookings.")
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
