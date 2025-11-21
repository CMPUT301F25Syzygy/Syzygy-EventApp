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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Fragment class that allows an organizer to create or edit an event's details.
 */
public class OrganizerEventEditDetailsFragment extends Fragment {

    private EditText titleInput, locationInput, entrantLimitInput, descriptionInput;
    private Button lotteryTimeButton, startTimeButton, endTimeButton, startDateButton, endDateButton;
    private Button importPosterButton, deletePosterButton;
    private ImageView posterPreview;
    private Button createButton, cancelButton, updateButton, deleteButton;
    private EventController eventController;
    private Organizer organizer;
    private Event event;
    private boolean isEditMode = false;
    private Timestamp lotteryTime, startTime, endTime;
    private NavigationStackFragment navStack;

    private static final String TAG = "OrganizerEventEdit";
    private TextView fragmentTitle;
    private Uri selectedImageUri;
    private String posterBase64;
    private boolean posterDeleted = false;
    private static final int MAX_IMAGE_SIZE = 800; // Max width/height in pixels

    // Activity result launcher for image picker
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public OrganizerEventEditDetailsFragment() {
        // Required empty public constructor
    }

    public static OrganizerEventEditDetailsFragment newInstance(@Nullable Event existingEvent, @NonNull Organizer organizer, @Nullable NavigationStackFragment navStack) {
        OrganizerEventEditDetailsFragment fragment = new OrganizerEventEditDetailsFragment();
        fragment.navStack = navStack;
        fragment.event = existingEvent;
        fragment.isEditMode = existingEvent != null;
        fragment.organizer = organizer;
        return fragment;
    }

    /**
     * Factory method for creating a new instance of this fragment
     *
     * @param existingEvent The event to edit (if any). If null, a new event will be created
     * @param organizer     The organizer creating or editing the event
     * @return A new instance of {@link OrganizerEventEditDetailsFragment}.
     */
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

        // Initialize image picker launcher
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


    /**
     * Inflates the layout for this fragment and initializes UI components
     * @return The root view of the fragment
     */
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

        eventController = new EventController();

        // Set the title based on mode (edit/create)
        if (isEditMode) {
            fragmentTitle.setText("Edit Event");
        }
        else {
            fragmentTitle.setText("Create Event");
        }

        setupButtonVisibility();
        setupListeners();

        if (isEditMode && event != null) {
            populateFields(event);
        }
        else {
            // Set the default button text for create mode
            updateDateButtonText(startDateButton, null);
            updateDateButtonText(endDateButton, null);
            updateTimeButtonText(startTimeButton, null);
            updateTimeButtonText(endTimeButton, null);
        }

