package com.example.syzygy_eventapp;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.ListenerRegistration;
import com.squareup.picasso.Picasso;

/**
 * EventView - Displays event details for entrants.
 * Responsibilities:
 * - Display event name/description
 * - Display poster/image
 * - Display information about criteria and guidelines
 * - Display how many entrants are on waiting list
 * - Display location
 * - Open location in map software
 * - Join/leave waiting list
 *
 * Collaborators: Event, EventController
 */
public class EventFragment extends Fragment {
    private final EventController eventController;
    private final NavigationStackFragment navStack;
    private final String eventID;

    // declare UI components
    private TextView eventNameText;
    private TextView eventDescriptionText;
    private TextView locationText;
    private TextView waitingListCountText;
    private TextView registrationPeriodText;
    private TextView maxAttendeesText;
    private ImageView posterImage;
    private Button joinWaitingListButton;
    private Button leaveWaitingListButton;
    private Button openMapButton;

    private ListenerRegistration eventListener;
    private Event currentEvent;
    private String userID;
    private boolean isOnWaitingList = false;

    /**
     * Constructor for EventFragment
     * @param navStack Navigation stack for managing screens
     * @param eventID The ID of the event to display
     */
    public EventFragment(NavigationStackFragment navStack, String eventID) {
        super();
        this.navStack = navStack;
        this.eventID = eventID;
        this.eventController = new EventController();
    }

