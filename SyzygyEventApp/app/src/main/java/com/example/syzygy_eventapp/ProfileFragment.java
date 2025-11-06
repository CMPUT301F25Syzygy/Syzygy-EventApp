package com.example.syzygy_eventapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Fragment representing the user's profile screen.
 * <p>
 * Displays and allows editing of user information such as name, email, and phone number.
 * Shows role badges and notification preferences. Data is synced with Firestore using
 * the UserControllerInterface.
 */
public class ProfileFragment extends Fragment {

    // Profile section
    private ShapeableImageView profileImageView;
    private LinearLayout profileNamePanel;
    private TextView profileNameText;
    private Chip profileRoleBadge;

    // Email & phone
    private LinearLayout profileEmailPanel;
    private TextView profileEmailText;
    private LinearLayout phoneNumberPanel;
    private TextView profilePhoneNumberText;

    // Notifications
    private SwitchMaterial lotteryNotificationsSwitch;
    private SwitchMaterial organizerNotificationsSwitch;

    // User handling
    private UserControllerInterface userController;
    private ListenerRegistration userListener;
    private User currentUser;
    private String userID;

    /**
     * Default constructor.
     */
    public ProfileFragment() { }

    /**
     * Inflates the fragment layout.
     *
     * @param inflater  LayoutInflater object used to inflate views.
     * @param container Parent view that the fragment's UI should attach to.
     * @param savedInstanceState Previously saved state (if any).
     * @return The root view of the fragment layout.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    /**
     * Called after the view has been created. Initializes all view references,
     * retrieves the user ID, and sets up editable panels with click listeners.
     *
     * @param view The fragment's root view.
     * @param savedInstanceState Previously saved state (if any).
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        profileImageView = view.findViewById(R.id.profileImage);
        profileNamePanel = view.findViewById(R.id.profileNamePanel);
        profileNameText = view.findViewById(R.id.profileNameText);
        profileRoleBadge = view.findViewById(R.id.profileRoleBadge);
        profileEmailPanel = view.findViewById(R.id.profileEmailPanel);
        profileEmailText = view.findViewById(R.id.profileEmailText);
        phoneNumberPanel = view.findViewById(R.id.phoneNumberPanel);
        profilePhoneNumberText = view.findViewById(R.id.profilePhoneNumberText);
        lotteryNotificationsSwitch = view.findViewById(R.id.lotteryNotificationsSwitch);
        organizerNotificationsSwitch = view.findViewById(R.id.organizerNotificationsSwitch);

        // Retrieve the userID for this device or authenticated user
        userID = AppInstallationId.get(requireContext());
        userController = UserController.getInstance();

        // Setup editable panels for name, email, and phone
        profileNamePanel.setOnClickListener(v -> showEditDialog(
                "Edit Username",
                profileNameText.getText().toString(),
                InputType.TYPE_CLASS_TEXT,
                newValue -> updateUserField("name", newValue)
        ));

        profileEmailPanel.setOnClickListener(v -> showEditDialog(
                "Edit Email",
                profileEmailText.getText().toString(),
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                newValue -> {
                    if (Patterns.EMAIL_ADDRESS.matcher(newValue).matches()) {
                        updateUserField("email", newValue);
                    } else {
                        showError("Please enter a valid email address.");
                    }
                }
        ));

        phoneNumberPanel.setOnClickListener(v -> showEditDialog(
                "Edit Phone Number",
                profilePhoneNumberText.getText().toString(),
                InputType.TYPE_CLASS_PHONE,
                newValue -> {
                    if (Patterns.PHONE.matcher(newValue).matches()) {
                        updateUserField("phone", newValue);
                    } else {
                        showError("Please enter a valid phone number.");
                    }
                }
        ));
    }

    /**
     * Starts the fragment and ensures that a user exists for this device.
     * Begins listening for real-time updates to the user's data.
     */
    @Override
    public void onStart() {
        super.onStart();

        // Ensure user exists before observing
        userController.getUser(userID)
                .addOnSuccessListener(user -> startUserListener(userID))
                .addOnFailureListener(err -> showError("Failed to find user: " + err.getMessage()));
    }

    /**
     * Stops the fragment and removes the UserController listener.
     */
    @Override
    public void onStop() {
        super.onStop();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }

    /**
     * Begins observing the UserController for real-time updates.
     * If a previous listener exists, it is removed first.
     *
     * @param userID The ID of the user to observe.
     */
    private void startUserListener(String userID) {
        if (userListener != null) userListener.remove();

        userListener = userController.observeUser(
                userID,
                this::updateUIFromUser,
                () -> showError("User was deleted")
        );
    }

    /**
     * Updates all UI elements with the latest data from the User object.
     * If the fragment view is not attached or user is null, the update is skipped.
     *
     * @param user The latest User object retrieved from the UserController.
     */
    private void updateUIFromUser(User user) {
        currentUser = user;
        if (user == null || getView() == null) return;

        requireActivity().runOnUiThread(() -> {
            profileNameText.setText(user.getName() != null ? user.getName() : "(No name)");
            profileEmailText.setText(user.getEmail() != null ? user.getEmail() : "(No email)");
            profilePhoneNumberText.setText(user.getPhone() != null ? user.getPhone() : "(No phone)");
            profileRoleBadge.setText(user.getRole() != null ? user.getRole().name() : "Unassigned");
        });
    }

    /**
     * Updates a single field in the user's Firestore document.
     *
     * @param key   The name of the field to update (e.g., "name", "email").
     * @param value The new value to set for the field.
     */
    private void updateUserField(String key, String value) {
        if (userID == null) {
            showError("User ID not available.");
            return;
        }

        HashMap<String, Object> update = new HashMap<>();
        update.put(key, value);

        userController.updateFields(userID, update);
    }

    /**
     * Shows a simple dialog allowing the user to edit a text field.
     *
     * @param title      The dialog title.
     * @param currentVal The current value of the field to pre-fill in the EditText.
     * @param inputType  The input type (e.g. text, phone, email) for the EditText.
     * @param onSave     Callback invoked with the new value when the user clicks "Save".
     */
    private void showEditDialog(String title, String currentVal, int inputType, Consumer<String> onSave) {
        if (getContext() == null) return;

        final EditText input = new EditText(getContext());
        input.setInputType(inputType);
        input.setText(currentVal);
        input.setSelection(input.getText().length());
        input.setPadding(60, 50, 60, 50);

        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newValue = input.getText().toString().trim();
                    if (!newValue.isEmpty()) onSave.accept(newValue);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Displays a Toast message to indicate an error.
     *
     * @param msg The error message to display.
     */
    private void showError(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
        }
    }
}
