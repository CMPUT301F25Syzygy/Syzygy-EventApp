package com.example.syzygy_eventapp;

import static org.junit.Assert.*;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for {@link EventController}.
 * These tests verify Firestore interactions without using mocks.
 */
public class EventControllerTest {

    private static final int TIMEOUT_SEC = 10;

    private static FirebaseFirestore db;
    private static EventController controller;

    private static String organizerID;
    private static final ArrayList<String> createdEventIds = new ArrayList<>();

    @BeforeClass
    public static void setUp() throws Exception {
        try {
            FirebaseApp.initializeApp(
                    InstrumentationRegistry.getInstrumentation().getTargetContext());
        } catch (IllegalStateException ignore) {}

        db = FirebaseFirestore.getInstance();
        controller = new EventController();

        String run = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        organizerID = "organizer_" + run;
    }

    /**
     * Delete all the events created during this test.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        Tasks.await(
                BatchDeleter.deleteCollectionIds("events", createdEventIds),
                30, TimeUnit.SECONDS);
    }

    private DocumentSnapshot getEvent(String id) throws Exception {
        return Tasks.await(
                db.collection("events").document(id).get(),
                TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    // ------------------------
    // TESTS
    // ------------------------

    /**
     * Verify createEvent stores all provided data and sets defaults.
     */
    @Test
    public void testCreateEvent() throws Exception {
        Event event = new Event();
        event.setName("Sample Event");
        event.setDescription("A test event for Firestore.");
        event.setOrganizerID(organizerID);
        event.setLocationName("Test Location");
        event.setLocationCoordinates(new GeoPoint(53.5461, -113.4938));
        event.setGeolocationRequired(true);
        event.setPosterUrl("http://example.com/poster.jpg");
        event.setMaxAttendees(100);
        event.setQrCodeData("qr12345");

        String id = Tasks.await(controller.createEvent(event),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        createdEventIds.add(id);

        assertNotNull(id);
        DocumentSnapshot snap = getEvent(id);
        assertTrue(snap.exists());
        assertEquals("Sample Event", snap.getString("name"));
        assertEquals("A test event for Firestore.", snap.getString("description"));
        assertEquals(organizerID, snap.getString("organizerID"));
        assertEquals("Test Location", snap.getString("locationName"));
        assertEquals("http://example.com/poster.jpg", snap.getString("posterUrl"));
        assertEquals(false, snap.getBoolean("lotteryComplete"));
        assertNotNull(snap.get("createdAt"));
        assertNotNull(snap.get("updatedAt"));
    }

    /**
     * Verify waiting list add/remove operations update Firestore correctly.
     */
    @Test
    public void testWaitingListOperations() throws Exception {
        // Create event
        Event event = new Event();
        event.setName("Waiting List Event");
        event.setOrganizerID(organizerID);
        String id = Tasks.await(controller.createEvent(event),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        createdEventIds.add(id);

        // Initially empty
        int size = Tasks.await(controller.getWaitingListSize(id),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        assertEquals(0, size);

        // Add user
        Tasks.await(controller.addToWaitingList(id, "user1"),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        size = Tasks.await(controller.getWaitingListSize(id),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        assertEquals(1, size);

        // Remove user
        Tasks.await(controller.removeFromWaitingList(id, "user1"),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        size = Tasks.await(controller.getWaitingListSize(id),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        assertEquals(0, size);
    }

    /**
     * Verify updateEvent modifies Firestore fields.
     */
    @Test
    public void testUpdateEvent() throws Exception {
        Event event = new Event();
        event.setName("Update Test");
        event.setOrganizerID(organizerID);
        String id = Tasks.await(controller.createEvent(event),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        createdEventIds.add(id);

        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Updated Description");
        updates.put("maxAttendees", 42);
        Tasks.await(controller.updateEvent(id, updates),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        DocumentSnapshot snap = getEvent(id);
        assertEquals("Updated Description", snap.getString("description"));
        assertEquals(42L, snap.getLong("maxAttendees").longValue());
    }

    /**
     * Verify deleteEvent removes the document.
     */
    @Test
    public void testDeleteEvent() throws Exception {
        Event event = new Event();
        event.setName("Delete Me");
        event.setOrganizerID(organizerID);
        String id = Tasks.await(controller.createEvent(event),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        createdEventIds.add(id);

        DocumentSnapshot before = getEvent(id);
        assertTrue(before.exists());

        Tasks.await(controller.deleteEvent(id), TIMEOUT_SEC, TimeUnit.SECONDS);

        DocumentSnapshot after = getEvent(id);
        assertFalse(after.exists());
    }

    /**
     * Verify addToWaitingList prevents duplicates.
     */
    @Test
    public void testAddToWaitingList_DuplicateFails() throws Exception {
        Event event = new Event();
        event.setName("Duplicate Test");
        event.setOrganizerID(organizerID);
        String id = Tasks.await(controller.createEvent(event),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        createdEventIds.add(id);

        // Add once
        Tasks.await(controller.addToWaitingList(id, "userA"),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        // Add again should fail
        Exception ex = assertThrows(Exception.class, () ->
                Tasks.await(controller.addToWaitingList(id, "userA"),
                        TIMEOUT_SEC, TimeUnit.SECONDS));
        assertNotNull(ex);
    }
}
