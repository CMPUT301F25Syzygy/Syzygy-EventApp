package com.example.syzygy_eventapp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.util.Date;

/**
 * A custom view that displays a compact summary of an event, including its title,
 * location, date, time, and status. It adapts its appearance based on whether the
 * viewer is an entrant or an admin, showing different status indicators and action
 * buttons accordingly. Used within event lists to give users a quick overview before
 * viewing full event details.
 */
public class OrganizerEventSummaryFragment extends LinearLayout {

    private TextView titleText, timeText, locationText, dateText, acceptedCountText, interestedCountText;
    private MaterialCardView card;
    private Chip statusChip;
    private FirebaseFirestore db;

    /**
     * Represents the status of an entrant for a given event.
     */
    public enum AttendeeStatus { WAITLIST, NOT_SELECTED, PENDING, ACCEPTED, REJECTED }

    /**
     * Default constructor for inflating via code.
     * @param context the current {@link Context}.
     */
    public OrganizerEventSummaryFragment(Context context) {
        super(context);
        init(context);
    }

    /**
     * Constructor called when inflating from XML.
     * @param context the current {@link Context}.
     * @param attrs the {@link AttributeSet} from XML.
     */
    public OrganizerEventSummaryFragment(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructor called when inflating from XML with a style attribute.
     * @param context the current {@link Context}.
     * @param attrs the {@link AttributeSet} from XML.
     * @param defStyleAttr the default style to apply to this view.
     */
    public OrganizerEventSummaryFragment(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Initializes and inflates the layout, binding all child views.
     * @param context the current {@link Context}.
     */
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.fragment_organizer_event_summary, this, true);

        titleText = findViewById(R.id.event_title);
        timeText = findViewById(R.id.event_time);
        locationText = findViewById(R.id.event_location);
        dateText = findViewById(R.id.event_date);
        acceptedCountText = findViewById(R.id.event_accepted_count);
        interestedCountText = findViewById(R.id.event_interested_count);
        card = findViewById(R.id.event_banner_card);
        statusChip = findViewById(R.id.chip_event_status);

        db = FirebaseFirestore.getInstance();
    }

    /**
     * Binds an {@link Event} object to the summary view, updating text fields and chip color.
     * Also controls admin button visibility.
     *
     * @param event the {@link Event} to display.
     * @param attendeeStatus the status of the current entrant, or {@code null} if admin view.
     */
    public void bind(Event event, AttendeeStatus attendeeStatus) {
        titleText.setText(event.getName());
        locationText.setText(event.getLocationName());

        // Had to comment this out and create a version where a null date is allowed, otherwise, the app crashes
        //Date date = event.getRegistrationEnd().toDate();
        //DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        //DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        //timeText.setText(timeFormat.format(date));
        //dateText.setText(dateFormat.format(date));

        Timestamp endTs = event.getRegistrationEnd();

        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

        if (endTs != null) {
            Date date = endTs.toDate();
            timeText.setText(timeFormat.format(date));
            dateText.setText(dateFormat.format(date));
        } else {
            timeText.setText("—");
            dateText.setText("No date");
        }

        // Get and update counts from Firestore
        getAndUpdateCounts(event.getEventID(), event.getMaxAttendees());

        if (attendeeStatus != null) {
            setAttendeeChipColor(attendeeStatus);
        } else {
            // Also had to make a null-safe verison here
            // setAdminChipColor(event.isLotteryComplete(), event.getRegistrationEnd().toDate());
            endTs = event.getRegistrationEnd();
            Date endDate = (endTs != null) ? endTs.toDate() : new Date(0);  // epoch fallback

            setAdminChipColor(event.isLotteryComplete(), endDate);
        }
    }

    /**
     * Gets accepted and interested counts from Firestore and updates the UI.
     *
     * @param eventID The ID of the event to get counts for
     * @param maxAttendees The maximum number of attendees for this event
     */
    private void getAndUpdateCounts(String eventID, Integer maxAttendees) {
        if (eventID == null || eventID.isEmpty()) {
            acceptedCountText.setText("0");
            interestedCountText.setText("0");
            return;
        }

        // Show a loading state
        acceptedCountText.setText("—");
        interestedCountText.setText("—");

        // Get accepted count (accepted = true, cancelled = false)
        db.collection("invitations")
                .whereEqualTo("event", eventID)
                .whereEqualTo("accepted", true)
                .whereEqualTo("cancelled", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int acceptedCount = querySnapshot.size();
                    acceptedCountText.setText(acceptedCount + "/" + maxAttendees);
                })
                .addOnFailureListener(e -> {
                    acceptedCountText.setText("0");
                });

        // Get interested count (basically just get the waiting list size from the event)
        db.collection("events")
                .document(eventID)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    int waitingListSize = 0;
                    if (eventDoc.exists() && eventDoc.contains("waitingList")) {
                        Object waitingListObj = eventDoc.get("waitingList");
                        if (waitingListObj instanceof java.util.List) {
                            waitingListSize = ((java.util.List<?>) waitingListObj).size();
                        }
                    }

                    interestedCountText.setText(String.valueOf(waitingListSize));

                })
                .addOnFailureListener(e -> {
                    interestedCountText.setText("0");
                });
    }

    /**
     * Sets the color and label of the status chip based on the entrant’s event status.
     * Used for entrant-facing summaries.
     *
     * @param status the {@link AttendeeStatus} of the user in the event.
     */
    private void setAttendeeChipColor(AttendeeStatus status) {
        int color;
        String label;

        switch (status) {
            case ACCEPTED:
                color = R.color.green;
                label = "Accepted";
                break;
            case PENDING:
                color = R.color.yellow;
                label = "Pending";
                break;
            case REJECTED:
                color = R.color.red;
                label = "Rejected";
                break;
            case WAITLIST:
                color = R.color.blue;
                label = "Waitlist";
                break;
            default:
                color = R.color.grey;
                label = "Not Selected";
                break;
        }

        statusChip.setText(label);
        statusChip.setChipBackgroundColor(
                ContextCompat.getColorStateList(getContext(), color)
        );
    }

    /**
     * Sets the color and label of the status chip based on the event’s administrative state.
     * Used for organizer/admin-facing summaries.
     *
     * @param lotteryComplete whether the lottery for the event has been completed.
     * @param registrationEnd the registration end date of the event.
     */
    private void setAdminChipColor(boolean lotteryComplete, Date registrationEnd) {
        int color;
        String label;

        if (lotteryComplete) {
            label = "Finished";
            color = R.color.grey;
        } else if (registrationEnd.before(new Date())) {
            label = "Lottery Done";
            color = R.color.purple;
        } else {
            label = "Open";
            color = R.color.green;
        }

        statusChip.setText(label);
        statusChip.setChipBackgroundColor(
                ContextCompat.getColorStateList(getContext(), color)
        );
    }

    /**
     * Sets a click listener for when the user taps the event card
     * to open event details.
     * @param listener the {@link OnClickListener} to invoke when the card is clicked.
     */
    public void setOnOpenDetailsClickListener(OnClickListener listener) {
        card.setOnClickListener(listener);
    }
}
