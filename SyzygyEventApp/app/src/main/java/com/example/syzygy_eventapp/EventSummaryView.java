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
     * @param status the status of the current entrant, or {@code null} if admin view.
     * @param isAdmin whether the current user has admin privileges.
     */
    public void bind(Event event, Event.Status status, boolean isAdmin) {
        titleText.setText(event.getName());
        locationText.setText(event.getLocationName());

        if (event.getEventTime() != null) {
            Date date = event.getEventTime().toDate();
            DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
            DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
            timeText.setText(timeFormat.format(date));
            dateText.setText(dateFormat.format(date));
        }

        if (status != null) {
            setAttendeeChipColor(status);
        }

        adminButtons.setVisibility(isAdmin ? VISIBLE : GONE);
    }

    /**
     * Sets the color and label of the status chip based on the entrant’s event status.
     * Used for entrant-facing summaries.
     *
     * @param status the {@link Event.Status} of the user in the event.
     */
    private void setAttendeeChipColor(Event.Status status) {
        int color;
        String label;

        switch (status) {
            case Open:
                color = R.color.purple;
                label = "Open";
                break;
            case Accepted:
                color = R.color.green;
                label = "Accepted";
                break;
            case Pending:
                color = R.color.yellow;
                label = "Pending";
                break;
            case Declined:
                color = R.color.red;
                label = "Declined";
                break;
            case Waitlisted:
                color = R.color.blue;
                label = "Waitlist";
                break;
            case DrawnEarly:
                color = R.color.grey;
                label = "Drawn Early";
                break;
            case RegistrationOver:
                color = R.color.grey;
                label = "Registration Over";
                break;
            case EventOver:
                color = R.color.grey;
                label = "Event Over";
                break;
            default:
            case Unknown:
                color = R.color.grey;
                label = "Unknown";
                break;
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
