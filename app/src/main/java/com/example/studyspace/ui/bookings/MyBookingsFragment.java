package com.example.studyspace.ui.bookings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.studyspace.data.local.BookingDao;
import com.example.studyspace.data.local.StudyDatabase;
import com.example.studyspace.data.model.Booking;
import com.example.studyspace.databinding.FragmentMyBookingsBinding;
import com.example.studyspace.ui.qr.QrCheckInActivity;
import com.example.studyspace.utils.NetworkUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.example.studyspace.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MyBookingsFragment — View upcoming/past bookings.
 * 
 * Tutorial Reference: T03 — Local Database (Room) - Cache bookings offline
 * Tutorial Reference: T04 — Online Database (Firestore) - Fetch user bookings
 * Tutorial Reference: T07 — Connectivity - Sync data when online
 */
public class MyBookingsFragment extends Fragment {

    private FragmentMyBookingsBinding binding;
    private BookingAdapter bookingAdapter;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private StudyDatabase database;
    private ExecutorService executorService;
    private boolean sortAscending = false;
    private List<Booking> currentBookings = new java.util.ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyBookingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        database = StudyDatabase.getDatabase(requireContext());
        executorService = Executors.newSingleThreadExecutor();

        setupRecyclerView();

        binding.swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                syncBookingsFromFirestore();
            }
        });

        binding.toolbar.inflateMenu(R.menu.menu_bookings);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_sort) {
                sortAscending = !sortAscending;
                item.setTitle(sortAscending ? "Sort Date Descending" : "Sort Date Ascending");
                updateListWithSorting();
                return true;
            }
            return false;
        });

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            database.bookingDao().getUserBookings(currentUser.getUid()).observe(getViewLifecycleOwner(), new Observer<List<Booking>>() {
                @Override
                public void onChanged(List<Booking> localBookings) {
                    currentBookings = new java.util.ArrayList<>(localBookings);
                    updateListWithSorting();
                    binding.tvEmptyState.setVisibility(localBookings.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });

            if (NetworkUtils.isConnected(requireContext())) {
                binding.swipeRefresh.setRefreshing(true);
                syncBookingsFromFirestore();
            } else {
                showSnackbar("Offline Mode: Showing cached bookings");
            }
        } else {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyState.setText("Please log in to view your bookings.");
        }
    }

    private void updateListWithSorting() {
        if (currentBookings == null) return;
        List<Booking> sorted = new java.util.ArrayList<>(currentBookings);
        if (sortAscending) {
            java.util.Collections.reverse(sorted);
        }
        bookingAdapter.updateData(sorted);
    }

    private void setupRecyclerView() {
        bookingAdapter = new BookingAdapter(new BookingAdapter.BookingClickListener() {
            @Override
            public void onCheckInClick(Booking booking) {
                Intent intent = new Intent(requireContext(), QrCheckInActivity.class);
                intent.putExtra("EXTRA_BOOKING_ID", booking.getBookingId());
                startActivity(intent);
            }

            @Override
            public void onCancelClick(Booking booking) {
                cancelBooking(booking);
            }
        });
        binding.recyclerViewBookings.setAdapter(bookingAdapter);
    }

    private void syncBookingsFromFirestore() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        if (!NetworkUtils.isConnected(requireContext())) {
            binding.swipeRefresh.setRefreshing(false);
            showSnackbar("No internet. Showing offline data.");
            return;
        }

        firestore.collection("bookings")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshot) {
                        binding.swipeRefresh.setRefreshing(false);
                        if (!snapshot.isEmpty()) {
                            final List<Booking> firestoreBookings = snapshot.toObjects(Booking.class);

                            executorService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    BookingDao dao = database.bookingDao();
                                    dao.insertAll(firestoreBookings);
                                    Log.d("MyBookings", "Successfully synced " + firestoreBookings.size() + " bookings to Room.");
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        binding.swipeRefresh.setRefreshing(false);
                        Log.e("MyBookings", "Error fetching from Firestore", e);
                        showSnackbar("Failed to sync bookings.");
                    }
                });
    }

    private void cancelBooking(Booking booking) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel your booking for " + booking.getRoomName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (!NetworkUtils.isConnected(requireContext())) {
                        showSnackbar("No internet connection.");
                        return;
                    }
                    
                    binding.swipeRefresh.setRefreshing(true);
                    firestore.collection("bookings").document(booking.getBookingId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                executorService.execute(() -> {
                                    database.bookingDao().deleteBooking(booking);
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            binding.swipeRefresh.setRefreshing(false);
                                            showSnackbar("Booking cancelled.");
                                        });
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                binding.swipeRefresh.setRefreshing(false);
                                showSnackbar("Failed to cancel booking: " + e.getMessage());
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null) {
            executorService.shutdown();
        }
        binding = null;
    }
}
