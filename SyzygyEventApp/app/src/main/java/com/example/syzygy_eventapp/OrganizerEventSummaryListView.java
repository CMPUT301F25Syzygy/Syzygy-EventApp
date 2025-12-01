package com.example.syzygy_eventapp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Displays a titled, collapsible list of OrganizerEventSummaryFragment rows with a count.
 * Used primarily by OrganizerFragment.
 */
public class OrganizerEventSummaryListView extends LinearLayout {
    private LinearLayout listContainer;
    private ImageView arrow;
    private TextView titleText;
    private TextView countText;
    private boolean expanded = true;

    /**
     * Constructs the view programmatically.
     *
     * @param context the context in which this view is created
     */
    public OrganizerEventSummaryListView(Context context) { super(context); init(context); }
    /**
     * Constructs the view from XML.
     *
     * @param context the current context
     * @param attrs   the set of attributes from XML
     */
    public OrganizerEventSummaryListView(Context context, AttributeSet attrs) { super(context, attrs); init(context); }
    /**
     * Constructs the view from XML with a style attribute.
     *
     * @param context      the current context
     * @param attrs        the set of attributes from XML
     * @param defStyleAttr the default style to apply
     */
    public OrganizerEventSummaryListView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(context); }

    /**
     * Initializes and inflates the layout, sets up references, and applies the click listener
     * for toggling the expand/collapse state.
     *
     * @param context the current context
     */
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.fragment_organizer_event_summary_list, this, true);
        setOrientation(VERTICAL);

        listContainer = findViewById(R.id.list_container);
        arrow        = findViewById(R.id.arrow);
        titleText    = findViewById(R.id.title);
        countText    = findViewById(R.id.count);

        findViewById(R.id.header).setOnClickListener(v -> toggle());

        setExpanded(expanded);
    }

    /**
     * Sets the title shown in the header (e.g., "Events", "Upcoming", "History").
     *
     * @param title the title text to display
     */
    public void setTitle(String title) { titleText.setText(title); }

    /**
     * Expands or collapses the list with a short arrow rotation animation.
     *
     * @param expand {@code true} to expand the list, {@code false} to collapse it
     */
    public void setExpanded(boolean expand) {
        expanded = expand;
        listContainer.setVisibility(expanded ? VISIBLE : GONE);
        arrow.animate().rotation(expanded ? 90f : 0f).setDuration(120).start();
        arrow.setContentDescription(expanded ? "Collapse" : "Expand");
    }

    /**
     * Toggles between expanded and collapsed states.
     */
    public void toggle() { setExpanded(!expanded); }

    /**
     * Populates the list with a set of {@link OrganizerEventSummaryFragment} items and assigns callbacks for user actions.
     * <p>
     * This method is used when the caller needs full control (admin or entrant context), including
     * banner toggling and removal functionality.
     *
     * @param events              the list of {@link Event} objects to display
     * @param onRowClick          click listener for opening event details
     * @param onToggleBannerClick click listener for enabling/disabling banners
     * @param onRemoveClick       click listener for removing events
     */
    public void setItems(
            List<Event> events,
            OnClickListener onRowClick,
            OnClickListener onToggleBannerClick,
            OnClickListener onRemoveClick
    ) {
        listContainer.removeAllViews();

        int size = (events == null) ? 0 : events.size();
        countText.setText(String.valueOf(size));
        if (size == 0) return;

        for (Event event : events) {
            OrganizerEventSummaryFragment row = new OrganizerEventSummaryFragment(getContext());

            Event.Status status = event.calculateAbsoluteStatus();
            row.bind(event, status);


            row.setTag(event);

            row.setOnOpenDetailsClickListener(v -> {
                v.setTag(event);
                onRowClick.onClick(v);
            });

            listContainer.addView(row);
        }
    }

    /**
     * Simpler version of {@link #setItems(List, OnClickListener, OnClickListener, OnClickListener)}
     * that omits banner and removal functionality.
     * <p>
     * Useful for entrant-facing lists where events are only opened, not managed.
     *
     * @param events     the list of {@link Event} objects to display
     * @param onRowClick click listener for opening event details
     */
    public void setItems(List<Event> events, boolean isAdmin, OnClickListener onRowClick) {
        setItems(events, onRowClick, v -> {}, v -> {});
    }
}
