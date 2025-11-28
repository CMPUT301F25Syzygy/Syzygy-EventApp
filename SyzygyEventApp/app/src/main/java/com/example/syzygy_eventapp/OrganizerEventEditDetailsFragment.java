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

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
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
 * Provides functionality for managing event info, including dates, times, posters, and for viewing waiting lists or invitation statuses in real-time
 */
public class OrganizerEventEditDetailsFragment extends Fragment {

    private View rootView;

    // Event detail input fields
    private EditText titleInput, locationInput, entrantLimitInput, descriptionInput, maxWaitingListInput;
    private Button startTimeButton, endTimeButton, startDateButton, endDateButton;
    private Button importPosterButton, deletePosterButton;
    private ImageView posterPreview;
    private Button createButton, cancelButton, updateButton, deleteButton, generateQRButton, viewMapButton;
    private androidx.appcompat.widget.SwitchCompat geolocationToggle;

    // Controllers for Firebase operations
    private EventController eventController;
    private InvitationController invitationController;

    // Current user and event data
    private Organizer organizer;
    private Event event;
    private boolean isEditMode = false;
    private boolean isViewMode = false;
    private Timestamp startTime, endTime;
    private NavigationStackFragment navStack;

    private static final String TAG = "OrganizerEventEdit";
    private TextView fragmentTitle;

    // Image handling vars
    private Uri selectedImageUri;
    private String posterBase64;
    private boolean posterDeleted = false;
    private static final int MAX_IMAGE_SIZE = 800;

    // Waiting list UI elements
    private TextView acceptedCountText, pendingCountText, waitingCountText, rejectedCountText, cancelledCountText;
    private LinearLayout acceptedLabelLayout, pendingLabelLayout, waitingLabelLayout, rejectedLabelLayout, cancelledLabelLayout;
    private ListView listAccepted, listPending, listWaiting, listRejected, listCancelled;

    // Real-time data lists for different invitation statuses
    private List<String> waitingListUsers = new ArrayList<>();
    private List<String> acceptedUsers = new ArrayList<>();
    private List<String> pendingUsers = new ArrayList<>();
    private List<String> rejectedUsers = new ArrayList<>();
    private List<String> cancelledUsers = new ArrayList<>();

    // Firestore listeners for real-time updates
    private ListenerRegistration eventListener;
    private ListenerRegistration invitationsListener;

    // Activity result launcher for image selection
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    /**
     * Default constructor
     */
    public OrganizerEventEditDetailsFragment() {
    }

    /**
     * Creates a new instance of the fragment with nav stack support
     *
     * @param existingEvent The event to edit, or null to create a new event
     * @param organizer The organizer creating or editing the event
     * @param navStack The nav stack for screen management
     * @return A new instance of OrganizerEventEditDetailsFragment
     */
    public static OrganizerEventEditDetailsFragment newInstance(@Nullable Event existingEvent, @NonNull Organizer organizer, @Nullable NavigationStackFragment navStack) {
        OrganizerEventEditDetailsFragment fragment = new OrganizerEventEditDetailsFragment();
        fragment.navStack = navStack;
        fragment.event = existingEvent;
        fragment.isEditMode = existingEvent != null;
        fragment.organizer = organizer;
        return fragment;
    }

    /**
     * A new instance of this fragment in VIEW-ONLY mode.
     * To be used so the organizer can retain the ability to view old events and their entrant info, without being able to edit it.
     *
     * @param existingEvent The event to view organizer details for
     * @param organizer The organizer creating or editing the event
     * @param navStack The nav stack for screen management
     * @return A new instance of OrganizerEventEditDetailsFragment
     */
    public static OrganizerEventEditDetailsFragment newInstanceViewOnly(@Nullable Event existingEvent, @NonNull Organizer organizer, @Nullable NavigationStackFragment navStack) {
        OrganizerEventEditDetailsFragment fragment = new OrganizerEventEditDetailsFragment();
        fragment.navStack = navStack;
        fragment.event = existingEvent;
        // Set to true so it shows waiting lists
        fragment.isEditMode = true;
        // Set view-only mode
        fragment.isViewMode = true;
        fragment.organizer = organizer;
        return fragment;
    }

