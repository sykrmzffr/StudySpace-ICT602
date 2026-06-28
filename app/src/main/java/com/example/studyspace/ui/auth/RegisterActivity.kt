package com.example.studyspace.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.studyspace.R
import com.example.studyspace.databinding.ActivityRegisterBinding
import com.example.studyspace.utils.NetworkUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

/**
 * RegisterActivity — New student account creation.
 *
 * Tutorial References:
 *  - T01 (Input/Output): 5 input fields (name, studentId, email, password, confirm), Snackbar output
 *  - T02 (Layout): Inflating activity_register.xml
 *  - T04 (Online Database): Saving new user profile to Firestore after auth creation
 *  - T05 (Authentication): Firebase Auth — createUserWithEmailAndPassword()
 *  - T07 (Connectivity): Network check before Firebase operations
 */
class RegisterActivity : AppCompatActivity() {

    // View Binding — generated from activity_register.xml
    private lateinit var binding: ActivityRegisterBinding

    // T05 — Firebase Auth instance
    private lateinit var auth: FirebaseAuth

    // T04 — Firestore for saving user profile data
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // T02 — Inflate register layout
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // T05 — Initialize Firebase services
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {

        // Back button — go back to Login
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Register button — triggers Firebase registration
        binding.btnRegister.setOnClickListener {
            performRegistration()
        }

        // Already have account — go to Login
        binding.tvGoToLogin.setOnClickListener {
            finish() // Simply pop back to LoginActivity
        }
    }

    /**
     * Validates all 5 inputs then creates account via Firebase Auth.
     * T05 — createUserWithEmailAndPassword
     * T04 — saves profile to Firestore users/{userId}
     * T07 — checks network before proceeding
     */
    private fun performRegistration() {
        // T01 — Read all input values
        val fullName = binding.etFullName.text.toString().trim()
        val studentId = binding.etStudentId.text.toString().trim().uppercase()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        // T01 — Validate all fields
        if (!validateInputs(fullName, studentId, email, password, confirmPassword)) return

        // T07 — Network connectivity check
        if (!NetworkUtils.isConnected(this)) {
            showSnackbar(getString(R.string.error_no_internet))
            return
        }

        // Show loading state
        setLoadingState(true)

        // T05 — Step 1: Create user account in Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser

                    // T05 — Step 2: Update display name on the Firebase Auth profile
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()

                    firebaseUser?.updateProfile(profileUpdates)

                    // T04 — Step 3: Save full profile data to Firestore
                    // Firestore path: users/{userId}/
                    saveUserToFirestore(
                        userId = firebaseUser!!.uid,
                        name = fullName,
                        studentId = studentId,
                        email = email
                    )
                } else {
                    setLoadingState(false)
                    // T01 — Output: Error message
                    showSnackbar(task.exception?.message ?: "Registration failed. Try again.")
                }
            }
    }

    /**
     * T04 — Saves the newly registered user's profile to Firestore.
     * Firestore structure: users/{userId} → { name, studentId, email }
     */
    private fun saveUserToFirestore(
        userId: String,
        name: String,
        studentId: String,
        email: String
    ) {
        // Build the user data map matching the Firestore schema
        val userData = hashMapOf(
            "name" to name,
            "studentId" to studentId,
            "email" to email,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        // T04 — Write to Firestore: users/{userId}
        firestore.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                setLoadingState(false)
                // T01 — Output: Success message
                showSnackbar(getString(R.string.register_success))

                // Navigate back to Login after short delay
                binding.root.postDelayed({
                    navigateToLogin()
                }, 1500)
            }
            .addOnFailureListener { exception ->
                setLoadingState(false)
                // Auth user was created but Firestore write failed — still let them in
                showSnackbar("Account created but profile save failed: ${exception.message}")
                navigateToLogin()
            }
    }

    /**
     * T01 — Validates all 5 registration form fields.
     * Returns true if all valid, false with appropriate error messages if not.
     */
    private fun validateInputs(
        fullName: String,
        studentId: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        // Clear all previous errors
        binding.tilFullName.error = null
        binding.tilStudentId.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        if (fullName.isEmpty()) {
            binding.tilFullName.error = getString(R.string.error_empty_name)
            return false
        }
        if (studentId.isEmpty()) {
            binding.tilStudentId.error = getString(R.string.error_empty_student_id)
            return false
        }
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_empty_email)
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            return false
        }
        if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_short)
            return false
        }
        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            return false
        }
        return true
    }

    /**
     * Shows/hides the loading state on the register button and progress bar.
     */
    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        binding.btnRegister.text = if (isLoading) "Creating account..." else getString(R.string.register)
    }

    /**
     * Navigates back to LoginActivity and clears this activity from the back stack.
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    /**
     * T01 — Shows a Snackbar for user feedback.
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