    // set up onCreate
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event, container, false);

        // Get user ID
        userID = AppInstallationId.get(requireContext());

        // Initialize UI components
        initializeViews(view);

        // Set up button listeners
        setupButtonListeners();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        //start observing the event
        eventListener = eventController.observeEvent(eventID, this::onEventUpdated, this::onError);
    }

    @Override
    public void onStop() {
        super.onStop();
        // stop observing when fragment is not visible
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
    }

    /**
     * Initialize all view components
     */
    private void initializeViews(View view) {
        eventNameText = view.findViewById(R.id.event_name_text);
        eventDescriptionText = view.findViewById(R.id.event_description_text);
        locationText = view.findViewById(R.id.location_text);
        waitingListCountText = view.findViewById(R.id.waiting_list_count_text);
        registrationPeriodText = view.findViewById(R.id.registration_period_text);
        maxAttendeesText = view.findViewById(R.id.max_attendees_text);
        posterImage = view.findViewById(R.id.poster_image);
        joinWaitingListButton = view.findViewById(R.id.join_waiting_list_button);
        leaveWaitingListButton = view.findViewById(R.id.leave_waiting_list_button);
        openMapButton = view.findViewById(R.id.open_map_button);
    }

    /**
     * Set up button click listeners
     */
    private void setupButtonListeners() {
        joinWaitingListButton.setOnClickListener(v -> joinWaitingList());
        leaveWaitingListButton.setOnClickListener(v -> leaveWaitingList());
        openMapButton.setOnClickListener(v -> openLocationInMap());
    }

    /**
     * Called when event data is updated from Firestore
     */
    private void onEventUpdated(Event event) {
        if (!isAdded()) return; // Safety check

        this.currentEvent = event;
        updateUI();
    }
    /**
     * Update the UI with current event data
     */
    private void updateUI() {
        if (currentEvent == null) return;

        // Display event name and description
        eventNameText.setText(currentEvent.getName());
        eventDescriptionText.setText(currentEvent.getDescription());

        // Display location
        if (currentEvent.getLocationName() != null) {
            locationText.setText(currentEvent.getLocationName());
            openMapButton.setVisibility(View.VISIBLE);
        } else {
            locationText.setText("Location not specified");
            openMapButton.setVisibility(View.GONE);
        }

        // Display waiting list count
        int waitingListSize = currentEvent.getWaitingList() != null
                ? currentEvent.getWaitingList().size()
                : 0;
        waitingListCountText.setText("Waiting List: " + waitingListSize + " entrants");

        // Display max attendees (lottery selection criteria)
        if (currentEvent.getMaxAttendees() != null) {
            maxAttendeesText.setText("Maximum Attendees: " + currentEvent.getMaxAttendees());
            maxAttendeesText.setVisibility(View.VISIBLE);
        } else {
            maxAttendeesText.setVisibility(View.GONE);
        }

        // Display registration period
        if (currentEvent.getRegistrationStart() != null || currentEvent.getRegistrationEnd() != null) {
            String period = formatRegistrationPeriod();
            registrationPeriodText.setText(period);
            registrationPeriodText.setVisibility(View.VISIBLE);
        } else {
            registrationPeriodText.setVisibility(View.GONE);
        }

        // Display poster image
        if (currentEvent.getPosterUrl() != null && !currentEvent.getPosterUrl().isEmpty()) {
            Picasso.get()
                    .load(currentEvent.getPosterUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(posterImage);
            posterImage.setVisibility(View.VISIBLE);
        } else {
            posterImage.setVisibility(View.GONE);
        }

        // Update button states based on whether user is on waiting list
        checkUserWaitingListStatus();
    }

    /**
     * Format registration period for display
     */
    private String formatRegistrationPeriod() {
        StringBuilder period = new StringBuilder("Registration: ");

        if (currentEvent.getRegistrationStart() != null) {
            period.append("Starts ").append(currentEvent.getRegistrationStart().toDate().toString());
        }

        if (currentEvent.getRegistrationEnd() != null) {
            if (currentEvent.getRegistrationStart() != null) {
                period.append(" | ");
            }
            period.append("Ends ").append(currentEvent.getRegistrationEnd().toDate().toString());
        }

        return period.toString();
    }

    /**
     * Check if current user is on the waiting list
     */
    private void checkUserWaitingListStatus() {
        if (currentEvent.getWaitingList() != null) {
            isOnWaitingList = currentEvent.getWaitingList().contains(userID);
        } else {
            isOnWaitingList = false;
        }

        // Update button visibility
        if (isOnWaitingList) {
            joinWaitingListButton.setVisibility(View.GONE);
            leaveWaitingListButton.setVisibility(View.VISIBLE);
        } else {
            joinWaitingListButton.setVisibility(View.VISIBLE);
            leaveWaitingListButton.setVisibility(View.GONE);
        }

        // Disable join button if waiting list is full
        if (currentEvent.getMaxWaitingList() != null &&
                currentEvent.getWaitingList() != null &&
                currentEvent.getWaitingList().size() >= currentEvent.getMaxWaitingList()) {
            joinWaitingListButton.setEnabled(false);
            joinWaitingListButton.setText("Waiting List Full");
        } else {
            joinWaitingListButton.setEnabled(true);
            joinWaitingListButton.setText("Join Waiting List");
        }
    }

    /**
     * Join the event's waiting list
     */
    private void joinWaitingList() {
        joinWaitingListButton.setEnabled(false);

        eventController.addToWaitingList(eventID, userID)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Successfully joined waiting list!",
                                Toast.LENGTH_SHORT).show();
                        joinWaitingListButton.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Failed to join: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        joinWaitingListButton.setEnabled(true);
                    }
                });
    }

    /**
     * Leave the event's waiting list
     */
    private void leaveWaitingList() {
        leaveWaitingListButton.setEnabled(false);

        eventController.removeFromWaitingList(eventID, userID)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Successfully left waiting list",
                                Toast.LENGTH_SHORT).show();
                        leaveWaitingListButton.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Failed to leave: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        leaveWaitingListButton.setEnabled(true);
                    }
                });
    }

    /**
     * Open the event location in map software (Google Maps)
     */
    private void openLocationInMap() {
        if (currentEvent == null || currentEvent.getLocationName() == null) {
            Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a URI for the location
        Uri mapUri;
        if (currentEvent.getLocationCoordinates() != null) {
            // Use coordinates if available for more accurate location
            double lat = currentEvent.getLocationCoordinates().getLatitude();
            double lng = currentEvent.getLocationCoordinates().getLongitude();
            mapUri = Uri.parse("geo:" + lat + "," + lng + "?q=" +
                    Uri.encode(currentEvent.getLocationName()));
        } else {
            // Fall back to address search
            mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(currentEvent.getLocationName()));
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        // Check if Google Maps is installed
        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fall back to web browser
            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" +
                            Uri.encode(currentEvent.getLocationName())));
            startActivity(webIntent);
        }
    }

    /**
     * Handle errors from Firestore
     */
    private void onError(Exception e) {
        if (isAdded()) {
            Toast.makeText(requireContext(),
                    "Error loading event: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }







}
