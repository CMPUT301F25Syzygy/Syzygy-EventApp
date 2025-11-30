package com.example.syzygy_eventapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Filter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
    private static final int MAX_IMAGE_SIZE = 200;
    private static final int PERMISSION_REQUEST_IMAGE = 100;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
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

    // Delete Profile Button
    private View deleteProfileButton;

    // Invitation handling
    private InvitationController invitationController;

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
        deleteProfileButton = view.findViewById(R.id.deleteProfileButton);

        // Retrieve the userID for this device or authenticated user
        userID = AppInstallationId.get(requireContext());
        userController = UserController.getInstance();

        invitationController = new InvitationController();

        // Setup editable panels for name, email, and phone
        profileNamePanel.setOnClickListener(v -> showEditDialog(
                "Edit Username",
                profileNameText.getText().toString(),
                InputType.TYPE_CLASS_TEXT,
                newValue -> updateUserField("name", newValue)
        ));

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            loadAndConvertProfileImage(selectedImageUri);
                        }
                    }
                }
        );

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

        profileRoleBadge.setOnClickListener(v -> {
            if (currentUser == null || currentUser.getRole() == null) {
                showError("User data not loaded yet.");
                return;
            }

            Role currentRole = currentUser.getRole();

            Role newRole = currentRole;
            switch(currentRole) {
                case ENTRANT:
                    newRole = Role.ORGANIZER;
                    break;
                case ORGANIZER:
                    newRole = Role.ADMIN;
                    break;
                case ADMIN:
                    newRole = Role.ENTRANT;
                    break;
            }

            Role finalNewRole = newRole;
            userController.setUserRole(userID, newRole)
                    .addOnSuccessListener(updatedUser ->
                            Toast.makeText(getContext(),
                                    "Role changed to " + finalNewRole.name(),
                                    Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(err ->
                            showError("Failed to change role: " + err.getMessage()));
        });

        deleteProfileButton.setOnClickListener(v -> {
            // Confirm before deleting
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete profile")
                    .setMessage("Are you sure you want to permanently delete your profile and data? This cannot be undone.")
                    .setPositiveButton("Confirm", (dialog, which) -> profileDelete())
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        profileImageView.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(getContext(), "User not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }
            openImagePicker();
        });
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
                .addOnSuccessListener(user -> {
                        startUserListener(userID);
                        checkPendingInvites();
                })
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

            if (user.getPhotoURL() != null && !user.getPhotoURL().isEmpty()) {
                byte[] decodedBytes = Base64.decode(user.getPhotoURL(), Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (decodedBitmap != null) {
                    profileImageView.setImageBitmap(decodedBitmap);
                } else {
                    profileImageView.setImageResource(R.drawable.profile_placeholder);
                }
            } else {
                profileImageView.setImageResource(R.drawable.profile_placeholder);
            }

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

    /**
     * Permanently deletes the current user's profile from DB and return to WelcomeActivity.
     */
    private void profileDelete() {
        if (userID == null) {
            showError("User ID not available.");
            return;
        }

        // Remove this fragment's listener
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }

        userController.deleteUser(userID)
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(err -> {
                    showError("Failed to delete profile: " +
                            (err.getMessage() == null ? "unknown error" : err.getMessage()));
                });
    }

    /**
     * Opens the device's image picker to allow the user to select a poster image.
     * It will request the needed permissions based on Android's version before opening.
     */
    private void openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_IMAGE);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_IMAGE);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    /**
     * Loads an image from the given URI, resizes it, and converts it to Base64 format.
     * Updates the poster preview and enables the delete button
     *
     * @param imageUri The URI of the selected image
     */
    private void loadAndConvertProfileImage(Uri imageUri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(
                        requireContext().getContentResolver(), imageUri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                InputStream inputStream = requireContext().getContentResolver()
                        .openInputStream(imageUri);
                bitmap = BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) inputStream.close();
            }

            if (bitmap == null) {
                showError("Failed to load image");
                return;
            }

            Bitmap resized = resizeBitmap(bitmap);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();

            String encoded = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            currentUser.setPhotoURL(encoded);
            updateUserField("photoURL", encoded);

            profileImageView.setImageBitmap(resized);
            Toast.makeText(getContext(), "Profile image updated", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Image error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Resizes a bitmap to fit within the specified max. dimensions while maintaining aspect ratio
     *
     * @param bitmap  The original bitmap to resize
     * @return The resized bitmpa, or the original if already smaller than maxSize
     */
    private Bitmap resizeBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= ProfileFragment.MAX_IMAGE_SIZE && height <= ProfileFragment.MAX_IMAGE_SIZE) return bitmap;

        float ratio = Math.min((float) ProfileFragment.MAX_IMAGE_SIZE / width, (float) ProfileFragment.MAX_IMAGE_SIZE / height);
        int newW = Math.round(width * ratio);
        int newH = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newW, newH, true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_IMAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                showError("Permission denied to read images");
            }
        }
    }


    /**
     * Checks Firestore for any pending invitations for this user and, if found,
     * opens InvitationActivity for the first pending invite.
     */
    private void checkPendingInvites() {
        com.google.firebase.firestore.Filter filter =
                com.google.firebase.firestore.Filter.equalTo("recipientID", userID);

        invitationController.getInvites(filter)
                .addOnSuccessListener(invitations -> {
                    if (!isAdded()) {
                        return;
                    }

                    for (Invitation invite : invitations) {
                        if (Boolean.TRUE.equals(invite.getCancelled())) {
                            continue;
                        }

                        if (invite.getResponseTime() != null) {
                            continue;
                        }

                        Boolean accepted = invite.getAccepted();
                        boolean isPending = (accepted == null) || !accepted;

                        if (isPending) {
                            Intent intent = new Intent(requireContext(), InvitationActivity.class);
                            intent.putExtra(
                                    InvitationActivity.EXTRA_INVITATION_ID,
                                    invite.getInvitation()
                            );
                            startActivity(intent);
                            break;
                        }
                    }
                });
    }




}
