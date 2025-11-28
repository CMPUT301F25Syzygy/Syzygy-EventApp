package com.example.syzygy_eventapp;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.ListenerRegistration;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment class that allows an organizer to create or edit an event's details.
 * Provides functionality for managing event info, including dates, times, posters, and for viewing waiting lists or invitation statuses in real-time
 */
public class EventOrganizerDetailsView extends Fragment {
    // declare UI components
    private TextView eventNameText, eventDescriptionText;
    private TextView locationText, eventTimeText, eventEntrantsText;
    private Button openMapButton;
    private UserListView acceptedListView, pendingListView, waitingListView;
    private ImageView posterImage;
    private Button cancelInvitesButton, sendInvitesButton, sendNotificationButton;

    // Controllers for Firebase operations
    private EventController eventController;
    private UserControllerInterface userController;
    private InvitationController invitationController;

    // Current user and event data
    private Event event;
    private final NavigationStackFragment navStack;

    private ListenerRegistration eventListener;
    private ListenerRegistration inviteListener;

    /**
     * A new instance of EventOrganizerDetailsView.
     * To be used so the organizer to view old events and their entrant info, without being able to edit it directly.
     *
     * @param event    The event to view organizer details for
     * @param navStack The nav stack for screen management
     * @return A new instance of EventOrganizerDetailsView
     */
    public EventOrganizerDetailsView(@Nullable Event event, @Nullable NavigationStackFragment navStack) {
        this.event = event;
        this.navStack = navStack;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_organizer_event_details, container, false);

        // Initialize views
        eventNameText = view.findViewById(R.id.event_title);
        eventDescriptionText = view.findViewById(R.id.event_description);

        locationText = view.findViewById(R.id.event_location);
        eventTimeText = view.findViewById(R.id.event_time);
        eventEntrantsText = view.findViewById(R.id.event_entrants);

        posterImage = view.findViewById(R.id.event_banner);

        acceptedListView = view.findViewById(R.id.accepted_list_view);
        acceptedListView.setTitle("Accepted");
        acceptedListView.setListVisibility(false);

        pendingListView = view.findViewById(R.id.pending_list_view);
        pendingListView.setTitle("Pending");
        pendingListView.setListVisibility(false);

        waitingListView = view.findViewById(R.id.waiting_list_view);
        waitingListView.setTitle("Waiting");
        waitingListView.setListVisibility(false);

        openMapButton = view.findViewById(R.id.open_map_button);
        cancelInvitesButton = view.findViewById(R.id.cancel_invites_button);
        sendInvitesButton = view.findViewById(R.id.send_invites_button);
        sendNotificationButton = view.findViewById(R.id.send_notification_button);

        // Initialize controllers for Firebase operations
        eventController = EventController.getInstance();
        userController = UserController.getInstance();
        invitationController = new InvitationController();

        eventListener = eventController.observeEvent(event.getEventID(), (newEvent) -> {
            event = newEvent;
            refreshInterface();
        });
        inviteListener = invitationController.observeEventInvites(event.getEventID(), (newEvent) -> {
            refreshInterface();
        });

        setupListeners();
        setupNavBar();

