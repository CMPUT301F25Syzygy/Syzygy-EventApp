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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

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

    // Current user and event data
    private final Event event;
    private final boolean isEditMode;
    private Timestamp startTime, endTime;

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
                deleteEvent(navStack::popScreen);
            } else if (item.getItemId() == R.id.save_nav_button) {
                updateEvent(navStack::popScreen);
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
                createEvent(navStack::popScreen);
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

        if (!isEditMode) {
            geolocationToggle.setOnClickListener((view) -> {
                if (geolocationToggle.isChecked()) {
                    viewMapButton.setVisibility(View.GONE);
                } else {
                    viewMapButton.setVisibility(View.GONE);
                }
            });
        }

        // View map button (works in both modes but only for preview during creation)
        viewMapButton.setOnClickListener(v -> {
            WaitlistMapFragment mapFragment = new WaitlistMapFragment(event, navStack);
            navStack.pushScreen(mapFragment);
        });

    }

    /**
     * Validates input fields and creates a new event. Shows a toast message and initiates the creation process.
     */
    private void createEvent(Runnable callback) {
        boolean isValid = updateLocalEventFromInputs();

        if (isValid) {
            Toast.makeText(getContext(), "Creating event...", Toast.LENGTH_SHORT).show();
            eventController.createEvent(event)
                    .addOnSuccessListener((eventId) -> {
                        callback.run();
                    })
                    .addOnFailureListener((exception) -> {
                        Toast.makeText(getContext(), "Failed to create event", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateEvent(Runnable callback) {
        boolean isValid = updateLocalEventFromInputs();

        if (isValid) {
            Toast.makeText(getContext(), "Updating event...", Toast.LENGTH_SHORT).show();
            eventController.updateEvent(event)
                    .addOnSuccessListener((eventId) -> {
                        callback.run();
                    })
                    .addOnFailureListener((exception) -> {
                        Toast.makeText(getContext(), "Failed to update event", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Displays a confirmation dialog and deleted the event if the organizer confirms.
     */
    private void deleteEvent(Runnable callback) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete event")
                .setMessage("Are you sure you want to permanently delete this event? This cannot be undone.")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    eventController.deleteEvent(event.getEventID());
                    callback.run();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();

        // TODO: remove the event from the organizer's list of owned events.
        // JUST NOT HERE, THAT IS A JOB FOR THE EVENT CONTROLLER
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
        } catch (Exception error) {
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
        Bitmap bitmap = event.generatePosterBitmap();

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

        event.setGeolocationRequired(geolocationToggle.isChecked());

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