package com.example.syzygy_eventapp;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;

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
 * <p>
 * Collaborators: Event, EventController
 */
public class EventFragment extends Fragment {
    private static final String TAG = "EventFragment";

    private final EventController eventController;

    private final NavigationStackFragment navStack;
    private final String eventID;
    private final InvitationController invitationController;

    // declare UI components
    private TextView eventNameText;
    private TextView eventDescriptionText;
    private TextView locationText;
    private TextView waitingListCountText;
    private TextView waitingListLimitText;
    private TextView registrationPeriodText;
    private TextView maxAttendeesText;
    private ImageView posterImage;
    private Button joinWaitingListButton;
    private Button leaveWaitingListButton;
    private Button openMapButton;
    private Button cancelAttendanceButton;

    private ListenerRegistration eventListener;
    private ListenerRegistration inviteListener;
    private Event currentEvent;
    private String userID;
    private boolean isOnWaitingList = false;
    private Invitation currentInvite;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            // just got permission
        } else {
            Toast.makeText(requireContext(),
                    "Notification permission was denied",
                    Toast.LENGTH_SHORT).show();
        }
    });

    /**
     * Constructor for EventFragment
     *
     * @param navStack Navigation stack for managing screens
     * @param eventID  The ID of the event to display
     */
    public EventFragment(NavigationStackFragment navStack, String eventID) {
        super();
        this.navStack = navStack;
        this.eventID = eventID;
        this.eventController = EventController.getInstance();
        this.invitationController = InvitationController.getInstance();
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

        // set a back button nav bar
        navStack.setScreenNavMenu(R.menu.back_nav_menu, item -> {
            if (item.getItemId() == R.id.back_nav_button) {
                navStack.popScreen();
                return true;
            }
            return false;
        });

        //start observing the event
        eventListener = eventController.observeEvent(eventID, this::onEventUpdated, navStack::popScreen);

        inviteListener = invitationController.observeUserEventInvite(
                eventID,
                userID,
                this::onInviteUpdated
        );
    }

    @Override
    public void onStop() {
        super.onStop();
        // stop observing when fragment is not visible
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }

        if (inviteListener != null) {
            inviteListener.remove();
            inviteListener = null;
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
        waitingListLimitText = view.findViewById(R.id.waiting_list_limit_text);
        registrationPeriodText = view.findViewById(R.id.registration_period_text);
        maxAttendeesText = view.findViewById(R.id.max_attendees_text);
        posterImage = view.findViewById(R.id.poster_image);
        joinWaitingListButton = view.findViewById(R.id.join_waiting_list_button);
        leaveWaitingListButton = view.findViewById(R.id.leave_waiting_list_button);
        openMapButton = view.findViewById(R.id.open_map_button);
        cancelAttendanceButton = view.findViewById(R.id.cancel_attendance_button);
    }

    /**
     * Set up button click listeners
     */
    private void setupButtonListeners() {
        joinWaitingListButton.setOnClickListener(v -> joinWaitingList());
        leaveWaitingListButton.setOnClickListener(v -> leaveWaitingList());
        openMapButton.setOnClickListener(v -> openLocationInMap());
        cancelAttendanceButton.setOnClickListener(v -> cancelAttendance());
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

        // Display geolocation requirement
        if (currentEvent.isGeolocationRequired()) {
            locationText.setText(currentEvent.getLocationName() + " (Location sharing required)");
        }

        // Display waiting list count
        int waitingListSize = currentEvent.getWaitingList() != null
                ? currentEvent.getWaitingList().size()
                : 0;

        // Update to show limit if it exists
        if (currentEvent.getMaxWaitingList() != null) {
            // If there is a limit, show it as x/max entrants
            waitingListCountText.setText("Waiting List: " + waitingListSize + "/" + currentEvent.getMaxWaitingList() + " entrants");
            waitingListLimitText.setText("Maximum Waiting List: " + currentEvent.getMaxWaitingList());
            waitingListLimitText.setVisibility(View.VISIBLE);
        } else {
            // No limit will just show the number of interested entrants
            waitingListCountText.setText("Waiting List: " + waitingListSize + " entrants");
            waitingListLimitText.setVisibility(View.GONE);
        }

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

        // Display poster image (now using Base64)
        loadPosterImage();

        // Update button states
        updateButtons();
    }

    /**
     * Load poster image from Base64 string, which needs to be decoded
     */
    private void loadPosterImage() {
        // Get the base64 string
        String posterData = currentEvent.getPosterUrl();

        if (posterData != null && !posterData.isEmpty()) {
            try {
                // Decode Base64 string to bitmap
                byte[] decodedBytes = Base64.decode(posterData, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                if (bitmap != null) {
                    posterImage.setImageBitmap(bitmap);
                    posterImage.setVisibility(View.VISIBLE);
                } else {
                    Log.e(TAG, "Failed to decode bitmap from Base64");
                    posterImage.setImageResource(R.drawable.image_placeholder);
                    posterImage.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading poster image", e);
                posterImage.setImageResource(R.drawable.image_placeholder);
                posterImage.setVisibility(View.VISIBLE);
            }
        } else {
            // No poster image, show placeholder
            posterImage.setImageResource(R.drawable.image_placeholder);
            posterImage.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Format registration period for display
     */
    private String formatRegistrationPeriod() {
        StringBuilder period = new StringBuilder("Registration: ");

        if (currentEvent.getRegistrationStart() != null) {
            period.append("Starts ").append(currentEvent.getRegistrationStart().toDate());
        }

        if (currentEvent.getRegistrationEnd() != null) {
            if (currentEvent.getRegistrationStart() != null) {
                period.append(" | ");
            }
            period.append("Ends ").append(currentEvent.getRegistrationEnd().toDate());
        }

        return period.toString();
    }

    /**
     * Update button visibility and enabled state based on
     * - waiting list membership
     * - invite status (pending / accepted / declined / cancelled)
     */
    private void updateButtons() {
        if (currentEvent == null) {
            return;
        }

        if (currentEvent.getWaitingList() != null) {
            isOnWaitingList = currentEvent.getWaitingList().contains(userID);
        } else {
            isOnWaitingList = false;
        }

        if (isOnWaitingList) {
            joinWaitingListButton.setVisibility(View.GONE);
            leaveWaitingListButton.setVisibility(View.VISIBLE);
        } else {
            joinWaitingListButton.setVisibility(View.VISIBLE);
            leaveWaitingListButton.setVisibility(View.GONE);
        }

        // Disable join if waiting list full
        if (currentEvent.getMaxWaitingList() != null &&
                currentEvent.getWaitingList() != null &&
                currentEvent.getWaitingList().size() >= currentEvent.getMaxWaitingList()) {
            joinWaitingListButton.setEnabled(false);
            joinWaitingListButton.setText("Waiting List Full");
        } else {
            joinWaitingListButton.setEnabled(true);
            joinWaitingListButton.setText("Join Waiting List");
        }

        // Pending invite: hide join/leave, no cancel attendance
        // Accepted invite: hide join/leave, show cancel attendance
        if (currentInvite != null) {
            Boolean cancelled = currentInvite.getCancelled();
            Boolean accepted = currentInvite.getAccepted();
            com.google.firebase.Timestamp responseTime = currentInvite.getResponseTime();

            boolean isCancelled = Boolean.TRUE.equals(cancelled);
            boolean isAccepted = Boolean.TRUE.equals(accepted);
            boolean hasResponded = responseTime != null;

            // Pending = not cancelled and no response yet
            if (!isCancelled && !hasResponded) {
                joinWaitingListButton.setVisibility(View.GONE);
                leaveWaitingListButton.setVisibility(View.GONE);
                cancelAttendanceButton.setVisibility(View.GONE);
                return;
            }

            // Accepted & not cancelled: show Cancel Attendance
            if (!isCancelled && isAccepted) {
                joinWaitingListButton.setVisibility(View.GONE);
                leaveWaitingListButton.setVisibility(View.GONE);
                cancelAttendanceButton.setVisibility(View.VISIBLE);
                cancelAttendanceButton.setEnabled(true);
                return;
            }
        }

        // Declined / cancelled / no invite: no cancel button
        cancelAttendanceButton.setVisibility(View.GONE);
    }

    /**
     * Join the event's waiting list
     */
    private void joinWaitingList() {
        // Check if geolocation is required for this event
        if (currentEvent != null && currentEvent.isGeolocationRequired()) {
            // Request location permission and get location before joining
            requestLocationAndJoin();
        } else {
            // No geolocation required, join directly
            performJoinWaitingList(null);
        }
    }

    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // already got permission
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                new AlertDialog.Builder(requireContext())
                        .setMessage("Notifications can alert you when you're chosen for an event. You may miss an invitation without them.")
                        .setTitle("Allow notifications?")
                        .setCancelable(false)
                        .setPositiveButton("Allow", (dialog, which) -> {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        })
                        .setNegativeButton("No thanks", (dialog, which) -> {
                        }).create().show();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /**
     * Request location permission and get user's location
     */
    private void requestLocationAndJoin() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
        } else {
            // Permission already granted, get location
            getUserLocationAndJoin();
        }
    }

    /**
     * Get user's current location and join waiting list
     */
    private void getUserLocationAndJoin() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(),
                    "Location permission required to join this event",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        performJoinWaitingList(geoPoint);
                    } else {
                        Toast.makeText(requireContext(),
                                "Unable to get location. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Failed to get location: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Join the event's waiting list
     */
    private void performJoinWaitingList(GeoPoint location) {
        joinWaitingListButton.setEnabled(false);

        eventController.addToWaitingList(eventID, userID, location)
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

        askNotificationPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocationAndJoin();
            } else {
                Toast.makeText(requireContext(),
                        "Location permission is required to join this event",
                        Toast.LENGTH_SHORT).show();
            }
        }
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
        if (currentEvent != null) {
            Maps.openInMaps(requireActivity(), currentEvent);
        }
    }

    private void onInviteUpdated(Invitation invite) {
        this.currentInvite = invite;
        if (!isAdded()) {
            return;
        }
        updateButtons();
    }

    /**
     * Cancel attendance for an accepted invitation.
     * This is equivalent to pressing "Decline" on the invite screen.
     */
    private void cancelAttendance() {
        if (currentInvite == null || currentInvite.getInvitation() == null) {
            if (isAdded()) {
                Toast.makeText(requireContext(),
                        "No invitation to cancel.",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        cancelAttendanceButton.setEnabled(false);

        invitationController.declineInvite(currentInvite.getInvitation())
                .addOnSuccessListener(updated -> {
                    if (!isAdded()) return;

                    if (Boolean.TRUE.equals(updated)) {
                        Toast.makeText(requireContext(),
                                "Attendance cancelled.",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                                "This invitation can no longer be changed.",
                                Toast.LENGTH_SHORT).show();
                    }
                    cancelAttendanceButton.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to cancel: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    cancelAttendanceButton.setEnabled(true);
                });
    }
}