    /**
     * Creates a new instance of the fragment with args bundle
     *
     * @param existingEvent The event to edit, or null to create a new event
     * @param organizer The organizer creating or editing the event
     * @return A new instance of OrganizerEventEditDetailsFragment
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
        maxWaitingListInput = view.findViewById(R.id.max_waiting_list);

        startDateButton = view.findViewById(R.id.btnStartDate);
        startTimeButton = view.findViewById(R.id.btnStartTime);
        endDateButton = view.findViewById(R.id.btnEndDate);
        endTimeButton = view.findViewById(R.id.btnEndTime);
        generateQRButton = view.findViewById(R.id.generate_qr_button);
        geolocationToggle = view.findViewById(R.id.geolocation_toggle);
        viewMapButton = view.findViewById(R.id.view_map_button);

        // Initialize waiting list UI elements
        acceptedCountText = view.findViewById(R.id.accepted_count);
        pendingCountText = view.findViewById(R.id.pending_count);
        waitingCountText = view.findViewById(R.id.Waiting_count);
        rejectedCountText = view.findViewById(R.id.rejected_count);
        cancelledCountText = view.findViewById(R.id.cancelled_count);

        // Get parent layouts for label sections (so we can handle clicks)
        acceptedLabelLayout = view.findViewById(R.id.tvAcceptedLabel).getParent() instanceof LinearLayout ? (LinearLayout) view.findViewById(R.id.tvAcceptedLabel).getParent() : null;
        pendingLabelLayout = view.findViewById(R.id.tvPendingLabel).getParent() instanceof LinearLayout ? (LinearLayout) view.findViewById(R.id.tvPendingLabel).getParent() : null;
        waitingLabelLayout = view.findViewById(R.id.WaitingLabel).getParent() instanceof LinearLayout ? (LinearLayout) view.findViewById(R.id.WaitingLabel).getParent() : null;
        rejectedLabelLayout = view.findViewById(R.id.RejectedLabel).getParent() instanceof LinearLayout ? (LinearLayout) view.findViewById(R.id.RejectedLabel).getParent() : null;
        cancelledLabelLayout = view.findViewById(R.id.CancelledLabel).getParent() instanceof LinearLayout ? (LinearLayout) view.findViewById(R.id.CancelledLabel).getParent() : null;

        listAccepted = view.findViewById(R.id.listAccepted);
        listPending = view.findViewById(R.id.listPending);
        listWaiting = view.findViewById(R.id.listWaiting);
        listRejected = view.findViewById(R.id.listRejected);
        listCancelled = view.findViewById(R.id.listCancelled);

        // Initialize controllers for Firebase operations
        eventController = EventController.getInstance();
        invitationController = new InvitationController();

        // Set the appropriate title based on mode
        if (isViewMode) {
            fragmentTitle.setText("View Event");
        }
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
            // In edit mode, populate the fields with EXISTING event data
            populateFields(event);
            startRealtimeListeners();

            // Disable all inputs if we're in view mode
            if (isViewMode) {
                disableAllInputs();
            }

            // Seed test data
            //createFakeUsers();

            // Wait 2 seconds for users to be created, then seed invitations
            //new android.os.Handler().postDelayed(() -> {
            //    seedTestInvitations();
            //}, 2000);
        }
        else {
            // In creatre mode, set the default button text
            updateDateButtonText(startDateButton, null);
            updateDateButtonText(endDateButton, null);
            updateTimeButtonText(startTimeButton, null);
            updateTimeButtonText(endTimeButton, null);

            // Hide waiting list sections in create mode
            hideWaitingListSections();
        }

        return view;
    }

    /**
     * Hides all waiting list sections in the UI. Used when creating a new event where no waiting lists exist yet.
     */
    private void hideWaitingListSections() {
        if (acceptedLabelLayout != null) acceptedLabelLayout.setVisibility(View.GONE);
        if (pendingLabelLayout != null) pendingLabelLayout.setVisibility(View.GONE);
        if (waitingLabelLayout != null) waitingLabelLayout.setVisibility(View.GONE);
        if (rejectedLabelLayout != null) rejectedLabelLayout.setVisibility(View.GONE);
        if (cancelledLabelLayout != null) cancelledLabelLayout.setVisibility(View.GONE);

        if (listAccepted != null) listAccepted.setVisibility(View.GONE);
        if (listPending != null) listPending.setVisibility(View.GONE);
        if (listWaiting != null) listWaiting.setVisibility(View.GONE);
        if (listRejected != null) listRejected.setVisibility(View.GONE);
        if (listCancelled != null) listCancelled.setVisibility(View.GONE);
    }

