package com.example.syzygy_eventapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Fragment class that allows an organizer to create or edit an event's details.
 */
public class OrganizerEventEditDetailsFragment extends Fragment {

    private EditText titleInput, locationInput, entrantLimitInput, descriptionInput;
    private Button startTimeButton, endTimeButton, startDateButton, endDateButton;
    private Button importPosterButton, deletePosterButton;
    private ImageView posterPreview;
    private Button createButton, cancelButton, updateButton, deleteButton;
    private EventController eventController;
    private InvitationController invitationController;
    private Organizer organizer;
    private Event event;
    private boolean isEditMode = false;
    private Timestamp startTime, endTime;
    private NavigationStackFragment navStack;

    private static final String TAG = "OrganizerEventEdit";
    private TextView fragmentTitle;
    private Uri selectedImageUri;
    private String posterBase64;
    private boolean posterDeleted = false;
    private static final int MAX_IMAGE_SIZE = 800;

    // Waiting list UI elements
    private TextView acceptedCountText, pendingCountText, waitingCountText;
    private LinearLayout acceptedLabelLayout, pendingLabelLayout, waitingLabelLayout;
    private ListView listAccepted, listPending, listWaiting;

    // Real-time data
    private List<String> waitingListUsers = new ArrayList<>();
    private List<String> acceptedUsers = new ArrayList<>();
    private List<String> pendingUsers = new ArrayList<>();

    // Firestore listeners
    private ListenerRegistration eventListener;
    private ListenerRegistration invitationsListener;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public OrganizerEventEditDetailsFragment() {
    }

    public static OrganizerEventEditDetailsFragment newInstance(@Nullable Event existingEvent, @NonNull Organizer organizer, @Nullable NavigationStackFragment navStack) {
        OrganizerEventEditDetailsFragment fragment = new OrganizerEventEditDetailsFragment();
        fragment.navStack = navStack;
        fragment.event = existingEvent;
        fragment.isEditMode = existingEvent != null;
        fragment.organizer = organizer;
        return fragment;
    }

    public static OrganizerEventEditDetailsFragment newInstance(@Nullable Event existingEvent, @NonNull Organizer organizer) {
        OrganizerEventEditDetailsFragment fragment = new OrganizerEventEditDetailsFragment();
        Bundle args = new Bundle();
        if (existingEvent != null) {
            args.putString("eventID", existingEvent.getEventID());
        }
        args.putString("organizerID", organizer.getUserID());
        fragment.setArguments(args);
        fragment.event = existingEvent;
        fragment.isEditMode = existingEvent != null;
        fragment.organizer = organizer;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            loadAndConvertImage(selectedImageUri);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_event, container, false);

        // Initialize views
        fragmentTitle = view.findViewById(R.id.edit_title);
        titleInput = view.findViewById(R.id.edit_event_name);
        locationInput = view.findViewById(R.id.edit_location);
        descriptionInput = view.findViewById(R.id.edit_description);
        posterPreview = view.findViewById(R.id.edit_poster);
        importPosterButton = view.findViewById(R.id.btnUpload);
        deletePosterButton = view.findViewById(R.id.btnDelete);
        createButton = view.findViewById(R.id.create_button);
        cancelButton = view.findViewById(R.id.cancel_button);
        updateButton = view.findViewById(R.id.update_button);
        deleteButton = view.findViewById(R.id.delete_button);
        entrantLimitInput = view.findViewById(R.id.max_entrants);

        startDateButton = view.findViewById(R.id.btnStartDate);
        startTimeButton = view.findViewById(R.id.btnStartTime);
        endDateButton = view.findViewById(R.id.btnEndDate);
        endTimeButton = view.findViewById(R.id.btnEndTime);

        // Initialize waiting list UI elements
        acceptedCountText = view.findViewById(R.id.accepted_count);
        pendingCountText = view.findViewById(R.id.pending_count);
        waitingCountText = view.findViewById(R.id.Waiting_count);

