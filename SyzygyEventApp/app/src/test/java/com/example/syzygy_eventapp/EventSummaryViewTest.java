package com.example.syzygy_eventapp;

import static org.junit.Assert.*;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.Timestamp;
import com.google.android.material.chip.Chip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

/**
 * Unit tests for {@link EventSummaryView}.
 * Verifies correct binding of text, chip labels, and visibility logic.
 */
@RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 34, manifest = org.robolectric.annotation.Config.NONE)
public class EventSummaryViewTest {
    private Context context;
    private EventSummaryView view;

    @Before
    public void setUp() {
        Context base = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        Context themed = new android.view.ContextThemeWrapper(
                base, com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
        );

        view = new EventSummaryView(themed);
        context = themed;
    }

    private static Timestamp tsDaysFromNow(int days) {
        long ms = System.currentTimeMillis() + days * 86_400_000L;
        return new Timestamp(new Date(ms));
    }

    private Event makeEvent(String title, String location, int daysUntilEnd, boolean lotteryComplete) {
        Event e = new Event();
        e.setName(title);
        e.setLocationName(location);
        e.setRegistrationEnd(tsDaysFromNow(daysUntilEnd));
        e.setLotteryComplete(lotteryComplete);
        return e;
    }

    @Test
    public void testBindSetsTitleAndLocation() {
        Event e = makeEvent("Science Fair", "Enterprise Square", 2, false);
        view.bind(e, null, false);

        TextView title = view.findViewById(R.id.event_title);
        TextView location = view.findViewById(R.id.event_location);

        assertEquals("Science Fair", title.getText().toString());
        assertEquals("Enterprise Square", location.getText().toString());
    }

    @Test
    public void testBindSetsTimeAndDate() {
        Event e = makeEvent("Hackathon", "Campus Hall", 3, false);
        view.bind(e, null, false);

        TextView timeText = view.findViewById(R.id.event_time);
        TextView dateText = view.findViewById(R.id.event_date);

        assertFalse(timeText.getText().toString().isEmpty());
        assertFalse(dateText.getText().toString().isEmpty());
    }

    @Test
    public void testBindEntrantAcceptedShowsAcceptedChip() {
        Event e = makeEvent("Title", "Loc", 1, false);
        view.bind(e, EventSummaryView.AttendeeStatus.ACCEPTED, false);

        Chip chip = view.findViewById(R.id.chip_event_status);
        assertEquals("Accepted", chip.getText().toString());
    }

    @Test
    public void testBindAdminFutureEventShowsOpenAndAdminButtonsVisible() {
        Event e = makeEvent("Future Event", "Arena", 5, false);
        view.bind(e, null, true);

        Chip chip = view.findViewById(R.id.chip_event_status);
        assertEquals("Open", chip.getText().toString());

        View adminButtons = view.findViewById(R.id.layout_admin_buttons);
        assertEquals(View.VISIBLE, adminButtons.getVisibility());
    }

    @Test
    public void testBindEntrantHidesAdminButtons() {
        Event e = makeEvent("User Event", "Library", 5, false);
        view.bind(e, EventSummaryView.AttendeeStatus.PENDING, false);

        View adminButtons = view.findViewById(R.id.layout_admin_buttons);
        assertEquals(View.GONE, adminButtons.getVisibility());
    }

    @Test
    public void testOpenDetailsClickListenerIsTriggered() {
        final boolean[] clicked = {false};
        view.setOnOpenDetailsClickListener(v -> clicked[0] = true);

        View card = view.findViewById(R.id.event_banner_card);
        card.performClick();

        assertTrue(clicked[0]);
    }
}

