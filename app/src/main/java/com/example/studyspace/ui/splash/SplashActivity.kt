package com.example.studyspace.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.studyspace.ui.MainActivity
import com.example.studyspace.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

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
class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // We don't inflate a layout — the splash background is set via theme
        // This keeps the splash instant with no layout inflation delay

        // T05 — Check current Firebase auth state
        auth = Firebase.auth

        // Wait 2 seconds then redirect based on auth state
        Handler(Looper.getMainLooper()).postDelayed({
            if (auth.currentUser != null) {
                // User is already logged in — go to dashboard
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // No session — show login screen
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish() // Remove splash from back stack
        }, 2000)
    }
}