        acceptedLabelLayout = view.findViewById(R.id.tvAcceptedLabel).getParent() instanceof LinearLayout ? (LinearLayout) view.findViewById(R.id.tvAcceptedLabel).getParent() : null;
        pendingLabelLayout = view.findViewById(R.id.tvPendingLabel).getParent() instanceof LinearLayout ? (LinearLayout) view.findViewById(R.id.tvPendingLabel).getParent() : null;
        waitingLabelLayout = view.findViewById(R.id.WaitingLabel).getParent() instanceof LinearLayout ? (LinearLayout) view.findViewById(R.id.WaitingLabel).getParent() : null;

        listAccepted = view.findViewById(R.id.listAccepted);
        listPending = view.findViewById(R.id.listPending);
        listWaiting = view.findViewById(R.id.listWaiting);

        eventController = new EventController();
        invitationController = new InvitationController();

        if (isEditMode) {
            fragmentTitle.setText("Edit Event");
        }
        else {
            fragmentTitle.setText("Create Event");
        }

        setupButtonVisibility();
        setupListeners();
        setupWaitingListListeners();

        if (isEditMode && event != null) {
            populateFields(event);
            startRealtimeListeners();
        }
        else {
            updateDateButtonText(startDateButton, null);
            updateDateButtonText(endDateButton, null);
            updateTimeButtonText(startTimeButton, null);
            updateTimeButtonText(endTimeButton, null);

            // Hide waiting list sections in create mode
            hideWaitingListSections();
        }

