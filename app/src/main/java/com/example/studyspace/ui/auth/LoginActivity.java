package com.example.studyspace.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studyspace.R;
import com.example.studyspace.databinding.ActivityLoginBinding;
import com.example.studyspace.ui.MainActivity;
import com.example.studyspace.utils.NetworkUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

/**
 * LoginActivity — Entry point for returning users.
 *
 * Tutorial References:
 *  - T01 (Input/Output): Reading email & password from TextInputEditText, showing Snackbar feedback
 *  - T02 (Layout): Inflating activity_login.xml with ConstraintLayout + MaterialCard
 *  - T05 (Authentication): Firebase Auth — signInWithEmailAndPassword()
 *  - T07 (Connectivity): Check network availability before making Firebase calls
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            navigateToHome();
            return;
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        binding.tvGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        binding.tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleForgotPassword();
            }
        });
    }

    private void performLogin() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";

        if (!validateInputs(email, password)) return;

        if (!NetworkUtils.isConnected(this)) {
            showSnackbar("No internet connection. Please check your network.");
            return;
        }

        setLoadingState(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setLoadingState(false);

                        if (task.isSuccessful()) {
                            showSnackbar("Welcome back!");
                            navigateToHome();
                        } else {
                            String errorMessage = task.getException() != null && task.getException().getMessage() != null
                                    ? task.getException().getMessage() : "Login failed. Try again.";
                            showSnackbar(errorMessage);
                        }
                    }
                });
    }

    private void handleForgotPassword() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";

        if (email.isEmpty()) {
            binding.tilEmail.setError("Enter your email first");
            return;
        }

        if (!NetworkUtils.isConnected(this)) {
            showSnackbar("No internet connection.");
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            showSnackbar("Reset email sent to " + email);
                        } else {
                            String errorMessage = task.getException() != null && task.getException().getMessage() != null
                                    ? task.getException().getMessage() : "Failed to send reset email";
                            showSnackbar(errorMessage);
                        }
                    }
                });
    }

    private boolean validateInputs(String email, String password) {
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        if (email.isEmpty()) {
            binding.tilEmail.setError(getString(R.string.error_empty_email));
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return false;
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.error_empty_password));
            return false;
        }
        return true;
    }

    private void setLoadingState(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnLogin.setText(isLoading ? "Signing in..." : getString(R.string.login));
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }
}
