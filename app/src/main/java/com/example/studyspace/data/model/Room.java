package com.example.studyspace.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

/**
 * Room Data Model
 * Tutorial Reference: T04 — Online Database (Firestore)
 *
 * Represents a study room in the Firebase Firestore database.
 * The @DocumentId annotation automatically populates the id field
 * with the Firestore document ID when fetching data.
 */
public class Room {
    @DocumentId
    private String id = "";
    private String name = "";
    private String location = "";
    private int capacity = 0;
    private int floor = 0;
    private double lat = 0.0;
    private double lng = 0.0;
    private String qrCode = "";
    private boolean isAvailable = true;
    private String imageUrl = "";

    // Transient field for local UI distance sorting (not saved to Firestore)
    @Exclude
    public float distanceToUser = 0f;

    // Required empty constructor for Firestore
    public Room() {}

    public Room(String id, String name, String location, int capacity, int floor, double lat, double lng, String qrCode, boolean isAvailable, String imageUrl) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.floor = floor;
        this.lat = lat;
        this.lng = lng;
        this.qrCode = qrCode;
        this.isAvailable = isAvailable;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean isAvailable) { this.isAvailable = isAvailable; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
