package com.example.studyspace.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studyspace.R;
import com.example.studyspace.databinding.ActivityRegisterBinding;
import com.example.studyspace.utils.NetworkUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

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
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        binding.btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistration();
            }
        });

        binding.tvGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void performRegistration() {
        String fullName = binding.etFullName.getText() != null ? binding.etFullName.getText().toString().trim() : "";
        String studentId = binding.etStudentId.getText() != null ? binding.etStudentId.getText().toString().trim().toUpperCase() : "";
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";
        String confirmPassword = binding.etConfirmPassword.getText() != null ? binding.etConfirmPassword.getText().toString() : "";

        if (!validateInputs(fullName, studentId, email, password, confirmPassword)) return;

        if (!NetworkUtils.isConnected(this)) {
            showSnackbar(getString(R.string.error_no_internet));
            return;
        }

        setLoadingState(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            if (firebaseUser != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullName)
                                        .build();

                                firebaseUser.updateProfile(profileUpdates);

                                saveUserToFirestore(firebaseUser.getUid(), fullName, studentId, email);
                            }
                        } else {
                            setLoadingState(false);
                            String errorMessage = task.getException() != null && task.getException().getMessage() != null
                                    ? task.getException().getMessage() : "Registration failed. Try again.";
                            showSnackbar(errorMessage);
                        }
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String studentId, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("studentId", studentId);
        userData.put("email", email);
        userData.put("createdAt", Timestamp.now());

        firestore.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        setLoadingState(false);
                        showSnackbar(getString(R.string.register_success));
                        
                        binding.getRoot().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                navigateToLogin();
                            }
                        }, 1500);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        setLoadingState(false);
                        showSnackbar("Account created but profile save failed: " + e.getMessage());
                        navigateToLogin();
                    }
                });
    }

    private boolean validateInputs(String fullName, String studentId, String email, String password, String confirmPassword) {
        binding.tilFullName.setError(null);
        binding.tilStudentId.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        if (fullName.isEmpty()) {
            binding.tilFullName.setError(getString(R.string.error_empty_name));
            return false;
        }
        if (studentId.isEmpty()) {
            binding.tilStudentId.setError(getString(R.string.error_empty_student_id));
            return false;
        }
        if (email.isEmpty()) {
            binding.tilEmail.setError(getString(R.string.error_empty_email));
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return false;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.error_password_short));
            return false;
        }
        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return false;
        }
        return true;
    }

    private void setLoadingState(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!isLoading);
        binding.btnRegister.setText(isLoading ? "Creating account..." : getString(R.string.register));
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }
}
