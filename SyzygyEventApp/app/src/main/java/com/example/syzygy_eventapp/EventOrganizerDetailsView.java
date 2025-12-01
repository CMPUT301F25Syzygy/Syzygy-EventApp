package com.example.syzygy_eventapp;


import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Layout;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.ListenerRegistration;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private NotificationController notificationController;

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
        invitationController = InvitationController.getInstance();
        notificationController = NotificationController.getInstance();

        eventListener = eventController.observeEvent(event.getEventID(), this::setEvent, navStack::popScreen);
        inviteListener = invitationController.observeEventInvites(event.getEventID(),
                (newEvent) -> {
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
            showToast("Starting lottery...");
            eventController.drawLotteryEarly(event.getEventID())
                    .addOnSuccessListener((result) -> {
                        showToast("Lottery drawn");
                    })
                    .addOnFailureListener((result) -> {
                        showToast("Failed to draw lottery");
                    });
        });

        sendNotificationButton.setOnClickListener(v -> {
            openSendNotificationDialog();
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
                navStack.pushScreen(
                        new OrganizerEventEditDetailsFragment(event, navStack)
                );
            }
            return true;
        });
    }

    public void setEvent(Event event) {
        this.event = event;
        refreshInterface();
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

        Bitmap bitmap = event.generatePosterBitmap();
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
                    showToast("Failed to load waitlist");
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
                        Boolean cancelled = invite.getCancelled();
                        Boolean accepted = invite.getAccepted();
                        com.google.firebase.Timestamp responseTime = invite.getResponseTime();

                        // Skip invites the organizer has cancelled
                        if (Boolean.TRUE.equals(cancelled)) {
                            continue;
                        }

                        // Accepted list
                        if (Boolean.TRUE.equals(accepted)) {
                            acceptedUserIds.add(invite.getRecipientID());
                            continue;
                        }

                        // Pending = no response yet (responseTime still null)
                        if (responseTime == null) {
                            pendingUserIds.add(invite.getRecipientID());
                            // declined = responded + not accepted, ignored
                        }
                    }

                    Task<List<User>> loadAcceptedUsersTask = userController.getUsers(acceptedUserIds);
                    loadAcceptedUsersTask
                            .addOnSuccessListener(users -> {
                                acceptedListView.setUsers(users);
                                eventEntrantsText.setText(users.size() + " / " + event.getMaxAttendees());
                            })
                            .addOnFailureListener(error -> {
                                showToast("Failed to load accepted users");
                            });

                    Task<List<User>> loadPendingUsersTask = userController.getUsers(pendingUserIds);
                    loadPendingUsersTask
                            .addOnSuccessListener(users -> {
                                pendingListView.setUsers(users);
                            })
                            .addOnFailureListener(error -> {
                                showToast("Failed to load pending users");
                            });

                    return Tasks.whenAllComplete(loadAcceptedUsersTask, loadPendingUsersTask);
                })
                .addOnFailureListener(error -> {
                    showToast("Failed to load invites");
                });
    }

    private Task<Void> cancelInvites() {
        List<String> inviteIds = event.getInvites();

        if (pendingListView.getUsers().size() == 0) {
            showToast("There are no pending invites");
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
                            showToast("Cancelled some invites, others failed");
                        } else {
                            showToast("Cancelled all invites");
                        }
                    } else {
                        if (someFailed) {
                            showToast("Failed to cancel all invites");
                        } else {
                            showToast("There are no pending invites");
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

    private void showToast(String message) {
        if (!isAdded()) {
            return;
        }

        android.content.Context ctx = getContext();
        if (ctx == null) {
            return;
        }

        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
    }

    private void openSendNotificationDialog() {
        // inflate the layout
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.send_notification_dialog, null);

        // make an alert dialog for sending notifications
        AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Send Notification")
                .setView(dialogView)
                .setPositiveButton("Send", null)
                .setNegativeButton("Cancel", null)
                .show();

        // set button separately to stop dialog from closing
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        EditText titleEditText = dialogView.findViewById(R.id.title_edit_text);
        EditText descriptionEditText = dialogView.findViewById(R.id.description_edit_text);

        SwitchMaterial acceptedUsersSwitch = dialogView.findViewById(R.id.acceptedUsersSwitch);
        SwitchMaterial pendingUsersSwitch = dialogView.findViewById(R.id.pendingUsersSwitch);
        SwitchMaterial waitingUsersSwitch = dialogView.findViewById(R.id.waitingUsersSwitch);

        positiveButton.setOnClickListener((view) -> {
            boolean isValid = true;

            if (titleEditText.getText().toString().trim().isEmpty()) {
                titleEditText.setError("Title is required");
                isValid = false;
            }

            if (descriptionEditText.getText().toString().trim().isEmpty()) {
                descriptionEditText.setError("Description is required");
                isValid = false;
            }

            if (!acceptedUsersSwitch.isChecked() && !pendingUsersSwitch.isChecked() && !waitingUsersSwitch.isChecked()) {
                if (isValid) {
                    showToast("At least one group must be checked");
                }
                isValid = false;
            }

            if (isValid) {
                sendNotification(
                        titleEditText.getText().toString().trim(),
                        descriptionEditText.getText().toString().trim(),
                        acceptedUsersSwitch.isChecked(),
                        pendingUsersSwitch.isChecked(),
                        waitingUsersSwitch.isChecked());
                dialog.dismiss();
            }
        });
    }

    public Task<Void> sendNotification(String title, String description, boolean toAccepted, boolean toPending, boolean toWaiting) {
        List<String> recipientIds = new ArrayList<>();
        List<Task<?>> tasks = new ArrayList<>();

        if (toWaiting) {
            recipientIds.addAll(event.getWaitingList());
        }

        if (toAccepted || toPending) {
            tasks.add(invitationController.getEventInvites(event.getEventID())
                    .addOnSuccessListener((invites) -> {
                        for (Invitation invite : invites) {
                            if (invite.getAccepted() && toAccepted) {
                                recipientIds.add(invite.getRecipientID());
                            } else if (!invite.hasResponse() && !invite.getCancelled() && toPending) {
                                recipientIds.add(invite.getRecipientID());
                            }
                        }
                    }));
        }

        Notification notif = new Notification(title, description, event.getEventID(), event.getOrganizerID());

        showToast("Sending notification...");
        return Tasks.whenAllSuccess(tasks).continueWithTask((_tasks) -> {
            return notificationController.postNotification(notif, recipientIds);
        }).addOnSuccessListener((nothing) -> {
            if (recipientIds.isEmpty()) {
                showToast("Selected group(s) were empty");
            } else {
                showToast("Notification sent");
                }
        });
    }
}