        return view;
    }

    /**
     * Sets up click listeners for all interactive UI elements
     */
    private void setupListeners() {
        // View map button
        openMapButton.setOnClickListener(v -> {
            Maps.openInMaps(requireActivity(), event);
        });

        cancelInvitesButton.setOnClickListener(v -> {
            cancelInvites();
        });

        sendInvitesButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Starting lottery...", Toast.LENGTH_SHORT).show();
            eventController.drawLotteryEarly(event.getEventID())
                    .addOnSuccessListener((result) -> {
                        Toast.makeText(getContext(), "Lottery drawn", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener((result) -> {
                        Toast.makeText(getContext(), "Failed to draw lottery", Toast.LENGTH_SHORT).show();
                    });
        });

        sendNotificationButton.setOnClickListener(v -> {
            // TODO
            Toast.makeText(getContext(), "Not implemented", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Sets up nav bar buttons and listener
     */
    private void setupNavBar() {
        navStack.setScreenNavMenu(R.menu.organizer_event_details, (MenuItem item) -> {
            if (item.getItemId() == R.id.back_nav_button) {
                navStack.popScreen();
            } else if (item.getItemId() == R.id.generate_qr_nav_button) {
                QRGenerateFragment qrFragment = new QRGenerateFragment(event, navStack);
                navStack.pushScreen(qrFragment);
            } else if (item.getItemId() == R.id.preview_nav_button) {
                EventFragment entrantEventFragment = new EventFragment(navStack, event.getEventID());
                navStack.pushScreen(entrantEventFragment);
            } else if (item.getItemId() == R.id.edit_nav_button) {
                // TODO
                Toast.makeText(getContext(), "Not implemented", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    /**
     * Fills in information about the event into the UI
     */
    private void refreshInterface() {
        eventNameText.setText(event.getName());
        eventDescriptionText.setText(event.getDescription());

        locationText.setText(event.getLocationName());

        String eventTime = DateFormat.format("MMM d, yyyy HH:mm", event.getEventTime().toDate()).toString();
        eventTimeText.setText(eventTime);

        Bitmap bitmap = event.getPosterBitmap();
        if (bitmap == null) {
            posterImage.setImageResource(R.drawable.image_placeholder);
        } else {
            posterImage.setImageBitmap(bitmap);
        }

        refreshInvitedUsers();
        refreshWaitlistUsers();
    }


    /**
     * Fills in information about invited users into the UI
     */
    private Task<?> refreshWaitlistUsers() {
        Task<List<User>> loadWaitingUsersTask = userController.getUsers(event.getWaitingList());
        return loadWaitingUsersTask
                .addOnSuccessListener(users -> {
                    waitingListView.setUsers(users);
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(getContext(), "Failed to load waitlist", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Fills in information about invited users into the UI
     */
    private Task<?> refreshInvitedUsers() {
        Task<List<Invitation>> loadInvitesTask = invitationController.getEventInvites(event.getEventID());

        return loadInvitesTask.continueWithTask(task -> {
                    List<Invitation> invites = task.getResult();

                    List<String> acceptedUserIds = new ArrayList<>();
                    List<String> pendingUserIds = new ArrayList<>();

                    // sort user invites into two groups
                    for (Invitation invite : invites) {
                        if (invite.getCancelled()) continue;

                        if (invite.getAccepted()) {
                            acceptedUserIds.add(invite.getRecipientID());
                        } else {
                            pendingUserIds.add(invite.getRecipientID());
                        }
                    }

                    Task<List<User>> loadAcceptedUsersTask = userController.getUsers(acceptedUserIds);
                    loadAcceptedUsersTask
                            .addOnSuccessListener(users -> {
                                acceptedListView.setUsers(users);
                                eventEntrantsText.setText(users.size() + " / " + event.getMaxAttendees());
                            })
                            .addOnFailureListener(error -> {
                                Toast.makeText(getContext(), "Failed to load accepted users", Toast.LENGTH_SHORT).show();
                            });

                    Task<List<User>> loadPendingUsersTask = userController.getUsers(pendingUserIds);
                    loadPendingUsersTask
                            .addOnSuccessListener(users -> {
                                pendingListView.setUsers(users);
                            })
                            .addOnFailureListener(error -> {
                                Toast.makeText(getContext(), "Failed to load pending users", Toast.LENGTH_SHORT).show();
                            });

                    return Tasks.whenAllComplete(loadAcceptedUsersTask, loadPendingUsersTask);
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(getContext(), "Failed to load invites", Toast.LENGTH_SHORT).show();
                });
    }

    private Task<Void> cancelInvites() {
        List<String> inviteIds = event.getInvites();

        if(pendingListView.getUsers().size() == 0) {
            Toast.makeText(getContext(), "There are pending invites", Toast.LENGTH_SHORT).show();
            return Tasks.forResult(null);
        }

        List<Task<Boolean>> cancelTasks = new ArrayList<>();
        for (String inviteId : inviteIds) {
            cancelTasks.add(invitationController.cancelInvite(inviteId));
        }

        return Tasks.whenAllComplete(cancelTasks)
                .continueWithTask(allCompleteTask -> {
                    List<Task<Boolean>> tasks = (List<Task<Boolean>>) (List<?>) allCompleteTask.getResult();

                    boolean someSucceded = false;
                    boolean someFailed = false;

                    for (Task<Boolean> task : tasks) {
                        if (!task.isSuccessful()) {
                            someFailed = true;
                        } else if (task.getResult() == true) {
                            someSucceded = true;
                        }
                    }

                    if (someSucceded) {
                        if (someFailed) {
                            Toast.makeText(getContext(), "Cancelled some invites, others failed", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Cancelled all invites", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (someFailed) {
                            Toast.makeText(getContext(), "Failed to cancel all invites", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "There are pending invites", Toast.LENGTH_SHORT).show();
                        }
                    }

                    return Tasks.forResult(null);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        eventListener.remove();
        inviteListener.remove();
    }
}