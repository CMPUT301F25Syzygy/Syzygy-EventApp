package com.example.syzygy_eventapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * OrganizerEventEditDetailsFragment
 *
 */
public class OrganizerEventEditDetailsFragment extends Fragment {

    private EditText titleInput, locationInput, entrantLimitInput, descriptionInput;
    private Button lotteryTimeButton, startTimeButton, endTimeButton;
    private Button importPosterButton;
    private Switch showContactSwitch;
    private ImageView posterPreview;

    private Button createButton, cancelButton, updateButton, deleteButton, revertButton;

    private EventController eventController;
    private Organizer organizer;
    private Event event;

    private boolean isEditMode = false;

    private Timestamp lotteryTime, startTime, endTime;

    public OrganizerEventEditDetailsFragment() {
        // Required empty public constructor
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_event, container, false);

        // --- Initialize Views ---
        titleInput = view.findViewById(R.id.edit_event_name);
        locationInput = view.findViewById(R.id.edit_location);
        descriptionInput = view.findViewById(R.id.edit_description);
        posterPreview = view.findViewById(R.id.edit_poster);
        importPosterButton = view.findViewById(R.id.btnUpload);

        //TBA
        //lotteryTimeButton = view.findViewById(R.id.btn_lottery_time);
        //startTimeButton = view.findViewById(R.id.btn_start_time);
        //endTimeButton = view.findViewById(R.id.btn_end_time);
        //entrantLimitInput = view.findViewById(R.id.edit_entrant_num);

        eventController = new EventController();

        setupButtonVisibility();
        setupListeners();

        if (isEditMode && event != null) {
            populateFields(event);
        }

        return view;
    }

    private void setupButtonVisibility() {
        if (isEditMode) {
            createButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            updateButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            revertButton.setVisibility(View.VISIBLE);
        } else {
            createButton.setVisibility(View.VISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
            updateButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
            revertButton.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        importPosterButton.setOnClickListener(v -> {
            // TODO: launch file chooser or image picker
            Toast.makeText(getContext(), "Import poster not implemented", Toast.LENGTH_SHORT).show();
        });

        lotteryTimeButton.setOnClickListener(v -> showDateTimePicker(time -> lotteryTime = time));
        startTimeButton.setOnClickListener(v -> showDateTimePicker(time -> startTime = time));
        endTimeButton.setOnClickListener(v -> showDateTimePicker(time -> endTime = time));

        createButton.setOnClickListener(v -> createEvent());
        updateButton.setOnClickListener(v -> updateEvent());
        deleteButton.setOnClickListener(v -> deleteEvent());
        revertButton.setOnClickListener(v -> populateFields(event));
        cancelButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void populateFields(Event e) {
        titleInput.setText(e.getName());
        locationInput.setText(e.getLocationName());
        entrantLimitInput.setText(e.getMaxAttendees() != null ? String.valueOf(e.getMaxAttendees()) : "");
        descriptionInput.setText(e.getDescription());
        showContactSwitch.setChecked(!e.isGeolocationRequired());
    }

    private void createEvent() {
        if (!validateInputs()) return;

        Event newEvent = new Event();
        newEvent.setName(titleInput.getText().toString());
        newEvent.setDescription(descriptionInput.getText().toString());
        newEvent.setLocationName(locationInput.getText().toString());
        newEvent.setOrganizerID(organizer.getUserID());
        newEvent.setMaxAttendees(Integer.parseInt(entrantLimitInput.getText().toString()));
        newEvent.setRegistrationStart(startTime);
        newEvent.setRegistrationEnd(endTime);
        newEvent.setLotteryComplete(false);

        eventController.createEvent(newEvent).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Event created!", Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
            } else {
                Toast.makeText(getContext(), "Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateEvent() {
        if (event == null || TextUtils.isEmpty(event.getEventID())) return;
        if (!validateInputs()) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", titleInput.getText().toString());
        updates.put("description", descriptionInput.getText().toString());
        updates.put("locationName", locationInput.getText().toString());
        updates.put("maxAttendees", Integer.parseInt(entrantLimitInput.getText().toString()));
        updates.put("registrationStart", startTime);
        updates.put("registrationEnd", endTime);

        eventController.updateEvent(event.getEventID(), updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Event updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to update: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteEvent() {
        if (event == null || TextUtils.isEmpty(event.getEventID())) return;
        eventController.deleteEvent(event.getEventID()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
            } else {
                Toast.makeText(getContext(), "Delete failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showDateTimePicker(TimeSelectionCallback callback) {
        final Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(getContext(), (view, year, month, day) -> {
            new TimePickerDialog(getContext(), (timePicker, hour, minute) -> {
                calendar.set(year, month, day, hour, minute);
                callback.onTimeSelected(new Timestamp(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(titleInput.getText())) {
            titleInput.setError("Required");
            return false;
        }
        if (TextUtils.isEmpty(locationInput.getText())) {
            locationInput.setError("Required");
            return false;
        }
        if (TextUtils.isEmpty(entrantLimitInput.getText())) {
            entrantLimitInput.setError("Required");
            return false;
        }
        return true;
    }

    private interface TimeSelectionCallback {
        void onTimeSelected(Timestamp time);
    }
}
