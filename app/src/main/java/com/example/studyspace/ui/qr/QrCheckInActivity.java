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

import java.util.List;
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

        firestore.collection("bookings").document(bookingId)
                .update("status", "Checked In", "checkedIn", true)
                .addOnSuccessListener(unused -> runOnUiThread(() -> {
                    Toast.makeText(QrCheckInActivity.this, "Successfully Checked In!", Toast.LENGTH_LONG).show();
                    finish();
                }))
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    Toast.makeText(QrCheckInActivity.this, "Failed to check in via network.", Toast.LENGTH_SHORT).show();
                    isScanning = true;
                }));
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
