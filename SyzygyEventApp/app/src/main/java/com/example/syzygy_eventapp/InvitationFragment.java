package com.example.syzygy_eventapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

/**
 * Invite screen shown to entrants who have won the lottery.
 * The user must accept or decline. "More Details" shows the entrant event page
 * without changing the invitation state.
 */
public class InvitationFragment extends Fragment {
    private NavigationStackFragment navStack;

    private InvitationController invitationController;
    private EventController eventController;
    private UserControllerInterface userController;

    private String invitationId;
    private Event event;

    private TextView eventTitleTextView;
    private ImageView eventImageView;
    private ImageView organizerImageView;
    private TextView organizerNameTextView;
    private TextView locationTextView;
    private TextView eventTimeTextView;
    private Button moreDetailsButton;

    InvitationFragment(String invitationId, NavigationStackFragment navStack) {
        super();
        this.navStack = navStack;
        this.invitationId = invitationId;
    }

    /**
     * Initializes the screen, loads the invitation, and binds event/organizer data.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate this fragment's layout
        View view = inflater.inflate(R.layout.fragment_event_invite, container, false);

        invitationController = new InvitationController();
        eventController = EventController.getInstance();
        userController = UserController.getInstance();

        eventTitleTextView = view.findViewById(R.id.event_title);
        eventImageView = view.findViewById(R.id.event_image);
        organizerImageView = view.findViewById(R.id.organizer_image);
        organizerNameTextView = view.findViewById(R.id.organizer_name);
        locationTextView = view.findViewById(R.id.location_text);
        eventTimeTextView = view.findViewById(R.id.event_time);

        moreDetailsButton = view.findViewById(R.id.btn_more_details);
        moreDetailsButton.setEnabled(false);
        moreDetailsButton.setOnClickListener(v -> openEventDetails(false));


        Button acceptButton = view.findViewById(R.id.btn_accept);
        acceptButton.setOnClickListener(v -> handleAccept());

        Button declineButton = view.findViewById(R.id.btn_decline);
        declineButton.setOnClickListener(v -> handleDecline());

        loadInvitation();

        return view;
    }


    @Override
    public void onStart() {
        super.onStart();

        // set a back button nav bar
        navStack.setScreenNavMenu(R.menu.empty_nav_menu, null);
        navStack.setScreenBackEnabled(false);
    }

    /**
     * Loads the Invitation from Firestore and then loads its Event and organizer.
     */
    private void loadInvitation() {
        invitationController.getInvite(invitationId)
                .addOnSuccessListener(invite -> {
                    loadEventAndOrganizer(invite.getEvent(), invite.getOrganizerID());
                })
                .addOnFailureListener(e -> {
                    navStack.popScreen();
                });
    }

    /**
     * Loads the Event and organizer User for this invitation and binds them to the UI.
     *
     * @param eventId     ID of the invited event
     * @param organizerId ID of the organizer user
     */
    private void loadEventAndOrganizer(String eventId, String organizerId) {
        eventController.getEvent(eventId)
                .addOnSuccessListener(loadedEvent -> {
                    event = loadedEvent;
                    bindEventToViews();

                    moreDetailsButton.setEnabled(true);

                    userController.getUser(organizerId)
                            .addOnSuccessListener(this::bindOrganizerToViews);
                })
                .addOnFailureListener(e -> {
                    invitationController.deleteInvite(invitationId);
                    navStack.popScreen();
                });
    }

    /**
     * Binds event data (title, location, time, poster) to the invite layout.
     */
    private void bindEventToViews() {
        eventTitleTextView.setText(event.getName());

        locationTextView.setText(event.getLocationName());

        String timeText = DateFormat.format("h:mm a", event.getEventTime().toDate()).toString();
        eventTimeTextView.setText(timeText);

        Bitmap poster = event.generatePosterBitmap();
        if (poster != null) {
            eventImageView.setImageBitmap(poster);
        }
    }

    /**
     * Binds organizer data (name and optional photo) to the invite layout.
     *
     * @param organizer The organizer user for this event
     */
    private void bindOrganizerToViews(User organizer) {
        organizerNameTextView.setText(organizer.getName());

        String photo = organizer.getPhotoURL();
        if (photo != null && !photo.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(photo, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (bmp != null) {
                    organizerImageView.setImageBitmap(bmp);
                } else {
                    organizerImageView.setImageResource(R.drawable.profile_placeholder);
                }
            } catch (Exception e) {
                organizerImageView.setImageResource(R.drawable.profile_placeholder);
            }
        } else {
            organizerImageView.setImageResource(R.drawable.profile_placeholder);
        }
    }

    /**
     * marks the invitation accepted and opens the entrant event details screen.
     */
    private void handleAccept() {
        invitationController.acceptInvite(invitationId)
                .addOnSuccessListener(updated -> openEventDetails(true))
                .addOnFailureListener(error -> {
                    Toast.makeText(requireContext(),
                            "Failed to accept invitation: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * marks the invitation declined and returns to the previous screen (Profile).
     */
    private void handleDecline() {
        invitationController.declineInvite(invitationId)
                .addOnSuccessListener(updated -> navStack.popScreen())
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Failed to decline invitation: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Opens MainActivity and pushes the entrant event details screen.
     *
     * @param fromAccept true if called directly after acceptance
     */
    private void openEventDetails(boolean fromAccept) {
        if (event == null || event.getEventID() == null) {
            Toast.makeText(getContext(),
                    "Event details are still loading. Please try again.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        EventFragment eventFragment = new EventFragment(navStack, event.getEventID());

        if (fromAccept) {
            navStack.replaceScreen(eventFragment);
        } else {
            navStack.pushScreen(eventFragment);
        }
    }
}
