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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;

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
import java.util.concurrent.TimeUnit;

/**
 * Fragment class that allows an organizer to create or edit an event's details.
 * Provides functionality for managing event info, including dates, times, posters, and for viewing waiting lists or invitation statuses in real-time
 */
public class OrganizerEventEditDetailsFragment extends Fragment {
    private static final int MAX_IMAGE_SIZE = 800;

    // nav stack
    private final NavigationStackFragment navStack;

    // Event detail input fields
    private EditText titleInput, locationInput, entrantLimitInput, descriptionInput, maxWaitingListInput;
    private Button startTimeButton, endTimeButton, startDateButton, endDateButton;
    private Button importPosterButton, deletePosterButton;
    private ImageView posterPreview;
    private Button viewMapButton;
    private androidx.appcompat.widget.SwitchCompat geolocationToggle;

    // Controllers for Firebase operations
    private EventController eventController;
    private InvitationController invitationController;

    // Current user and event data
    private final String organizerID;
    private final Event event;
    private final boolean isEditMode;
    private Timestamp startTime, endTime;

    private static final String TAG = "OrganizerEventEdit";

    // Activity result launcher for image selection
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    /**
     * Creates a new instance of OrganizerEventEditDetailsFragment in create mode
     *
     * @param organizerID The organizer creating or editing the event
     * @param navStack    The nav stack for screen management
     */
    public OrganizerEventEditDetailsFragment(@NonNull String organizerID, @Nullable NavigationStackFragment navStack) {
        event = new Event();
        event.setOrganizerID(organizerID);

        this.isEditMode = false;
        this.organizerID = organizerID;
        this.navStack = navStack;
    }

    /**
     * Creates a new instance of OrganizerEventEditDetailsFragment in edit mode
     *
     * @param event    The event to edit
     * @param navStack The nav stack for screen management
     */
    public OrganizerEventEditDetailsFragment(@NonNull Event event, @Nullable NavigationStackFragment navStack) {
        this.event = event;
        this.isEditMode = true;
        this.organizerID = event.getOrganizerID();
        this.navStack = navStack;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
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
        TextView fragmentTitle = view.findViewById(R.id.edit_title);
        titleInput = view.findViewById(R.id.edit_event_name);
        locationInput = view.findViewById(R.id.edit_location);
        descriptionInput = view.findViewById(R.id.edit_description);
        posterPreview = view.findViewById(R.id.edit_poster);
        importPosterButton = view.findViewById(R.id.btnUpload);
        deletePosterButton = view.findViewById(R.id.btnDelete);
        entrantLimitInput = view.findViewById(R.id.max_entrants);
        maxWaitingListInput = view.findViewById(R.id.max_waiting_list);

        startDateButton = view.findViewById(R.id.btnStartDate);
        startTimeButton = view.findViewById(R.id.btnStartTime);
        endDateButton = view.findViewById(R.id.btnEndDate);
        endTimeButton = view.findViewById(R.id.btnEndTime);
        geolocationToggle = view.findViewById(R.id.geolocation_toggle);
        viewMapButton = view.findViewById(R.id.view_map_button);

        // Initialize controllers for Firebase operations
        eventController = EventController.getInstance();
        invitationController = new InvitationController();

        setupListeners();

        if (isEditMode) {
            fragmentTitle.setText("Edit Event");
            setupEditNavButtons();
            // In edit mode, populate the fields with EXISTING event data
            populateFields(event);
        } else {
            fragmentTitle.setText("Create Event");
            setupCreateNavButtons();
            // In create mode, set the default button text
            updateDateButtonText(startDateButton, null);
            updateDateButtonText(endDateButton, null);
            updateTimeButtonText(startTimeButton, null);
            updateTimeButtonText(endTimeButton, null);
        }

        return view;
    }

    /**
     * Configures button visibility based on whether we're in edit or create mode.
     * Edit mode shows the update + delete button combo, while create mode shows the create + cancel one.
     */
    private void setupEditNavButtons() {
        navStack.setScreenNavMenu(R.menu.edit_event_menu, (item) -> {
            if (item.getItemId() == R.id.undo_nav_button) {
                navStack.popScreen();
            } else if (item.getItemId() == R.id.delete_nav_button) {
                deleteEvent();
            } else if (item.getItemId() == R.id.save_nav_button) {
                // TODO
            }
            return true;
        });
    }

    /**
     * Configures button visibility based on whether we're in edit or create mode.
     * Edit mode shows the update + delete button combo, while create mode shows the create + cancel one.
     */
    private void setupCreateNavButtons() {
        navStack.setScreenNavMenu(R.menu.create_event_menu, (item) -> {
            if (item.getItemId() == R.id.cancel_nav_button) {
                navStack.popScreen();
            } else if (item.getItemId() == R.id.create_nav_button) {
                createEvent();
            }
            return true;
        });
    }


