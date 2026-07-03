package com.example.studyspace.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.studyspace.R;
import com.example.studyspace.databinding.ActivityMainBinding;
import com.example.studyspace.utils.NetworkMonitor;
import android.view.View;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

/**
 * MainActivity — Main Application Shell
 *
 * Tutorial Reference: T02 — Layout (Bottom Navigation, Fragment hosting)
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // T02 — Inflate layout with View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up Navigation Component
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            // Link Bottom Navigation with NavController
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }

        // Initialize and observe NetworkMonitor
        NetworkMonitor.getInstance(this).startMonitoring();
        NetworkMonitor.getInstance(this).getNetworkState().observe(this, isConnected -> {
            if (isConnected) {
                binding.tvOfflineBanner.setVisibility(View.GONE);
            } else {
                binding.tvOfflineBanner.setVisibility(View.VISIBLE);
            }
        });

        requestNotificationPermission();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(), isGranted -> {
                            // Handle permission result if needed
                        }
                );
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NetworkMonitor.getInstance(this).stopMonitoring();
    }
}
