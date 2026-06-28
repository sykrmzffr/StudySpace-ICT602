package com.example.studyspace.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.studyspace.R;
import com.example.studyspace.databinding.ActivityMainBinding;

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
    }
}