    /**
     * Configures button visibility based on whether we're in edit or create mode.
     * Edit mode shows the update + delete button combo, while create mode shows the create + cancel one.
     */
    private void setupButtonVisibility() {
        // View only mode has to be added, where all action buttons are hidden, and I'll use the back button from the navStack since that's the only one we need
        if (isViewMode) {
            createButton.setVisibility(View.GONE);
            updateButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
            generateQRButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.VISIBLE);
            cancelButton.setText("Back");
            // Override the cancel button to use navStack
            cancelButton.setOnClickListener(v -> {
                if (navStack != null) {
                    navStack.popScreen();
                }
            });
        }
        if (isEditMode) {
            createButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            updateButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            generateQRButton.setVisibility(View.VISIBLE);
        }
        else {
            createButton.setVisibility(View.VISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
            updateButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
            generateQRButton.setVisibility(View.GONE);
        }
    }

    /**
     * Sets up click listeners for all interactive UI elements
     */
    private void setupListeners() {
        // Poster import button that will open the image picker
        importPosterButton.setOnClickListener(v -> openImagePicker());

        // Poster delete button that will remove the poster
        deletePosterButton.setOnClickListener(v -> {
            selectedImageUri = null;
            posterBase64 = null;
            posterDeleted = true;
            posterPreview.setImageResource(R.drawable.image_placeholder);
            deletePosterButton.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Poster removed", Toast.LENGTH_SHORT).show();
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

        // QR Code generate button
        generateQRButton.setOnClickListener(v -> {
            if (event != null && navStack != null) {
                QRGenerateFragment qrFragment = new QRGenerateFragment(event, navStack);
                navStack.pushScreen(qrFragment);
            }
            else {
                Toast.makeText(getContext(), "Please save the event first", Toast.LENGTH_SHORT).show();
            }
        });

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

        // Action buttons
        createButton.setOnClickListener(v -> createEvent());
        updateButton.setOnClickListener(v -> updateEvent());
        cancelButton.setOnClickListener(v -> {
            if (navStack != null) navStack.popScreen();
        });
        deleteButton.setOnClickListener(v -> deleteEvent());
    }

    /**
     * Sets up click listeners for waiting list sections. Each section opens a dialog showinf the list of users in that status.
     */
    private void setupWaitingListListeners() {
        // Click listeners for accepted users
        if (acceptedLabelLayout != null) {
            acceptedLabelLayout.setOnClickListener(v -> showUserListDialog("Accepted Users", acceptedUsers, "accepted"));
        }
        if (acceptedCountText != null) {
            acceptedCountText.setOnClickListener(v -> showUserListDialog("Accepted Users", acceptedUsers, "accepted"));
        }

        // Click listeners for pending users
        if (pendingLabelLayout != null) {
            pendingLabelLayout.setOnClickListener(v -> showUserListDialog("Pending Users", pendingUsers, "pending"));
        }
        if (pendingCountText != null) {
            pendingCountText.setOnClickListener(v -> showUserListDialog("Pending Users", pendingUsers, "pending"));
        }

        // Click listeners for waiting users
        if (waitingLabelLayout != null) {
            waitingLabelLayout.setOnClickListener(v -> showUserListDialog("Waiting List Users", waitingListUsers, "waiting"));
        }
        if (waitingCountText != null) {
            waitingCountText.setOnClickListener(v -> showUserListDialog("Waiting List Users", waitingListUsers, "waiting"));
        }

        // Click listeners for rejected users
        if (rejectedLabelLayout != null) {
            rejectedLabelLayout.setOnClickListener(v -> showUserListDialog("Rejected Users", rejectedUsers, "rejected"));
        }
        if (rejectedCountText != null) {
            rejectedCountText.setOnClickListener(v -> showUserListDialog("Rejected Users", rejectedUsers, "rejected"));
        }

        // Click listeners for cancelled invitations
        if (cancelledLabelLayout != null) {
            cancelledLabelLayout.setOnClickListener(v -> showUserListDialog("Cancelled Invitations", cancelledUsers, "cancelled"));
        }
        if (cancelledCountText != null) {
            cancelledCountText.setOnClickListener(v -> showUserListDialog("Cancelled Invitations", cancelledUsers, "cancelled"));
        }
    }

    /**
     * Displays a dialog showing a detailed list of entries with timestamps
     *
     * @param title The title for the dialog
     * @param userIds The list fo userIDs to display
     * @param status The status type for proper timestamp display
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
     * Starts the FIrestore listeners to observe changes to the event and/or invitations.
     * Updates UI automatically when data changes.
     */
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

        // Listen to invitations changes (for accepted, pending, rejected, or cancelled)
        invitationsListener = invitationController.observeEventInvites(event.getEventID(), invitations -> {
            acceptedUsers = new ArrayList<>();
            pendingUsers = new ArrayList<>();
            rejectedUsers = new ArrayList<>();
            cancelledUsers = new ArrayList<>();

            // Categorize invitations by status
            for (Invitation invitation : invitations) {
                if (Boolean.TRUE.equals(invitation.getCancelled())) {
                    cancelledUsers.add(invitation.getRecipientID());
                    continue;
                }

                Boolean accepted = invitation.getAccepted();
                if (accepted == null) {
                    // Pending (null means that the user hasn't responded yet)
                    pendingUsers.add(invitation.getRecipientID());
                }
                else if (accepted) {
                    // Accepted
                    acceptedUsers.add(invitation.getRecipientID());
                }
                else {
                    // Rejected (accepted == false)
                    rejectedUsers.add(invitation.getRecipientID());
                }
            }

            // Call methods to update counts
            updateAcceptedCount();
            updatePendingCount();
            updateRejectedCount();
            updateCancelledCount();
        });
    }

    /**
     * Updates the accepted users count display
     */
    private void updateAcceptedCount() {
        if (acceptedCountText != null) {
            acceptedCountText.setText(String.valueOf(acceptedUsers.size()));
        }
    }

    /**
     * Updates the rejected users count display
     */
    private void updateRejectedCount() {
        if (rejectedCountText != null) {
            rejectedCountText.setText(String.valueOf(rejectedUsers.size()));
        }
    }

    /**
     * Updates the pending users count display
     */
    private void updatePendingCount() {
        if (pendingCountText != null) {
            pendingCountText.setText(String.valueOf(pendingUsers.size()));
        }
    }

    /**
     * Updates the waiting users count display
     */
    private void updateWaitingCount() {
        if (waitingCountText != null) {
            waitingCountText.setText(String.valueOf(waitingListUsers.size()));
        }
    }

    /**
     * Updates the cancelled users count display
     */
    private void updateCancelledCount() {
        if (cancelledCountText != null) {
            cancelledCountText.setText(String.valueOf(cancelledUsers.size()));
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
        }
        else {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                return;
            }
        }

        // Laumch the image picker intent
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

            // Resize the bitmap to reduce file size
            Bitmap resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE);

