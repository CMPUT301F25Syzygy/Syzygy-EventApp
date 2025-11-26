package com.example.syzygy_eventapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Initial onboarding / welcome screen.
 *
 * Shown when the device does NOT yet have a user profile.
 * - If a profile already exists, this activity immediately forwards to MainActivity.
 * - If no profile exists, it shows the title and a "Create Profile" button.
 *
 * Used both on fresh app install and after the user deletes their profile.
 */
public class WelcomeActivity extends AppCompatActivity {

    private UserControllerInterface userController;
    private String userID;
    private Button createProfileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        userController = UserController.getInstance();
        userID = AppInstallationId.get(this);

        boolean profileDeleted = getIntent().getBooleanExtra("profile_deleted", false);
        if (profileDeleted) {
            Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
        }

        createProfileButton = findViewById(R.id.createProfileButton);
        createProfileButton.setOnClickListener(v -> createProfile());
    }

    @Override
    protected void onStart() {
        super.onStart();

        // If a user profile already exists for this device, skip the welcome screen.
        userController.getUser(userID)
                .addOnSuccessListener(user -> {
                    // Profile exists -> go straight into the app.
                    openMainActivity();
                })
                .addOnFailureListener(err -> {
                    // User not found -> show the Create Profile button.
                    createProfileButton.setVisibility(View.VISIBLE);
                });
    }

    /**
     * Creates a new entrant profile and then enters the main app.
     */
    private void createProfile() {
        userController.createEntrant(userID)
                .addOnSuccessListener(user -> {
                    openMainActivity();
                })
                .addOnFailureListener(err -> {
                    String msg = (err.getMessage() == null) ? "unknown error" : err.getMessage();
                    Toast.makeText(
                            this,
                            "Failed to create profile: " + msg,
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    /**
     * Launches MainActivity and clears the back stack so you can't go "Back" to the welcome screen.
     */
    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
