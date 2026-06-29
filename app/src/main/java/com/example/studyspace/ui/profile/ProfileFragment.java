package com.example.studyspace.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.studyspace.R;
import com.example.studyspace.databinding.FragmentProfileBinding;
import com.example.studyspace.ui.auth.LoginActivity;
import com.example.studyspace.utils.NetworkUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            binding.tvUserEmail.setText("Not Logged In");
            setFieldsEnabled(false);
            return;
        }

        binding.tvUserEmail.setText(currentUser.getEmail());
        fetchProfileData();

        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileData();
            }
        });

        binding.btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                auth.signOut();
                navigateToLogin();
            }
        });

        binding.btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangePasswordDialog();
            }
        });

        binding.btnDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmAccountDeletion();
            }
        });
    }

    private void fetchProfileData() {
        if (!NetworkUtils.isConnected(requireContext())) {
            showSnackbar("No internet connection. Unable to load profile.");
            return;
        }

        setLoadingState(true);
        firestore.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        setLoadingState(false);
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String studentId = documentSnapshot.getString("studentId");
                            
                            if (name != null) binding.etFullName.setText(name);
                            if (studentId != null) binding.etStudentId.setText(studentId);
                        } else {
                            // Document doesn't exist, fallback to Auth Display Name
                            String displayName = currentUser.getDisplayName();
                            if (displayName != null) {
                                binding.etFullName.setText(displayName);
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        setLoadingState(false);
                        showSnackbar("Failed to load profile: " + e.getMessage());
                    }
                });
    }

    private void saveProfileData() {
        String newName = binding.etFullName.getText() != null ? binding.etFullName.getText().toString().trim() : "";
        String newStudentId = binding.etStudentId.getText() != null ? binding.etStudentId.getText().toString().trim().toUpperCase() : "";

        if (newName.isEmpty()) {
            binding.tilFullName.setError("Name cannot be empty");
            return;
        }
        if (newStudentId.isEmpty()) {
            binding.tilStudentId.setError("Student ID cannot be empty");
            return;
        }

        binding.tilFullName.setError(null);
        binding.tilStudentId.setError(null);

        if (!NetworkUtils.isConnected(requireContext())) {
            showSnackbar("No internet connection.");
            return;
        }

        setLoadingState(true);

        // Update Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("studentId", newStudentId);

        firestore.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // Also update Firebase Auth display name
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(newName)
                                .build();
                        
                        currentUser.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                setLoadingState(false);
                                if (task.isSuccessful()) {
                                    showSnackbar("Profile updated successfully!");
                                } else {
                                    showSnackbar("Profile partially updated (Auth sync failed).");
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        setLoadingState(false);
                        showSnackbar("Failed to update profile: " + e.getMessage());
                    }
                });
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);
        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String currentPw = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString() : "";
                    String newPw = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
                    performPasswordChange(currentPw, newPw);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performPasswordChange(String currentPassword, String newPassword) {
        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
            showSnackbar("Passwords cannot be empty");
            return;
        }
        if (newPassword.length() < 6) {
            showSnackbar("New password must be at least 6 characters");
            return;
        }
        if (!NetworkUtils.isConnected(requireContext())) {
            showSnackbar("No internet connection.");
            return;
        }

        setLoadingState(true);

        String email = currentUser.getEmail();
        if (email == null) {
            setLoadingState(false);
            return;
        }

        // Re-authenticate user
        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);
        currentUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> reauthTask) {
                if (reauthTask.isSuccessful()) {
                    // Update password
                    currentUser.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> updateTask) {
                            setLoadingState(false);
                            if (updateTask.isSuccessful()) {
                                showSnackbar("Password updated successfully");
                            } else {
                                String msg = updateTask.getException() != null ? updateTask.getException().getMessage() : "Failed to update password";
                                showSnackbar("Error: " + msg);
                            }
                        }
                    });
                } else {
                    setLoadingState(false);
                    String msg = reauthTask.getException() != null ? reauthTask.getException().getMessage() : "Authentication failed";
                    showSnackbar("Authentication Failed: " + msg);
                }
            }
        });
    }

    private void confirmAccountDeletion() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        if (!NetworkUtils.isConnected(requireContext())) {
            showSnackbar("No internet connection.");
            return;
        }

        setLoadingState(true);
        String uid = currentUser.getUid();

        // 1. Delete Firestore User Document
        firestore.collection("users").document(uid)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // 2. Delete Firebase Auth User
                        currentUser.delete()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        setLoadingState(false);
                                        if (task.isSuccessful()) {
                                            Toast.makeText(requireContext(), "Account deleted successfully.", Toast.LENGTH_LONG).show();
                                            navigateToLogin();
                                        } else {
                                            // Handle case where re-authentication is required
                                            Log.e("ProfileFragment", "Failed to delete Auth account", task.getException());
                                            showSnackbar("Failed to delete account. Please log out and log back in to verify your identity.");
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        setLoadingState(false);
                        showSnackbar("Failed to delete profile data: " + e.getMessage());
                    }
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setFieldsEnabled(boolean enabled) {
        binding.tilFullName.setEnabled(enabled);
        binding.tilStudentId.setEnabled(enabled);
        binding.btnSave.setEnabled(enabled);
        binding.btnChangePassword.setEnabled(enabled);
        binding.btnDeleteAccount.setEnabled(enabled);
    }

    private void setLoadingState(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        setFieldsEnabled(!isLoading);
        binding.btnLogout.setEnabled(!isLoading);
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
