package com.example.studyspace.ui.maps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.studyspace.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.graphics.Color;

/**
 * MapsActivity — Google Maps Navigation
 * 
 * Tutorial Reference: T08 — Google Maps
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private String mode;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        enableMyLocation();
                    } else {
                        Toast.makeText(MapsActivity.this, "Location permission is required for navigation.", Toast.LENGTH_LONG).show();
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        mode = getIntent().getStringExtra("MODE");
        if (mode == null) mode = "ROUTING";

        if ("OVERVIEW".equals(mode)) {
            loadAllRooms();
        } else {
            setupRoutingMode();
        }

        checkLocationPermission();
    }

    private void loadAllRooms() {
        FirebaseFirestore.getInstance().collection("rooms").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    LatLng firstRoom = null;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double lat = doc.getDouble("lat");
                        Double lng = doc.getDouble("lng");
                        String name = doc.getString("name");
                        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                            LatLng pos = new LatLng(lat, lng);
                            map.addMarker(new MarkerOptions().position(pos).title(name));
                            if (firstRoom == null) firstRoom = pos;
                        }
                    }
                    if (firstRoom != null) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(firstRoom, 14f));
                    }
                });
    }

    private void setupRoutingMode() {
        String roomName = getIntent().getStringExtra("EXTRA_ROOM_NAME");
        double lat = getIntent().getDoubleExtra("EXTRA_ROOM_LAT", 3.0738);
        double lng = getIntent().getDoubleExtra("EXTRA_ROOM_LNG", 101.5183);

        LatLng destination = new LatLng(lat, lng);
        map.addMarker(new MarkerOptions().position(destination).title(roomName));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(destination, 16f));
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
            
            map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(location.getLatitude(), location.getLongitude()), 16f));
                        } else {
                            Toast.makeText(MapsActivity.this, "Waiting for GPS location...", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return true;
                }
            });

            if ("ROUTING".equals(mode)) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        double destLat = getIntent().getDoubleExtra("EXTRA_ROOM_LAT", 3.0738);
                        double destLng = getIntent().getDoubleExtra("EXTRA_ROOM_LNG", 101.5183);
                        LatLng destination = new LatLng(destLat, destLng);
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                        // Draw polyline directly between user and destination
                        map.addPolyline(new PolylineOptions()
                                .add(userLocation, destination)
                                .width(8f)
                                .color(Color.BLUE));
                    }
                });
            }
        }
    }
}
