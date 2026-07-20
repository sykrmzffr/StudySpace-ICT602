package com.example.studyspace.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.studyspace.data.model.Room;
import com.example.studyspace.databinding.FragmentHomeBinding;
import com.example.studyspace.ui.detail.RoomDetailActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import java.util.Collections;
import java.util.Comparator;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseFirestore firestore;
    private RoomAdapter roomAdapter;
    private List<Room> allRooms = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    sortRoomsByNearest();
                } else {
                    showSnackbar("Location permission denied. Cannot sort by nearest.");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupRecyclerView();
        setupSearch();
        setupButtons();
        fetchRooms();
    }

    private void setupButtons() {
        binding.fabMap.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.studyspace.ui.maps.MapsActivity.class);
            intent.putExtra("MODE", "OVERVIEW");
            startActivity(intent);
        });

        binding.btnSortNearest.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                sortRoomsByNearest();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });
    }

    private void sortRoomsByNearest() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        
        binding.progressBar.setVisibility(View.VISIBLE);
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            binding.progressBar.setVisibility(View.GONE);
            if (location != null) {
                for (Room room : allRooms) {
                    float[] results = new float[1];
                    Location.distanceBetween(location.getLatitude(), location.getLongitude(), room.getLat(), room.getLng(), results);
                    room.distanceToUser = results[0];
                }
                Collections.sort(allRooms, new Comparator<Room>() {
                    @Override
                    public int compare(Room r1, Room r2) {
                        return Float.compare(r1.distanceToUser, r2.distanceToUser);
                    }
                });
                filterRooms(binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "");
                showSnackbar("Sorted by nearest");
            } else {
                showSnackbar("Unable to get current location");
            }
        });
    }

    private void calculateDistancesForDisplay() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    for (Room room : allRooms) {
                        float[] results = new float[1];
                        Location.distanceBetween(location.getLatitude(), location.getLongitude(), room.getLat(), room.getLng(), results);
                        room.distanceToUser = results[0];
                    }
                    // Update UI without sorting
                    filterRooms(binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "");
                }
            });
        }
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRooms(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.chipAvailable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            filterRooms(binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "");
        });
        binding.chipSmall.setOnCheckedChangeListener((buttonView, isChecked) -> {
            filterRooms(binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "");
        });
        binding.chipLarge.setOnCheckedChangeListener((buttonView, isChecked) -> {
            filterRooms(binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "");
        });
    }

    private void filterRooms(String query) {
        if (allRooms == null) return;
        List<Room> filteredList = new ArrayList<>();
        
        boolean filterAvailable = binding.chipAvailable.isChecked();
        boolean filterSmall = binding.chipSmall.isChecked();
        boolean filterLarge = binding.chipLarge.isChecked();

        for (Room room : allRooms) {
            boolean matchesSearch = room.getName().toLowerCase().contains(query.toLowerCase()) ||
                    room.getLocation().toLowerCase().contains(query.toLowerCase());
                    
            boolean matchesAvailable = !filterAvailable || room.isAvailable();
            
            boolean passesCapacity = true;
            if (filterSmall || filterLarge) {
                passesCapacity = false;
                if (filterSmall && room.getCapacity() >= 1 && room.getCapacity() <= 4) passesCapacity = true;
                if (filterLarge && room.getCapacity() >= 5) passesCapacity = true;
            }

            if (matchesSearch && matchesAvailable && passesCapacity) {
                filteredList.add(room);
            }
        }
        roomAdapter.updateData(filteredList);
    }

    private void setupRecyclerView() {
        roomAdapter = new RoomAdapter(new ArrayList<>(), new RoomAdapter.OnRoomClickListener() {
            @Override
            public void onRoomClick(Room room) {
                Intent intent = new Intent(requireContext(), RoomDetailActivity.class);
                intent.putExtra("EXTRA_ROOM_ID", room.getId());
                startActivity(intent);
            }
        });
        binding.recyclerViewRooms.setAdapter(roomAdapter);
    }

    private void fetchRooms() {
        binding.progressBar.setVisibility(View.VISIBLE);

        firestore.collection("rooms")
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshot,
                                        @Nullable FirebaseFirestoreException exception) {
                        
                        binding.progressBar.setVisibility(View.GONE);

                        if (exception != null) {
                            Log.e("HomeFragment", "Error fetching rooms", exception);
                            showSnackbar("Failed to load rooms");
                            return;
                        }

                        if (snapshot != null && !snapshot.isEmpty()) {
                            List<Room> rawRooms = snapshot.toObjects(Room.class);
                            allRooms = new ArrayList<>();
                            List<String> seenNames = new ArrayList<>();
                            for (Room r : rawRooms) {
                                if (!seenNames.contains(r.getName())) {
                                    seenNames.add(r.getName());
                                    allRooms.add(r);
                                }
                            }
                            calculateDistancesForDisplay();
                            filterRooms(binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "");
                        } else {
                            seedDummyRooms();
                        }
                    }
                });
    }

    private void seedDummyRooms() {
        List<Room> dummyRooms = new ArrayList<>();
        // Using approximate coordinates for UiTM Shah Alam
        String defaultImg = "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=800&q=80";
        dummyRooms.add(new Room("", "Main Library 101", "Main Library, Floor 1", 4, 1, 3.0697, 101.5036, "LIBRARY101", true, defaultImg));
        dummyRooms.add(new Room("", "Engineering Hub A", "Engineering Building, Floor 2", 6, 2, 3.0685, 101.4988, "ENGA", true, "https://images.unsplash.com/photo-1577415124269-3111103a7e28?auto=format&fit=crop&w=800&q=80"));
        dummyRooms.add(new Room("", "Quiet Zone Beta", "Student Center, Floor 3", 1, 3, 3.0722, 101.5055, "BETA", true, "https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&w=800&q=80"));
        dummyRooms.add(new Room("", "Law Faculty Study Room", "Law Building, Ground Floor", 8, 0, 3.0705, 101.4991, "LAW100", true, "https://images.unsplash.com/photo-1521737852567-6949f3f9f2b5?auto=format&fit=crop&w=800&q=80"));
        dummyRooms.add(new Room("", "Business School Lounge", "Business Building, Floor 2", 10, 2, 3.0735, 101.5020, "BUS200", true, "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=800&q=80"));
        dummyRooms.add(new Room("", "Fellicia's Math Lab", "Science Tower, Floor 4", 3, 4, 3.0711, 101.5075, "MATH400", true, defaultImg));
        dummyRooms.add(new Room("", "Cempaka Study Pod", "Cempaka College, Block A", 2, 1, 3.0650, 101.5015, "CEMPAKA", true, defaultImg));

        WriteBatch batch = firestore.batch();
        for (Room room : dummyRooms) {
            DocumentReference docRef = firestore.collection("rooms").document();
            batch.set(docRef, room);
        }

        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                showSnackbar("Added sample rooms to database");
            }
        });
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
