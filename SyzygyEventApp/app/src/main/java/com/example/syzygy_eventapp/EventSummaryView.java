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

public class EventSummaryView extends LinearLayout {

    private TextView titleText, timeText, locationText, dateText;
    private MaterialCardView card;
    private MaterialButton removeButton, toggleBannerButton;
    private Chip statusChip;
    private LinearLayout adminButtons;

    public enum AttendeeStatus { WAITLIST, NOT_SELECTED, PENDING, ACCEPTED, REJECTED }

    public EventSummaryView(Context context) {
        super(context);
        init(context);
    }

    public EventSummaryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EventSummaryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.fragment_event_summary_view, this, true);

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

    /** For entrant-side view (colored by status) */
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
        statusChip.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
    }

    /** For admin-side view (colored by state of event) */
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
        statusChip.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
    }

    public void setOnOpenDetailsClickListener(OnClickListener listener) {
        card.setOnClickListener(listener);
    }

    public void setOnToggleBannerClickListener(OnClickListener listener) {
        toggleBannerButton.setOnClickListener(listener);
    }

    public void setOnRemoveClickListener(OnClickListener listener) {
        removeButton.setOnClickListener(listener);
    }
}
