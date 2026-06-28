package com.example.studyspace.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.studyspace.databinding.ActivityLoginBinding
import com.example.studyspace.ui.MainActivity
import com.example.studyspace.utils.NetworkUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * LoginActivity — Entry point for returning users.
 *
 * Tutorial References:
 *  - T01 (Input/Output): Reading email & password from TextInputEditText, showing Snackbar feedback
 *  - T02 (Layout): Inflating activity_login.xml with ConstraintLayout + MaterialCard
 *  - T05 (Authentication): Firebase Auth — signInWithEmailAndPassword()
 *  - T07 (Connectivity): Check network availability before making Firebase calls
 */
class LoginActivity : AppCompatActivity() {

    // View Binding — generated from activity_login.xml (eliminates findViewById)
    private lateinit var binding: ActivityLoginBinding

    // Firebase Auth instance (T05)
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // T02 — Inflate the login layout using View Binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // T05 — Initialize Firebase Authentication
        auth = Firebase.auth

        // T05 — If user is already logged in, skip login and go to home
        if (auth.currentUser != null) {
            navigateToHome()
            return
        }

        setupClickListeners()
    }

    /**
     * Sets up click handlers for all interactive elements.
     */
    private fun setupClickListeners() {

        // Login Button — triggers Firebase Auth (T05)
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        // Navigate to Register Screen
        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Forgot Password — sends a password reset email (T05)
        binding.tvForgotPassword.setOnClickListener {
            handleForgotPassword()
        }
    }

    /**
     * Validates inputs and calls Firebase signInWithEmailAndPassword.
     * T05 — Firebase Authentication
     * T07 — Checks network connectivity first
     */
    private fun performLogin() {
        // T01 — Read user input from TextInputEditText fields
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // T01 — Validate inputs before making any network calls
        if (!validateInputs(email, password)) return

        // T07 — Check internet connectivity before calling Firebase
        if (!NetworkUtils.isConnected(this)) {
            showSnackbar("No internet connection. Please check your network.")
            return
        }

        // Show loading state
        setLoadingState(true)

        // T05 — Firebase: Sign in with email and password
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoadingState(false)

                if (task.isSuccessful) {
                    // T01 — Output: Success feedback
                    showSnackbar("Welcome back!")

                    // Navigate to main app screen
                    navigateToHome()
                } else {
                    // T01 — Output: Error message from Firebase
                    val errorMessage = task.exception?.message ?: "Login failed. Try again."
                    showSnackbar(errorMessage)
                }
            }
    }

    /**
     * Sends a password reset email to the user.
     * T05 — Firebase Auth: sendPasswordResetEmail()
     */
    private fun handleForgotPassword() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            binding.tilEmail.error = "Enter your email first"
            return
        }

        // T07 — Network check
        if (!NetworkUtils.isConnected(this)) {
            showSnackbar("No internet connection.")
            return
        }

        // T05 — Firebase: Send password reset email
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showSnackbar("Reset email sent to $email")
                } else {
                    showSnackbar(task.exception?.message ?: "Failed to send reset email")
                }
            }
    }

    /**
     * T01 — Validates email and password fields.
     * Returns true if valid, false with error messages if not.
     */
    private fun validateInputs(email: String, password: String): Boolean {
        // Clear previous errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(com.example.studyspace.R.string.error_empty_email)
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(com.example.studyspace.R.string.error_invalid_email)
            return false
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(com.example.studyspace.R.string.error_empty_password)
            return false
        }
        return true
    }

    /**
     * Shows or hides the loading progress bar and disables the login button.
     */
    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) "Signing in..." else getString(com.example.studyspace.R.string.login)
    }

    /**
     * Navigates to the main app (dashboard) and clears the back stack.
     */
    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        // Clear back stack so user can't press back to return to login
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * T01 — Shows a Snackbar message as user feedback.
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
