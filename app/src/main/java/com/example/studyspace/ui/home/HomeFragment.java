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

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseFirestore firestore;
    private RoomAdapter roomAdapter;
    private List<Room> allRooms = new ArrayList<>();

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

        setupRecyclerView();
        setupSearch();
        fetchRooms();
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
    }

    private void filterRooms(String query) {
        if (query.trim().isEmpty()) {
            roomAdapter.updateData(allRooms);
            return;
        }

        String lowerQuery = query.toLowerCase();
        List<Room> filteredRooms = new ArrayList<>();
        for (Room room : allRooms) {
            if (room.getName() != null && room.getName().toLowerCase().contains(lowerQuery) ||
                room.getLocation() != null && room.getLocation().toLowerCase().contains(lowerQuery)) {
                filteredRooms.add(room);
            }
        }
        roomAdapter.updateData(filteredRooms);
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
                            allRooms = snapshot.toObjects(Room.class);
                            filterRooms(binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "");
                        } else {
                            seedDummyRooms();
                        }
                    }
                });
    }

    private void seedDummyRooms() {
        List<Room> dummyRooms = new ArrayList<>();
        dummyRooms.add(new Room("", "Main Library 101", "Main Library, Floor 1", 4, 1, 0.0, 0.0, "", true));
        dummyRooms.add(new Room("", "Main Library 102", "Main Library, Floor 1", 2, 1, 0.0, 0.0, "", false));
        dummyRooms.add(new Room("", "Engineering Hub A", "Engineering Building, Floor 2", 6, 2, 0.0, 0.0, "", true));
        dummyRooms.add(new Room("", "Quiet Zone Beta", "Student Center, Floor 3", 1, 3, 0.0, 0.0, "", true));

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
