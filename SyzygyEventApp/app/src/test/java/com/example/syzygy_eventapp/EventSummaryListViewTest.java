package com.example.syzygy_eventapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ContextThemeWrapper;
import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Unit tests for {@link EventSummaryListView}. These tests run with Robolectric
 * and verify UI population, expand/collapse behavior, status-chip rendering,
 * admin-only UI visibility, and correct click propagation.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, manifest = Config.NONE)
public class EventSummaryListViewTest {

    private EventSummaryListView listView;

    /**
     * Creates a themed context and initializes a fresh EventSummaryListView
     * before each test.
     */
    @Before
    public void setUp() {
        ContextThemeWrapper themed =
                new ContextThemeWrapper(
                        ApplicationProvider.getApplicationContext(),
                        com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
                );

        listView = new EventSummaryListView(themed);
        listView.setTitle("Events");
    }

    /**
     * Returns a Date offset by the given number of days from the current time.
     *
     * @param days number of days from now (can be negative)
     * @return resulting Date
     */
    private static Date daysFromNow(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, days);
        return c.getTime();
    }

    /**
     * Creates a sample Event with the provided parameters.
     *
     * @param id event ID
     * @param name event name
     * @param regEnd registration end date
     * @param lotteryComplete whether lottery is complete
     * @return configured Event instance
     */
    private static Event makeEvent(String id, String name, Date regEnd, boolean lotteryComplete) {
        Event e = new Event();
        e.setEventID(id);
        e.setName(name);
        e.setLocationName("Edmonton, AB");
        e.setOrganizerID("user001");
        e.setRegistrationEnd(new Timestamp(regEnd));
        e.setLotteryComplete(lotteryComplete);
        return e;
    }

    /**
     * Produces a small, representative list of Events used by multiple tests.
     *
     * @return list of sample Events
     */
    private List<Event> sampleEvents3() {
        List<Event> list = new ArrayList<>();
        list.add(makeEvent("event1", "Event 1", daysFromNow(10), false));
        list.add(makeEvent("event2", "Event 2", daysFromNow(5), false));
        list.add(makeEvent("event-42", "Event 42", daysFromNow(-2), true));
        return list;
    }

    /**
     * Ensures that the item count label and the list container both reflect
     * the correct number of events after setItems() is called.
     */
    @Test
    public void testCountAndChildrenPopulate() {
        List<Event> events = sampleEvents3();

        listView.setItems(events, /*isAdmin=*/false, v -> {});

        TextView count = listView.findViewById(R.id.count);
        LinearLayout container = listView.findViewById(R.id.list_container);

        assertEquals("3", count.getText().toString());
        assertEquals(3, container.getChildCount());
    }

    /**
     * Verifies that clicking the header toggles the expand/collapse state of
     * the list container.
     */
    @Test
    public void testToggleExpandCollapse() {
        // Arrange: populate items
        listView.setItems(sampleEvents3(), false, v -> {});

        View header = listView.findViewById(R.id.header);
        View container = listView.findViewById(R.id.list_container);

        // Capture whatever the initial visibility is (VISIBLE or GONE)
        int initialVisibility = container.getVisibility();

        // First click should toggle it to the opposite
        header.performClick();
        assertTrue(container.getVisibility() != initialVisibility);

        // Second click should toggle it back to the original state
        header.performClick();
        assertEquals(initialVisibility, container.getVisibility());
    }

    /**
     * Confirms that the StatusProvider determines the correct chip label
     * rendered inside each EventSummaryView.
     */
    @Test
    public void testStatusProviderDrivesChipLabels() {
        List<Event> events = sampleEvents3();

        // ACCEPTED for event1, REJECTED for others
        EventSummaryListView.StatusProvider provider = e ->
                "event1".equals(e.getEventID())
                        ? EventSummaryView.AttendeeStatus.ACCEPTED
                        : EventSummaryView.AttendeeStatus.REJECTED;

        listView.setItems(
                events,
                /*isAdmin=*/false,
                provider,
                v -> {},
                v -> {},
                v -> {}
        );

        LinearLayout container = listView.findViewById(R.id.list_container);
        EventSummaryView row0 = (EventSummaryView) container.getChildAt(0);
        EventSummaryView row1 = (EventSummaryView) container.getChildAt(1);

        TextView chip0 = row0.findViewById(R.id.chip_event_status);
        TextView chip1 = row1.findViewById(R.id.chip_event_status);

        assertEquals("Accepted", chip0.getText().toString());
        assertEquals("Rejected", chip1.getText().toString());
    }

    /**
     * Verifies that admin controls are visible when the isAdmin flag is true.
     */
    @Test
    public void testAdminButtonsVisibleWhenIsAdminTrue() {
        List<Event> events = sampleEvents3();

        listView.setItems(events, /*isAdmin=*/true, v -> {});

        LinearLayout container = listView.findViewById(R.id.list_container);
        for (int i = 0; i < container.getChildCount(); i++) {
            EventSummaryView row = (EventSummaryView) container.getChildAt(i);
            View adminBar = row.findViewById(R.id.layout_admin_buttons);
            assertNotNull(adminBar);
            assertEquals(View.VISIBLE, adminBar.getVisibility());
        }
    }

    /**
     * Ensures that clicking an event row calls the listener with the Event
     * corresponding to that row via the view's tag.
     */
    @Test
    public void testRowClickPassesEventViaTag() {
        List<Event> events = sampleEvents3();
        final List<Event> clicked = new ArrayList<>(1);

        listView.setItems(
                events,
                /*isAdmin=*/false,
                null,
                v -> {
                    Object tag = v.getTag();
                    if (tag instanceof Event) {
                        clicked.add((Event) tag);
                    }
                },
                v -> {},
                v -> {}
        );

        listView.findViewById(R.id.header).performClick();

        LinearLayout container = listView.findViewById(R.id.list_container);
        EventSummaryView first = (EventSummaryView) container.getChildAt(0);
        View card = first.findViewById(R.id.event_banner_card);
        card.performClick();

        assertEquals(1, clicked.size());
        assertEquals("event1", clicked.get(0).getEventID());
        assertTrue(clicked.get(0).getLocationName().contains("Edmonton"));
    }
}
