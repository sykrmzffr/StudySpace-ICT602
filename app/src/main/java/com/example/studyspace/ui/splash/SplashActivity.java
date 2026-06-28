package com.example.studyspace.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studyspace.ui.MainActivity;
import com.example.studyspace.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * SplashActivity — App entry point / loading screen.
 *
 * Tutorial Reference: T05 — Authentication
 * Checks if a user is already logged in (Firebase persists sessions).
 * - If logged in → go to MainActivity (Dashboard)
 * - If not logged in → go to LoginActivity
 *
 * The splash screen auto-redirects after 2 seconds.
 */
public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We don't inflate a layout — the splash background is set via theme
        // This keeps the splash instant with no layout inflation delay

        // T05 — Check current Firebase auth state
        auth = FirebaseAuth.getInstance();

        // Wait 2 seconds then redirect based on auth state
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (auth.getCurrentUser() != null) {
                    // User is already logged in — go to dashboard
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                } else {
                    // No session — show login screen
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
                finish(); // Remove splash from back stack
            }
        }, 2000);
    }
}
