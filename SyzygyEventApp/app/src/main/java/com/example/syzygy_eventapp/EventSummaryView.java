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

import java.text.DateFormat;
import java.util.Date;

/**
 * A custom view that displays a compact summary of an event, including its title,
 * location, date, time, and status. It adapts its appearance based on whether the
 * viewer is an entrant or an admin, showing different status indicators and action
 * buttons accordingly. Used within event lists to give users a quick overview before
 * viewing full event details.
 */
public class EventSummaryView extends LinearLayout {

    private TextView titleText, timeText, locationText, dateText;
    private MaterialCardView card;
    private MaterialButton removeButton, toggleBannerButton;
    private Chip statusChip;
    private LinearLayout adminButtons;

    /**
     * Represents the status of an entrant for a given event.
     */
    public enum AttendeeStatus { WAITLIST, NOT_SELECTED, PENDING, ACCEPTED, REJECTED }

    /**
     * Default constructor for inflating via code.
     * @param context the current {@link Context}.
     */
    public EventSummaryView(Context context) {
        super(context);
        init(context);
    }

    /**
     * Constructor called when inflating from XML.
     * @param context the current {@link Context}.
     * @param attrs the {@link AttributeSet} from XML.
     */
    public EventSummaryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructor called when inflating from XML with a style attribute.
     * @param context the current {@link Context}.
     * @param attrs the {@link AttributeSet} from XML.
     * @param defStyleAttr the default style to apply to this view.
     */
    public EventSummaryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Initializes and inflates the layout, binding all child views.
     * @param context the current {@link Context}.
     */
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_event_summary, this, true);

        titleText = findViewById(R.id.event_title);
        timeText = findViewById(R.id.event_time);
        locationText = findViewById(R.id.event_location);
        dateText = findViewById(R.id.event_date);
        card = findViewById(R.id.event_banner_card);
        removeButton = findViewById(R.id.button_remove_event);
        toggleBannerButton = findViewById(R.id.button_toggle_banner);
        statusChip = findViewById(R.id.chip_event_status);
        adminButtons = findViewById(R.id.layout_admin_buttons);
    }

    /**
     * Binds an {@link Event} object to the summary view, updating text fields and chip color.
     * Also controls admin button visibility.
     *
     * @param event the {@link Event} to display.
     * @param attendeeStatus the status of the current entrant, or {@code null} if admin view.
     * @param isAdmin whether the current user has admin privileges.
     */
    public void bind(Event event, AttendeeStatus attendeeStatus, boolean isAdmin) {
        titleText.setText(event.getName());
        locationText.setText(event.getLocationName());

        Date date = event.getRegistrationEnd().toDate();
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        timeText.setText(timeFormat.format(date));
        dateText.setText(dateFormat.format(date));

        if (attendeeStatus != null) {
            setAttendeeChipColor(attendeeStatus);
        } else {
            setAdminChipColor(event.isLotteryComplete(), event.getRegistrationEnd().toDate());
        }

        adminButtons.setVisibility(isAdmin ? VISIBLE : GONE);
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

    /**
     * Sets a click listener for when the admin toggles an event’s banner.
     * @param listener the {@link OnClickListener} to invoke when the banner button is clicked.
     */
    public void setOnToggleBannerClickListener(OnClickListener listener) {
        toggleBannerButton.setOnClickListener(listener);
    }

    /**
     * Sets a click listener for when the admin removes an event.
     * @param listener the {@link OnClickListener} to invoke when the remove button is clicked.
     */
    public void setOnRemoveClickListener(OnClickListener listener) {
        removeButton.setOnClickListener(listener);
    }
}