    /**
     * Sets up click listeners for all interactive UI elements
     */
    private void setupListeners() {
        // Poster import button that will open the image picker
        importPosterButton.setOnClickListener(v -> openImagePicker());

        // Poster delete button that will remove the poster
        deletePosterButton.setOnClickListener(v -> {
            event.setPosterUrl(null);
            posterPreview.setImageResource(R.drawable.image_placeholder);
        });

        // Date and time picker buttons
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

        // View map button
        if (viewMapButton != null) {
            viewMapButton.setOnClickListener(v -> {
                if (event != null && event.isGeolocationRequired() && navStack != null) {
                    WaitlistMapFragment mapFragment = new WaitlistMapFragment(event, navStack);
                    navStack.pushScreen(mapFragment);
                } else if (event != null && !event.isGeolocationRequired()) {
                    Toast.makeText(getContext(),
                            "Geolocation was not required for this event",
                            Toast.LENGTH_SHORT).show();
                }
            });

            // Show/hide based on geolocation requirement
            if (isEditMode && event != null) {
                viewMapButton.setVisibility(event.isGeolocationRequired() ? View.VISIBLE : View.GONE);
            } else {
                viewMapButton.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Validates input fields and creates a new event. Shows a toast message and initiates the creation process.
     */
    private void createEvent() {
        boolean isValid = updateLocalEventFromInputs();

        if (isValid) {
            Toast.makeText(getContext(), "Creating event...", Toast.LENGTH_SHORT).show();
            eventController.createEvent(event)
                    .addOnSuccessListener((eventId) -> {
                        navStack.popScreen();
                    })
                    .addOnFailureListener((exception) -> {
                        Log.e(TAG, "failed to create event", exception);
                    });
        }
    }


    /**
     * Displays a confirmation dialog and deleted the event if the organizer confirms.
     */
    private void deleteEvent() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete event")
                .setMessage("Are you sure you want to permanently delete this event? This cannot be undone.")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    eventController.deleteEvent(event.getEventID());
                    navStack.popScreen();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();

        // TODO: remove the event from the organizer's list of owned events.
    }


    /**
     * Displays a dialog showing a detailed list of entries with timestamps
     *
     * @param title   The title for the dialog
     * @param userIds The list fo userIDs to display
     * @param status  The status type for proper timestamp display
     */
    private void showUserListDialog(String title, List<String> userIds, String status) {
        if (userIds == null || userIds.isEmpty()) {
            Toast.makeText(getContext(), "No users in this list", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_waitlist_entries, null);
        builder.setView(dialogView);

        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        RecyclerView recyclerView = dialogView.findViewById(R.id.entries_recycler_view);
        TextView emptyView = dialogView.findViewById(R.id.empty_text);

        titleView.setText(title);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<WaitlistEntry> entries = new ArrayList<>();
        WaitlistEntryAdapter adapter = new WaitlistEntryAdapter(entries);
        recyclerView.setAdapter(adapter);

        // Load entries with timestamps
        loadWaitlistEntries(userIds, status, entries, adapter, recyclerView, emptyView);

        AlertDialog dialog = builder.create();
        dialogView.findViewById(R.id.close_button).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadWaitlistEntries(List<String> userIds, String status, List<WaitlistEntry> entries, WaitlistEntryAdapter adapter, RecyclerView recyclerView, TextView emptyView) {
        UserControllerInterface userController = UserController.getInstance();

        // no users for the status, so show empty state message
        if (userIds == null || userIds.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(getEmptyMessage(status));
            return;
        }

        // If we have users, show the recycler view and hide empty message
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        // Waiting list users don't have timestamps
        if ("waiting".equals(status)) {
            for (String userId : userIds) {
                // Loaiding state
                WaitlistEntry entry = new WaitlistEntry(userId, "Loading...", null, status);
                entries.add(entry);

                // Load user details
                userController.getUser(userId).addOnSuccessListener(user -> {
                    if (user != null) {
                        int index = entries.indexOf(entry);
                        if (index != -1) {
                            entry.setUserName(user.getName());
                            adapter.notifyItemChanged(index);
                        }
                    }
                }).addOnFailureListener(e -> {
                    // Fallback: show the userID is user details cannot be retrieved
                    int index = entries.indexOf(entry);
                    if (index != -1) {
                        entry.setUserName(userId);
                        adapter.notifyItemChanged(index);
                    }
                });
            }
            return;
        }

        // For invitation-based statuses, get the timestamps from invitation data via Firestore
        invitationController.observeEventInvites(event.getEventID(), invitations -> {
            // Create a map of userID and invitation for quick lookup
            Map<String, Invitation> invitationMap = new HashMap<>();
            for (Invitation invitation : invitations) {
                invitationMap.put(invitation.getRecipientID(), invitation);
            }

            // Create an entry for each user and attach the relevant timestamps
            for (String userId : userIds) {
                Invitation invitation = invitationMap.get(userId);
                WaitlistEntry entry = new WaitlistEntry(userId, "Loading...", null, status);

                // Set appropriate timestamp based on status
                if (invitation != null) {
                    switch (status) {
                        case "accepted":
                            entry.setRegistrationDate(invitation.getResponseTime());
                            break;

                        case "pending":
                            entry.setJoinedAt(invitation.getSendTime());
                            break;

                        case "cancelled":
                            entry.setCancellationDate(invitation.getCancelTime());
                            break;

                        case "rejected":
                            entry.setRegistrationDate(invitation.getResponseTime());
                            break;
                    }
                }

                entries.add(entry);

                // Load user details
                userController.getUser(userId).addOnSuccessListener(user -> {
                    if (user != null) {
                        int index = entries.indexOf(entry);
                        if (index != -1) {
                            entry.setUserName(user.getName());

                            // Sort and refresh (ensure a sorted order ehenever new info is given)
                            sortEntries(entries, status);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }).addOnFailureListener(e -> {
                    // Fallback: uuse the username if details cannot be loaded
                    int index = entries.indexOf(entry);
                    if (index != -1) {
                        entry.setUserName(userId);
                        adapter.notifyItemChanged(index);
                    }
                });
            }
        });
    }

    /**
     * Sorts entries by timestamp, based on status, with earliest times first.
     * Each status category uses its own timestamp field (sendTime, responseTime, etc.)
     */
    private void sortEntries(List<WaitlistEntry> entries, String status) {
        entries.sort((e1, e2) -> {
            Timestamp t1 = null;
            Timestamp t2 = null;

            switch (status) {
                case "accepted":

                case "rejected":
                    t1 = e1.getRegistrationDate();
                    t2 = e2.getRegistrationDate();
                    break;

                case "pending":
                    t1 = e1.getJoinedAt();
                    t2 = e2.getJoinedAt();
                    break;

                case "cancelled":
                    t1 = e1.getCancellationDate();
                    t2 = e2.getCancellationDate();
                    break;
            }

            // Entries with missing timestamps get put in the bottom
            if (t1 == null) return 1;
            if (t2 == null) return -1;

            // Earliest first (which translates to ascending order)
            return t1.compareTo(t2);
        });
    }

    /**
     * Returns appropriate empty message based on list type
     */
    private String getEmptyMessage(String status) {
        switch (status) {
            case "waiting":
                return "No entrants on the waitlist";

            case "cancelled":
                return "No entrants have cancelled this event";

            case "accepted":
                return "No entrants have been accepted yet";

            case "pending":
                return "No pending invitations";

            case "rejected":
                return "No entrants have rejected yet";

            default:
                return "No users in this list";
        }
    }

    /**
     * Opens the device's image picker to allow the user to select a poster image.
     * It will request the needed permissions based on Android's version before opening.
     */
    private void openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                return;
            }
        }

        // Launch the image picker intent
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
    private void loadAndConvertImage(Uri imageUri) {
        try {
            Bitmap bitmap;
            // Use the appropriate emthod based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(
                        requireContext().getContentResolver(), imageUri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
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

            // Resize the bitmap to reduce file size
            Bitmap resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE);

            // Compress and convert to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            event.setPosterUrl(Base64.encodeToString(imageBytes, Base64.DEFAULT));

            // Update the UI
            posterPreview.setImageBitmap(resizedBitmap);
            deletePosterButton.setVisibility(View.VISIBLE);

            Toast.makeText(getContext(), "Image loaded successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception error) {
            Log.e(TAG, "Error loading image", error);
            Toast.makeText(getContext(), "Failed to load image: " + error.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Resizes a bitmap to fit within the specified max. dimensions while maintaining aspect ratio
     *
     * @param bitmap  The original bitmap to resize
     * @param maxSize The maximum width or height in pixels
     * @return The resized bitmpa, or the original if already smaller than maxSize
     */
    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Return original if it's already small
        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Updates the text on a date button to display the formatted date. If the timestamp is null, it will show the default text.
     *
     * @param button    The button to update
     * @param timestamp The timestamp containign the data to display
     */
    private void updateDateButtonText(Button button, Timestamp timestamp) {
        if (timestamp != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            button.setText(dateFormat.format(timestamp.toDate()));
        } else {
            // Set default text based on which button it is
            if (button.getId() == R.id.btnStartDate) {
                button.setText("Pick Start Date");
            } else if (button.getId() == R.id.btnEndDate) {
                button.setText("Pick End Date");
            }
        }
    }

    /**
     * Updates the text on a time button to display the formatted time. If the timestamp is null, it will show the default text.
     *
     * @param button    The button to update
     * @param timestamp The timestamp containing the time to display
     */
    private void updateTimeButtonText(Button button, Timestamp timestamp) {
        if (timestamp != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            button.setText(dateFormat.format(timestamp.toDate()));
        } else {
            // Set default text based on which button it is
            if (button.getId() == R.id.btnStartTime) {
                button.setText("Pick Start Time");
            } else if (button.getId() == R.id.btnEndTime) {
                button.setText("Pick End Time");
            }
        }
    }

    /**
     * Populates all input fields with data from a given event. Used when editing an event
     *
     * @param event The event whose data will be displayed
     */
    private void populateFields(Event event) {
        titleInput.setText(event.getName());
        locationInput.setText(event.getLocationName());
        descriptionInput.setText(event.getDescription());
        entrantLimitInput.setText(event.getMaxAttendees() != null ? String.valueOf(event.getMaxAttendees()) : "");
        maxWaitingListInput.setText(event.getMaxWaitingList() != null ? String.valueOf(event.getMaxWaitingList()) : "");

        // Set date/time values
        startTime = event.getRegistrationStart();
        endTime = event.getRegistrationEnd();
        updateDateButtonText(startDateButton, startTime);
        updateDateButtonText(endDateButton, endTime);
        updateTimeButtonText(startTimeButton, startTime);
        updateTimeButtonText(endTimeButton, endTime);

        // Load the poster is available
        Bitmap bitmap = event.getPosterBitmap();

        if (bitmap == null) {
            posterPreview.setImageResource(R.drawable.image_placeholder);
        } else {
            posterPreview.setImageBitmap(bitmap);
        }

        // Set the geolocation toggle state
        geolocationToggle.setChecked(event.isGeolocationRequired());

        // Disable geolocation toggle in edit mode (cannot be changed after creation)
        if (isEditMode) {
            geolocationToggle.setEnabled(false);
            geolocationToggle.setAlpha(0.6f);
        }


    }

    /**
     * Creates a new event object with the provided poster data abd saves it to Firestore.
     * Navigates back to the OrganizerFragment on success, and shows an error message on failure.
     *
     * @param posterData The Base64 encoded poster image, which is null is there is no poster
     */
    private void createEventWithPoster(String posterData) {
        // Builf a new event object from input fields
        Event newEvent = new Event();
        newEvent.setName(titleInput.getText().toString());
        newEvent.setDescription(descriptionInput.getText().toString());
        newEvent.setLocationName(locationInput.getText().toString());
        newEvent.setOrganizerID(organizerID);
        newEvent.setMaxAttendees(Integer.parseInt(entrantLimitInput.getText().toString()));
        newEvent.setRegistrationStart(startTime);
        newEvent.setRegistrationEnd(endTime);
        newEvent.setLotteryComplete(false);
        newEvent.setPosterUrl(posterData);

        // Max waiting list
        String maxWaitingListStr = maxWaitingListInput.getText().toString().trim();
        if (!maxWaitingListStr.isEmpty()) {
            newEvent.setMaxWaitingList(Integer.parseInt(maxWaitingListStr));
        } else {
            // Keep as null, which means unlimited people can enter the waiting list
            newEvent.setMaxWaitingList(null);
        }

        // Set geolocation requirement from toggle
        newEvent.setGeolocationRequired(geolocationToggle.isChecked());

        // Set default values for optional fields
        // TODO: Change these if/when they are implemented based on the organizer's choices.
        newEvent.setLocationCoordinates(null);
        newEvent.setQrCodeData(null);
        newEvent.setWaitingList(new ArrayList<>());
        newEvent.setCreatedAt(Timestamp.now());
        newEvent.setUpdatedAt(Timestamp.now());

        // Save to Firestore
        eventController.createEvent(newEvent)
                .addOnSuccessListener(eventID -> {
                    Log.d(TAG, "SUCCESS! Event created with ID: " + eventID);
                    Toast.makeText(getContext(), "Event created!", Toast.LENGTH_SHORT).show();
                    if (navStack != null) {
                        navStack.popScreen();
                    }
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "FAILED to create event", error);
                    Toast.makeText(getContext(), "Failed to create event: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean updateLocalEventFromInputs() {
        boolean isValid = true;

        // validate title
        if (titleInput.getText().toString().isEmpty()) {
            titleInput.setError("Title is required");
            isValid = false;
        } else {
            event.setName(titleInput.getText().toString());
        }

        event.setDescription(descriptionInput.getText().toString());

        // validate location
        if (locationInput.getText().toString().isEmpty()) {
            locationInput.setError("Location is required");
            isValid = false;
        } else {
            event.setLocationName(locationInput.getText().toString());
        }

        // Validate entrant limit
        if (entrantLimitInput.getText().toString().isEmpty()) {
            entrantLimitInput.setError("Entrant limit is required");
            isValid = false;
        } else {
            try {
                int maxEntrants = Integer.parseInt(entrantLimitInput.getText().toString());
                if (maxEntrants <= 0) {
                    entrantLimitInput.setError("Must be greater than 0");
                    isValid = false;
                } else {
                    event.setMaxAttendees(maxEntrants);
                }
            } catch (NumberFormatException e) {
                entrantLimitInput.setError("Invalid number");
                isValid = false;
            }
        }

        // Validate start date and time
        if (startTime == null) {
            Toast.makeText(getContext(), "Start date/time required", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        // Validate end date and time
        if (endTime == null) {
            Toast.makeText(getContext(), "End date/time required", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Validate that end time is AFTER the start time
        if (startTime != null && endTime != null) {
            if (endTime.toDate().before(startTime.toDate())) {
                Toast.makeText(getContext(), "End date must be after start date", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
        }

        event.setRegistrationStart(startTime);
        event.setRegistrationEnd(endTime);

        if (maxWaitingListInput.getText().toString().isEmpty()) {
            event.setMaxWaitingList(null);
        } else {
            try {
                int maxWaiting = Integer.parseInt(maxWaitingListInput.getText().toString());
                if (maxWaiting <= 0) {
                    maxWaitingListInput.setError("Must be greater than 0");
                    isValid = false;
                } else {
                    event.setMaxWaitingList(maxWaiting);
                }
            } catch (NumberFormatException e) {
                maxWaitingListInput.setError("Invalid number");
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Displays a date picker dialog and invokes the callbakc with the selected date
     *
     * @param button   The button that triggeref the picker
     * @param current  The current timestamp to initialize the picker, null for today
     * @param callback The callback to invoke with the selected date
     */
    private void showDatePicker(Button button, Timestamp current, DateSelectedCallback callback) {
        final Calendar calendar = Calendar.getInstance();
        if (current != null) calendar.setTime(current.toDate());

        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            calendar.set(year, month, day);
            callback.onDateSelected(new Timestamp(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Displays a time picker dialog and invokes the callback with the selected time
     *
     * @param button   The button that triggered the picker
     * @param current  The current timestamp to initialize the picker, null if now
     * @param callback The callback to invoke iwth the selected time
     */
    private void showTimePicker(Button button, Timestamp current, TimeSelectedCallback callback) {
        final Calendar calendar = Calendar.getInstance();
        if (current != null) calendar.setTime(current.toDate());

        new TimePickerDialog(requireContext(), (timePicker, hour, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            callback.onTimeSelected(new Timestamp(calendar.getTime()));
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
    }

    /**
     * Merges the date from one timestamp with the time from another timestamp.
     * Used to combine seperately selected date and time values.
     *
     * @param datePart The timestamp containing the desired date
     * @param timePart The timestamp containing the desired time
     * @return A new timestamp with the combined date and time
     */
    private Timestamp mergeDateWithTime(Timestamp datePart, Timestamp timePart) {
        Calendar calendarDate = Calendar.getInstance();
        if (datePart != null) calendarDate.setTime(datePart.toDate());

        Calendar calendarTime = Calendar.getInstance();
        if (timePart != null) calendarTime.setTime(timePart.toDate());

        // Copy time components from timePart to calendarDate
        calendarDate.set(Calendar.HOUR_OF_DAY, calendarTime.get(Calendar.HOUR_OF_DAY));
        calendarDate.set(Calendar.MINUTE, calendarTime.get(Calendar.MINUTE));
        return new Timestamp(calendarDate.getTime());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open the image picker
                openImagePicker();
            } else {
                Toast.makeText(getContext(), "Permission denied to read images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Callback interface for time selection from TimePickerDialog
     */
    private interface TimeSelectedCallback {
        void onTimeSelected(Timestamp time);
    }

    /**
     * Callback interface for date selection from DatePickerDialog
     */
    private interface DateSelectedCallback {
        void onDateSelected(Timestamp date);
    }
}