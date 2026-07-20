package com.example.studyspace.ui.qr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.studyspace.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * QrCheckInActivity — Scans QR codes to check into rooms.
 * 
 * Tutorial Reference: T09 — Camera and ML Kit
 */
public class QrCheckInActivity extends AppCompatActivity {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private boolean isScanning = true;
    private String bookingId = null;
    private FirebaseFirestore firestore;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(QrCheckInActivity.this, "Camera permission is required.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_check_in);

        firestore = FirebaseFirestore.getInstance();
        bookingId = getIntent().getStringExtra("EXTRA_BOOKING_ID");

        findViewById(R.id.fabClose).setOnClickListener(v -> finish());

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                PreviewView viewFinder = findViewById(R.id.viewFinder);
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalyzer.setAnalyzer(cameraExecutor, new QrAnalyzer(new QrAnalyzer.OnQrDetectedListener() {
                    @Override
                    public void onQrDetected(String barcodeValue) {
                        if (isScanning) {
                            isScanning = false;
                            handleSuccessfulScan(barcodeValue);
                        }
                    }
                }));

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(QrCheckInActivity.this, cameraSelector, preview, imageAnalyzer);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("QrCheckIn", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void handleSuccessfulScan(String qrData) {
        if (bookingId == null) {
            runOnUiThread(() -> {
                Toast.makeText(QrCheckInActivity.this, "Invalid booking. Returning...", Toast.LENGTH_SHORT).show();
                finish();
            });
            return;
        }

        runOnUiThread(() -> findViewById(R.id.loadingOverlay).setVisibility(android.view.View.VISIBLE));

        firestore.collection("bookings").document(bookingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String roomId = documentSnapshot.getString("roomId");
                        String date = documentSnapshot.getString("date"); // e.g. 2023-10-15
                        String startTime = documentSnapshot.getString("startTime"); // e.g. 10:00 AM
                        String endTime = documentSnapshot.getString("endTime"); // e.g. 12:00 PM

                        if (roomId != null) {
                            // Fetch the room to compare its qrCode field with the scanned QR
                            firestore.collection("rooms").document(roomId).get()
                                    .addOnSuccessListener(roomDoc -> {
                                        String expectedQr = roomDoc.getString("qrCode");
                                        if (expectedQr != null && expectedQr.equals(qrData)) {
                                            // Validate Time Window
                                            if (date != null && startTime != null && endTime != null) {
                                                try {
                                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
                                                    Date start = sdf.parse(date + " " + startTime);
                                                    Date end = sdf.parse(date + " " + endTime);
                                                    long now = System.currentTimeMillis();
                                                    
                                                    long windowStart = start.getTime() - (15 * 60 * 1000);
                                                    long windowEnd = end.getTime();
                                                    
                                                    if (now < windowStart) {
                                                        hideLoadingAndFinish("Too early. Check in opens 15 mins before booking.");
                                                        return;
                                                    } else if (now > windowEnd) {
                                                        hideLoadingAndFinish("Booking has expired.");
                                                        return;
                                                    }
                                                } catch (ParseException e) {
                                                    Log.e("QrCheckIn", "Error parsing booking time", e);
                                                }
                                            }

                                            // QR matches and time is valid, proceed to check-in
                                            firestore.collection("bookings").document(bookingId)
                                                    .update("status", "Checked In", "checkedIn", true)
                                                    .addOnSuccessListener(unused -> hideLoadingAndFinish("Successfully Checked In!"))
                                                    .addOnFailureListener(e -> hideLoadingAndFinish("Failed to check in via network."));
                                        } else {
                                            hideLoadingAndFinish("Invalid QR Code for this room.");
                                        }
                                    })
                                    .addOnFailureListener(e -> hideLoadingAndFinish("Could not verify room."));
                        } else {
                            hideLoadingAndFinish("Booking not found.");
                        }
                    } else {
                        hideLoadingAndFinish("Booking not found.");
                    }
                })
                .addOnFailureListener(e -> hideLoadingAndFinish("Failed to fetch booking."));
    }

    private void hideLoadingAndFinish(String message) {
        runOnUiThread(() -> {
            findViewById(R.id.loadingOverlay).setVisibility(android.view.View.GONE);
            Toast.makeText(QrCheckInActivity.this, message, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    private static class QrAnalyzer implements ImageAnalysis.Analyzer {

        public interface OnQrDetectedListener {
            void onQrDetected(String value);
        }

        private final OnQrDetectedListener listener;
        private final BarcodeScanner scanner;

        QrAnalyzer(OnQrDetectedListener listener) {
            this.listener = listener;
            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build();
            scanner = BarcodeScanning.getClient(options);
        }

        @Override
        @ExperimentalGetImage
        public void analyze(@NonNull ImageProxy imageProxy) {
            if (imageProxy.getImage() != null) {
                InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

                scanner.process(image)
                        .addOnSuccessListener(barcodes -> {
                            for (Barcode barcode : barcodes) {
                                if (barcode.getRawValue() != null) {
                                    listener.onQrDetected(barcode.getRawValue());
                                }
                            }
                        })
                        .addOnFailureListener(e -> Log.e("QrCheckIn", "Barcode scanning failed", e))
                        .addOnCompleteListener(task -> imageProxy.close());
            } else {
                imageProxy.close();
            }
        }
    }
}