            // Compress and convert to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            posterBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            Log.d(TAG, "Image converted to Base64, size: " + posterBase64.length() + " characters");

            // Update the UI
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

    /**
     * Resizes a bitmap to fit within the specified max. dimensions while maintaining aspect ratio
     *
     * @param bitmap The original bitmap to resize
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
     * Loads and displays an image from a Base64 encoded image string. Shows placeholder if the string is empty or invalid.
     *
     * @param base64String The Base64 encoded image string
     */
    private void loadImageFromBase64(String base64String) {
        if (TextUtils.isEmpty(base64String)) {
            posterPreview.setImageResource(R.drawable.image_placeholder);
            deletePosterButton.setVisibility(View.GONE);
            return;
        }

        try {
            // Decode the Base64 string to a byte array
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

    /**
     * Updates the text on a date button to display the formatted date. If the timestamp is null, it will show the default text.
     *
     * @param button The button to update
     * @param timestamp The timestamp containign the data to display
     */
    private void updateDateButtonText(Button button, Timestamp timestamp) {
        if (timestamp != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            button.setText(dateFormat.format(timestamp.toDate()));
        }
        else {
            // Set default text based on which button it is
            if (button.getId() == R.id.btnStartDate) {
                button.setText("Pick Start Date");
            }
            else if (button.getId() == R.id.btnEndDate) {
                button.setText("Pick End Date");
            }
        }
    }

    /**
     * Updates the text on a time button to display the formatted time. If the timestamp is null, it will show the default text.
     *
     * @param button The button to update
     * @param timestamp The timestamp containing the time to display
     */
    private void updateTimeButtonText(Button button, Timestamp timestamp) {
        if (timestamp != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            button.setText(dateFormat.format(timestamp.toDate()));
        }
        else {
            // Set default text based on which button it is
            if (button.getId() == R.id.btnStartTime) {
                button.setText("Pick Start Time");
            }
            else if (button.getId() == R.id.btnEndTime) {
                button.setText("Pick End Time");
            }
        }
    }

    /**
     * Populates all input fields with data from a given event. Used when editing an event
     *
     * @param e The event whose data will be displayed
     */
    private void populateFields(Event e) {
        titleInput.setText(e.getName());
        locationInput.setText(e.getLocationName());
        descriptionInput.setText(e.getDescription());
        entrantLimitInput.setText(e.getMaxAttendees() != null ? String.valueOf(e.getMaxAttendees()) : "");
        maxWaitingListInput.setText(e.getMaxWaitingList() != null ? String.valueOf(e.getMaxWaitingList()) : "");

        // Set date/time values
        startTime = e.getRegistrationStart();
        endTime = e.getRegistrationEnd();
        updateDateButtonText(startDateButton, startTime);
        updateDateButtonText(endDateButton, endTime);
        updateTimeButtonText(startTimeButton, startTime);
        updateTimeButtonText(endTimeButton, endTime);

        // Load the poster is available
        posterBase64 = e.getPosterUrl();
        if (!TextUtils.isEmpty(posterBase64)) {
            loadImageFromBase64(posterBase64);
        }

        // Set the geolocation toggle state
        geolocationToggle.setChecked(e.isGeolocationRequired());

        // Disable geolocation toggle in edit mode (cannot be changed after creation)
        if (isEditMode && !isViewMode) {
            geolocationToggle.setEnabled(false);
            geolocationToggle.setAlpha(0.6f);
        }


    }

    /**
     * Validates input fields and creates a new event. Shows a toast message and initiates the creation process.
     */
    private void createEvent() {
        if (!validateInputs()) {
            return;
        }

        Toast.makeText(getContext(), "Creating event...", Toast.LENGTH_SHORT).show();
        createEventWithPoster(posterBase64);
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
        newEvent.setOrganizerID(organizer.getUserID());
        newEvent.setMaxAttendees(Integer.parseInt(entrantLimitInput.getText().toString()));
        newEvent.setRegistrationStart(startTime);
        newEvent.setRegistrationEnd(endTime);
        newEvent.setLotteryComplete(false);
        newEvent.setPosterUrl(posterData);

        // Max waiting list
        String maxWaitingListStr = maxWaitingListInput.getText().toString().trim();
        if (!maxWaitingListStr.isEmpty()) {
            newEvent.setMaxWaitingList(Integer.parseInt(maxWaitingListStr));
        }
        else {
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
                .addOnFailureListener(e -> {
                    Log.e(TAG, "FAILED to create event", e);
                    Toast.makeText(getContext(), "Failed to create event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Validates inpit fields and updates the existing event. It determines whether to keep, update, or delete the poster based on user actions
     */
    private void updateEvent() {
        if (event == null || TextUtils.isEmpty(event.getEventID())) return;

        if (!validateInputs()) {
            return;
        }

        Toast.makeText(getContext(), "Updating event...", Toast.LENGTH_SHORT).show();

        // Determine which poster data to save
        String posterDataToSave;
        if (posterDeleted) {
            // User explicitly deleted the poster
            posterDataToSave = null;
        }
        else if (posterBase64 != null && !posterBase64.equals(event.getPosterUrl())) {
            // User uploaded a new poster
            posterDataToSave = posterBase64;
        }
        else {
            // Keep the existing poster unchanged
            posterDataToSave = event.getPosterUrl();
        }

        updateEventWithPoster(posterDataToSave);
    }

    /**
     * Updates the event in Firestore with the provided poster data and the form field vals.
     * On success, will navigate back to the OrganizerFragment, and shows an error message on failure.
     *
     * @param posterData The BAse 64 encoded poster image, null if we're removing a poster
     */
    private void updateEventWithPoster(String posterData) {
        // Builf a map of fields to update
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", titleInput.getText().toString());
        updates.put("description", descriptionInput.getText().toString());
        updates.put("locationName", locationInput.getText().toString());
        updates.put("maxAttendees", Integer.parseInt(entrantLimitInput.getText().toString()));
        updates.put("registrationStart", startTime);
        updates.put("registrationEnd", endTime);
        updates.put("posterUrl", posterData);

        // Max waiting list change
        String maxWaitingListStr = maxWaitingListInput.getText().toString().trim();
        if (!maxWaitingListStr.isEmpty()) {
            updates.put("maxWaitingList", Integer.parseInt(maxWaitingListStr));
        }
        else {
            // Null means unlimited
            updates.put("maxWaitingList", null);
        }

        // Perform the update in Firestore
        eventController.updateEvent(event.getEventID(), updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Event updated!", Toast.LENGTH_SHORT).show();
                // Update the local event object
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

    /**
     * Displays a date picker dialog and invokes the callbakc with the selected date
     *
     * @param button The button that triggeref the picker
     * @param current The current timestamp to initialize the picker, null for today
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
     * @param button The button that triggered the picker
     * @param current The current timestamp to initialize the picker, null if now
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

    /**
     * Validates all required input fields by checking for empty fields, valid numbers, and logical date/time constraints.
     *
     * @return true if all validations pass, false otherwise
     */
    private boolean validateInputs() {
        boolean isValid = true;

        // Validate title
        if (TextUtils.isEmpty(titleInput.getText())) {
            titleInput.setError("Title is required");
            isValid = false;
        }
        // Validate location
        if (TextUtils.isEmpty(locationInput.getText())) {
            locationInput.setError("Location is required");
            isValid = false;
        }
        // Validate entrant limit
        if (TextUtils.isEmpty(entrantLimitInput.getText())) {
            entrantLimitInput.setError("Entrant limit is required");
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

        return isValid;
    }

    /**
     * Displays a confirmation dialog and deleted the event if the organizer confirms. It also removes the event from the organizer's list of owned events.
     */
    private void deleteEvent() {
        if (event == null || TextUtils.isEmpty(event.getEventID())) {
            Toast.makeText(getContext(), "Error: Missing eventID", Toast.LENGTH_SHORT).show();
            return;
        }
        // Shows confirmation dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Event?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    String eventID = event.getEventID();

                    // Delete event from Firestore
                    eventController.deleteEvent(eventID)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Event deleted from Firestore");

                                // Remove the event from the organizer's owned events
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
                                    // No organizer object available, so just navigate back
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

    /**
     * DIsables all the input fields and buttons, for view-only mode. This will prevent organizers from editing past events.
     */
    private void disableAllInputs() {
        // Disable text inputs
        titleInput.setEnabled(false);
        locationInput.setEnabled(false);
        descriptionInput.setEnabled(false);
        entrantLimitInput.setEnabled(false);
        maxWaitingListInput.setEnabled(false);

        // Disable date/time buttons
        startDateButton.setEnabled(false);
        startTimeButton.setEnabled(false);
        endDateButton.setEnabled(false);
        endTimeButton.setEnabled(false);

        // Disable poster buttons
        importPosterButton.setEnabled(false);
        deletePosterButton.setEnabled(false);

        // Disable geolocation toggle
        geolocationToggle.setEnabled(false);

        // I'm just gonna change the opacity so clear fields are disabled
        titleInput.setAlpha(0.6f);
        locationInput.setAlpha(0.6f);
        descriptionInput.setAlpha(0.6f);
        entrantLimitInput.setAlpha(0.6f);
        maxWaitingListInput.setAlpha(0.6f);
        startDateButton.setAlpha(0.6f);
        startTimeButton.setAlpha(0.6f);
        endDateButton.setAlpha(0.6f);
        endTimeButton.setAlpha(0.6f);
        importPosterButton.setAlpha(0.6f);
        deletePosterButton.setAlpha(0.6f);
        geolocationToggle.setAlpha(0.6f);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open the image picker
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