        return view;
    }

    private void hideWaitingListSections() {
        if (acceptedLabelLayout != null) acceptedLabelLayout.setVisibility(View.GONE);
        if (pendingLabelLayout != null) pendingLabelLayout.setVisibility(View.GONE);
        if (waitingLabelLayout != null) waitingLabelLayout.setVisibility(View.GONE);
        if (listAccepted != null) listAccepted.setVisibility(View.GONE);
        if (listPending != null) listPending.setVisibility(View.GONE);
        if (listWaiting != null) listWaiting.setVisibility(View.GONE);
    }

    private void setupButtonVisibility() {
        if (isEditMode) {
            createButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            updateButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
        }
        else {
            createButton.setVisibility(View.VISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
            updateButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        importPosterButton.setOnClickListener(v -> openImagePicker());

        deletePosterButton.setOnClickListener(v -> {
            selectedImageUri = null;
            posterBase64 = null;
            posterDeleted = true;
            posterPreview.setImageResource(R.drawable.image_placeholder);
            deletePosterButton.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Poster removed", Toast.LENGTH_SHORT).show();
        });

        startDateButton.setOnClickListener(v -> showDatePicker(startDateButton, startTime, date -> {
            startTime = mergeDateWithTime(date, startTime);
            updateDateButtonText(startDateButton, startTime);
        }));

        startTimeButton.setOnClickListener(v -> showTimePicker(startTimeButton, startTime, time -> {
            startTime = mergeDateWithTime(startTime, time);
            updateTimeButtonText(startTimeButton, startTime);
        }));

        endDateButton.setOnClickListener(v -> showDatePicker(endDateButton, endTime, date -> {
            endTime = mergeDateWithTime(date, endTime);
            updateDateButtonText(endDateButton, endTime);
        }));

        endTimeButton.setOnClickListener(v -> showTimePicker(endTimeButton, endTime, time -> {
            endTime = mergeDateWithTime(endTime, time);
            updateTimeButtonText(endTimeButton, endTime);
        }));

        createButton.setOnClickListener(v -> createEvent());
        updateButton.setOnClickListener(v -> updateEvent());
        cancelButton.setOnClickListener(v -> {
            if (navStack != null) navStack.popScreen();
        });
        deleteButton.setOnClickListener(v -> deleteEvent());
    }

    private void setupWaitingListListeners() {
        // Click listeners for accepted
        if (acceptedLabelLayout != null) {
            acceptedLabelLayout.setOnClickListener(v -> showUserListDialog("Accepted Users", acceptedUsers));
        }
        if (acceptedCountText != null) {
            acceptedCountText.setOnClickListener(v -> showUserListDialog("Accepted Users", acceptedUsers));
        }

        // Click listeners for pending
        if (pendingLabelLayout != null) {
            pendingLabelLayout.setOnClickListener(v -> showUserListDialog("Pending Users", pendingUsers));
        }
        if (pendingCountText != null) {
            pendingCountText.setOnClickListener(v -> showUserListDialog("Pending Users", pendingUsers));
        }

        // Click listeners for waiting
        if (waitingLabelLayout != null) {
            waitingLabelLayout.setOnClickListener(v -> showUserListDialog("Waiting List Users", waitingListUsers));
        }
        if (waitingCountText != null) {
            waitingCountText.setOnClickListener(v -> showUserListDialog("Waiting List Users", waitingListUsers));
        }
    }

    private void showUserListDialog(String title, List<String> userIds) {
        if (userIds == null) {
            userIds = new ArrayList<>();
        }
        UserListDialogFragment dialog = UserListDialogFragment.newInstance(title, userIds);
        dialog.show(getChildFragmentManager(), "user_list_dialog");
    }

    private void startRealtimeListeners() {
        if (event == null || event.getEventID() == null) return;

        // Listen to event changes (for waiting list)
        eventListener = eventController.observeEvent(event.getEventID(), updatedEvent -> {
            event = updatedEvent;
            waitingListUsers = event.getWaitingList() != null ? event.getWaitingList() : new ArrayList<>();
            updateWaitingCount();
        }, error -> {
            Log.e(TAG, "Error observing event", error);
        });

        // Listen to invitations changes (for accepted and pending)
        invitationsListener = invitationController.observeEventInvitations(event.getEventID(), invitations -> {
            acceptedUsers = new ArrayList<>();
            pendingUsers = new ArrayList<>();

            for (Invitation invitation : invitations) {
                if (Boolean.TRUE.equals(invitation.getCancelled())) {
                    continue; // Skip cancelled invitations
                }

                Boolean accepted = invitation.getAccepted();
                if (accepted == null) {
                    // Pending
                    pendingUsers.add(invitation.getRecipientID());
                }
                else if (accepted) {
                    // Accepted
                    acceptedUsers.add(invitation.getRecipientID());
                }
                // We don't track rejected invitations in these lists
            }

            updateAcceptedCount();
            updatePendingCount();
        }, error -> {
            Log.e(TAG, "Error observing invitations", error);
        });
    }

    private void updateAcceptedCount() {
        if (acceptedCountText != null) {
            acceptedCountText.setText(String.valueOf(acceptedUsers.size()));
        }
    }

    private void updatePendingCount() {
        if (pendingCountText != null) {
            pendingCountText.setText(String.valueOf(pendingUsers.size()));
        }
    }

    private void updateWaitingCount() {
        if (waitingCountText != null) {
            waitingCountText.setText(String.valueOf(waitingListUsers.size()));
        }
    }

    private void openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
                return;
            }
        }
        else {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void loadAndConvertImage(Uri imageUri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(
                        requireContext().getContentResolver(), imageUri);
                bitmap = ImageDecoder.decodeBitmap(source);
            }
            else {
                InputStream inputStream = requireContext().getContentResolver()
                        .openInputStream(imageUri);
                bitmap = BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) {
                    inputStream.close();
                }
            }

            if (bitmap == null) {
                Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }

            Bitmap resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            posterBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            Log.d(TAG, "Image converted to Base64, size: " + posterBase64.length() + " characters");

            posterPreview.setImageBitmap(resizedBitmap);
            deletePosterButton.setVisibility(View.VISIBLE);
            posterDeleted = false;

            Toast.makeText(getContext(), "Image loaded successfully", Toast.LENGTH_SHORT).show();

        }
        catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            Toast.makeText(getContext(), "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void loadImageFromBase64(String base64String) {
        if (TextUtils.isEmpty(base64String)) {
            posterPreview.setImageResource(R.drawable.image_placeholder);
            deletePosterButton.setVisibility(View.GONE);
            return;
        }

        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            posterPreview.setImageBitmap(bitmap);
            deletePosterButton.setVisibility(View.VISIBLE);
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to load image from Base64", e);
            posterPreview.setImageResource(R.drawable.image_placeholder);
            deletePosterButton.setVisibility(View.GONE);
        }
    }

    private void updateDateButtonText(Button button, Timestamp timestamp) {
        if (timestamp != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            button.setText(dateFormat.format(timestamp.toDate()));
        }
        else {
            if (button.getId() == R.id.btnStartDate) {
                button.setText("Select Start Date");
            }
            else if (button.getId() == R.id.btnEndDate) {
                button.setText("Select End Date");
            }
        }
    }

    private void updateTimeButtonText(Button button, Timestamp timestamp) {
        if (timestamp != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            button.setText(dateFormat.format(timestamp.toDate()));
        }
        else {
            if (button.getId() == R.id.btnStartTime) {
                button.setText("Select Start Time");
            }
            else if (button.getId() == R.id.btnEndTime) {
                button.setText("Select End Time");
            }
        }
    }

    private void populateFields(Event e) {
        titleInput.setText(e.getName());
        locationInput.setText(e.getLocationName());
        descriptionInput.setText(e.getDescription());
        entrantLimitInput.setText(e.getMaxAttendees() != null ? String.valueOf(e.getMaxAttendees()) : "");

        startTime = e.getRegistrationStart();
        endTime = e.getRegistrationEnd();
        updateDateButtonText(startDateButton, startTime);
        updateDateButtonText(endDateButton, endTime);
        updateTimeButtonText(startTimeButton, startTime);
        updateTimeButtonText(endTimeButton, endTime);

        posterBase64 = e.getPosterUrl();
        if (!TextUtils.isEmpty(posterBase64)) {
            loadImageFromBase64(posterBase64);
        }
    }

    private void createEvent() {
        if (!validateInputs()) {
            return;
        }

        Toast.makeText(getContext(), "Creating event...", Toast.LENGTH_SHORT).show();
        createEventWithPoster(posterBase64);
    }

    private void createEventWithPoster(String posterData) {
        Event newEvent = new Event();
        newEvent.setName(titleInput.getText().toString());
        newEvent.setDescription(descriptionInput.getText().toString());
        newEvent.setLocationName(locationInput.getText().toString());
        newEvent.setOrganizerID(organizer.getUserID());
        newEvent.setMaxAttendees(Integer.parseInt(entrantLimitInput.getText().toString()));
        newEvent.setRegistrationStart(startTime);
        newEvent.setRegistrationEnd(endTime);
        newEvent.setLotteryComplete(false);
        newEvent.setPosterUrl(posterData);

        newEvent.setGeolocationRequired(false);
        newEvent.setLocationCoordinates(null);
        newEvent.setMaxWaitingList(null);
        newEvent.setQrCodeData(null);
        newEvent.setWaitingList(new ArrayList<>());
        newEvent.setCreatedAt(Timestamp.now());
        newEvent.setUpdatedAt(Timestamp.now());

        eventController.createEvent(newEvent)
                .addOnSuccessListener(eventID -> {
                    Log.d(TAG, "SUCCESS! Event created with ID: " + eventID);
                    Toast.makeText(getContext(), "Event created!", Toast.LENGTH_SHORT).show();
                    if (navStack != null) {
                        navStack.popScreen();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "FAILED to create event", e);
                    Toast.makeText(getContext(), "Failed to create event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateEvent() {
        if (event == null || TextUtils.isEmpty(event.getEventID())) return;

        if (!validateInputs()) {
            return;
        }

        Toast.makeText(getContext(), "Updating event...", Toast.LENGTH_SHORT).show();

        String posterDataToSave;
        if (posterDeleted) {
            posterDataToSave = null;
        }
        else if (posterBase64 != null && !posterBase64.equals(event.getPosterUrl())) {
            posterDataToSave = posterBase64;
        }
        else {
            posterDataToSave = event.getPosterUrl();
        }

        updateEventWithPoster(posterDataToSave);
    }

    private void updateEventWithPoster(String posterData) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", titleInput.getText().toString());
        updates.put("description", descriptionInput.getText().toString());
        updates.put("locationName", locationInput.getText().toString());
        updates.put("maxAttendees", Integer.parseInt(entrantLimitInput.getText().toString()));
        updates.put("registrationStart", startTime);
        updates.put("registrationEnd", endTime);
        updates.put("posterUrl", posterData);

        eventController.updateEvent(event.getEventID(), updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Event updated!", Toast.LENGTH_SHORT).show();
                event.setPosterUrl(posterData);
                posterBase64 = posterData;

                if (navStack != null) {
                    navStack.popScreen();
                }
            }
            else {
                Toast.makeText(getContext(), "Failed to update: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showDatePicker(Button button, Timestamp current, DateSelectedCallback callback) {
        final Calendar calendar = Calendar.getInstance();
        if (current != null) calendar.setTime(current.toDate());

        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            calendar.set(year, month, day);
            callback.onDateSelected(new Timestamp(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(Button button, Timestamp current, TimeSelectedCallback callback) {
        final Calendar calendar = Calendar.getInstance();
        if (current != null) calendar.setTime(current.toDate());

        new TimePickerDialog(requireContext(), (timePicker, hour, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            callback.onTimeSelected(new Timestamp(calendar.getTime()));
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
    }

    private Timestamp mergeDateWithTime(Timestamp datePart, Timestamp timePart) {
        Calendar calendarDate = Calendar.getInstance();
        if (datePart != null) calendarDate.setTime(datePart.toDate());

        Calendar calendarTime = Calendar.getInstance();
        if (timePart != null) calendarTime.setTime(timePart.toDate());

        calendarDate.set(Calendar.HOUR_OF_DAY, calendarTime.get(Calendar.HOUR_OF_DAY));
        calendarDate.set(Calendar.MINUTE, calendarTime.get(Calendar.MINUTE));
        return new Timestamp(calendarDate.getTime());
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (TextUtils.isEmpty(titleInput.getText())) {
            titleInput.setError("Required");
            isValid = false;
        }
        if (TextUtils.isEmpty(locationInput.getText())) {
            locationInput.setError("Required");
            isValid = false;
        }
        if (TextUtils.isEmpty(entrantLimitInput.getText())) {
            entrantLimitInput.setError("Required");
            isValid = false;
        }
        else {
            try {
                int maxEntrants = Integer.parseInt(entrantLimitInput.getText().toString().trim());
                if (maxEntrants <= 0) {
                    entrantLimitInput.setError("Must be greater than 0");
                    isValid = false;
                }
            }
            catch (NumberFormatException e) {
                entrantLimitInput.setError("Invalid number");
                isValid = false;
            }
        }

        if (startTime == null) {
            Toast.makeText(getContext(), "Start date/time required", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        if (endTime == null) {
            Toast.makeText(getContext(), "End date/time required", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (startTime != null && endTime != null) {
            if (endTime.toDate().before(startTime.toDate())) {
                Toast.makeText(getContext(), "End date must be after start date", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
        }

        return isValid;
    }

    private void deleteEvent() {
        if (event == null || TextUtils.isEmpty(event.getEventID())) {
            Toast.makeText(getContext(), "Error: Missing eventID", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Event?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    String eventID = event.getEventID();

                    eventController.deleteEvent(eventID)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Event deleted from Firestore");

                                if (organizer != null) {
                                    organizer.removeOwnedEventID(eventID)
                                            .addOnSuccessListener(unused -> {
                                                Log.d(TAG, "Event removed from organizer's list");
                                                if (isAdded() && getContext() != null) {
                                                    Toast.makeText(getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
                                                    if (navStack != null) {
                                                        navStack.popScreen();
                                                    }
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Failed to remove from organizer list", e);
                                                if (isAdded() && getContext() != null) {
                                                    Toast.makeText(getContext(), "Event deleted but failed to update organizer list", Toast.LENGTH_SHORT).show();
                                                    if (navStack != null) {
                                                        navStack.popScreen();
                                                    }
                                                }
                                            });
                                }
                                else {
                                    if (isAdded() && getContext() != null) {
                                        Toast.makeText(getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
                                        if (navStack != null) {
                                            navStack.popScreen();
                                        }
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete event", e);
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            }
            else {
                Toast.makeText(getContext(), "Permission denied to read images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up listeners to prevent memory leaks
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
        if (invitationsListener != null) {
            invitationsListener.remove();
            invitationsListener = null;
        }
    }

    private interface TimeSelectedCallback {
        void onTimeSelected(Timestamp time);
    }

    private interface DateSelectedCallback {
        void onDateSelected(Timestamp date);
    }
}