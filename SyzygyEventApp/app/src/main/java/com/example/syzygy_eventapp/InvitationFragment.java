package com.example.syzygy_eventapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Invite screen shown to entrants who have won the lottery.
 * The user must accept or decline. "More Details" shows the entrant event page
 * without changing the invitation state.
 */
public class InvitationFragment extends AppCompatActivity {

    public static final String EXTRA_INVITATION_ID = "extra_invitation_id";

    private InvitationController invitationController;
    private EventController eventController;
    private UserControllerInterface userController;

    private String invitationId;
    private Invitation invitation;
    private Event event;

    private TextView invitedTextView;
    private TextView eventTitleTextView;
    private ImageView eventImageView;
    private ImageView organizerImageView;
    private TextView organizerNameTextView;
    private TextView locationTextView;
    private TextView eventTimeTextView;
    private Button moreDetailsButton;
    private Button declineButton;
    private Button acceptButton;

    /**
     * Initializes the screen, loads the invitation, and binds event/organizer data.
     * The Activity must be started with {@link #EXTRA_INVITATION_ID}.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_event_invite);

        invitationController = new InvitationController();
        eventController = EventController.getInstance();
        userController = UserController.getInstance();

        invitationId = getIntent().getStringExtra(EXTRA_INVITATION_ID);

        invitedTextView = findViewById(R.id.invited_text);
        eventTitleTextView = findViewById(R.id.event_title);
        eventImageView = findViewById(R.id.event_image);
        organizerImageView = findViewById(R.id.organizer_image);
        organizerNameTextView = findViewById(R.id.organizer_name);
        locationTextView = findViewById(R.id.location_text);
        eventTimeTextView = findViewById(R.id.event_time);
        moreDetailsButton = findViewById(R.id.btn_more_details);
        declineButton = findViewById(R.id.btn_decline);
        acceptButton = findViewById(R.id.btn_accept);

        moreDetailsButton.setEnabled(false);

        loadInvitation();

        moreDetailsButton.setOnClickListener(v -> openEventDetails(false));
        acceptButton.setOnClickListener(v -> handleAccept());
        declineButton.setOnClickListener(v -> handleDecline());
    }

    /**
     * Loads the Invitation from Firestore and then loads its Event and organizer.
     */
    private void loadInvitation() {
        invitationController.getInvite(invitationId)
                .addOnSuccessListener(invite -> {
                    invitation = invite;
                    loadEventAndOrganizer(invite.getEvent(), invite.getOrganizerID());
                })
                .addOnFailureListener(e -> {
                    finish();
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
                    finish();
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
                .addOnSuccessListener(updated -> {
                    openEventDetails(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to accept invitation: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * marks the invitation declined and returns to the previous screen (Profile).
     */
    private void handleDecline() {
        invitationController.declineInvite(invitationId)
                .addOnSuccessListener(updated -> finish())
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
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
            Toast.makeText(this,
                    "Event details are still loading. Please try again.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String eventId = event.getEventID();

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_EVENT_ID, eventId);
        startActivity(intent);

        finish();
    }
}