        return view;
    }

    /**
     * Sets button visibility depending on whether the fragment is in create or edit mode
     */
    private void setupButtonVisibility() {
        if (isEditMode) {
            createButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            updateButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            createButton.setVisibility(View.VISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
            updateButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        }
    }

    /**
     * Listeners for buttons and UI
     */
    private void setupListeners() {
        // Image upload functionality
        importPosterButton.setOnClickListener(v -> openImagePicker());

        // Delete poster functionality
        deletePosterButton.setOnClickListener(v -> {
            selectedImageUri = null;
            posterBase64 = null;
            posterDeleted = true;
            posterPreview.setImageResource(R.drawable.image_placeholder);
            deletePosterButton.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Poster removed", Toast.LENGTH_SHORT).show();
        });

        // Registration period date/time pickers

        // Start date/time
        startDateButton.setOnClickListener(v -> showDatePicker(startDateButton, startTime, date -> {
            startTime = mergeDateWithTime(date, startTime);
            updateDateButtonText(startDateButton, startTime);
        }));

        startTimeButton.setOnClickListener(v -> showTimePicker(startTimeButton, startTime, time -> {
            startTime = mergeDateWithTime(startTime, time);
            updateTimeButtonText(startTimeButton, startTime);
        }));

        // End date/time
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
        cancelButton.setOnClickListener(v -> navStack.popScreen());
        deleteButton.setOnClickListener(v -> deleteEvent());
    }

    /**
     * Opens the system's image picker to let the organizer select a poster image.
     */
    private void openImagePicker() {
        // For Android 13+ (API 33+), use READ_MEDIA_IMAGES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
                return;
            }
        } else {
            // For older versions, use READ_EXTERNAL_STORAGE
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

    /**
     * Load and convert the selected image to Base64
     */
    private void loadAndConvertImage(Uri imageUri) {
        try {
            // Load the bitmap
            Bitmap bitmap;
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

            // Resize bitmap to reduce size
            Bitmap resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE);

            // Convert to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            posterBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            Log.d(TAG, "Image converted to Base64, size: " + posterBase64.length() + " characters");

            // Display in preview
            posterPreview.setImageBitmap(resizedBitmap);
            deletePosterButton.setVisibility(View.VISIBLE);
            posterDeleted = false;

            Toast.makeText(getContext(), "Image loaded successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            Toast.makeText(getContext(), "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Resize bitmap to fit within max dimensions while maintaining aspect ratio
     */
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

    /**
     * Load an image from Base64 string and display it in the preview
     */
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
        } catch (Exception e) {
            Log.e(TAG, "Failed to load image from Base64", e);
            posterPreview.setImageResource(R.drawable.image_placeholder);
            deletePosterButton.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the text on the date button to show the selected date instead.
     * @param button
     * @param timestamp
     */
    private void updateDateButtonText(Button button, Timestamp timestamp) {
        if (timestamp != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            button.setText(dateFormat.format(timestamp.toDate()));
        }
        else {
            // Show the placeholder text instead
            if (button.getId() == R.id.btnStartDate) {
                button.setText("Select Start Date");
            }
            else if (button.getId() == R.id.btnEndDate) {
                button.setText("Select End Date");
            }
        }
    }

    /**
     * Updates the text on the time button to show the selected time instead.
     * @param button
     * @param timestamp
     */
    private void updateTimeButtonText(Button button, Timestamp timestamp) {
        if (timestamp != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            button.setText(dateFormat.format(timestamp.toDate()));
        }
        else {
            // Show the placeholder text instead
            if (button.getId() == R.id.btnStartTime) {
                button.setText("Select Start Time");
            }
            else if (button.getId() == R.id.btnEndTime) {
                button.setText("Select End Time");
            }
        }
    }

    /**
     * Populates UI fields with an existing event's data when editing
     * @param e The {@link Event} object containing existing data.
     */
    private void populateFields(Event e) {
        titleInput.setText(e.getName());
        locationInput.setText(e.getLocationName());
        descriptionInput.setText(e.getDescription());
        entrantLimitInput.setText(e.getMaxAttendees() != null ? String.valueOf(e.getMaxAttendees()) : "");

        // Populate registration period
        startTime = e.getRegistrationStart();
        endTime = e.getRegistrationEnd();
        updateDateButtonText(startDateButton, startTime);
        updateDateButtonText(endDateButton, endTime);
        updateTimeButtonText(startTimeButton, startTime);
        updateTimeButtonText(endTimeButton, endTime);

        // Load existing poster if available
        posterBase64 = e.getPosterUrl(); // Now storing Base64 string in posterUrl field
        if (!TextUtils.isEmpty(posterBase64)) {
            loadImageFromBase64(posterBase64);
        }
    }

    /**
     * Validates user inputs, creates a new {@link Event}, and stores it in Firestore via {@link EventController}
     */
    private void createEvent() {
        if (!validateInputs()) {
            return;
        }

        Toast.makeText(getContext(), "Creating event...", Toast.LENGTH_SHORT).show();
        createEventWithPoster(posterBase64);
    }

    /**
     * Create the event object and save it to Firestore
     * @param posterData Base64 string of the poster image
     */
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
        newEvent.setPosterUrl(posterData); // Storing Base64 string in posterUrl field

        // explicit defaults
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

    /**
     * Updates an existing {@link Event} in Firestore with the modified input data
     */
    private void updateEvent() {
        if (event == null || TextUtils.isEmpty(event.getEventID())) return;

        if (!validateInputs()) {
            return;
        }

        Toast.makeText(getContext(), "Updating event...", Toast.LENGTH_SHORT).show();

        // Determine the poster data to save
        String posterDataToSave;
        if (posterDeleted) {
            posterDataToSave = null;
        } else if (posterBase64 != null && !posterBase64.equals(event.getPosterUrl())) {
            // New image selected
            posterDataToSave = posterBase64;
        } else {
            // No change to poster
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

                // Return to Organizer screen
                if (navStack != null) {
                    navStack.popScreen();
                }
            } else {
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

    /**
     * Validates all required text fields before performing create or update operations
     *
     * @return {@code true} if all required fields are filled; otherwise {@code false}
     */
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

        // Validate that the end time is after the start time
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

                    // Delete from Firestore
                    eventController.deleteEvent(eventID)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Event deleted from Firestore");

                                // Then remove from organizer's list
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
                                } else {
                                    // No organizer reference, just navigate away
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
            } else {
                Toast.makeText(getContext(), "Permission denied to read images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Interface for time selection callbacks
     */
    private interface TimeSelectedCallback {
        void onTimeSelected(Timestamp time);
    }

    private interface DateSelectedCallback {
        void onDateSelected(Timestamp date);
    }
}