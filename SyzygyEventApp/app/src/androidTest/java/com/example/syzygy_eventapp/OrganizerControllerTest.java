package com.example.syzygy_eventapp;

import static org.junit.Assert.*;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * Integration tests for {@link OrganizerController}.
 * Tests Firestore interactions related to organizer data and owned event management.
 */
public class OrganizerControllerTest {

    private OrganizerController controller;
    private FirebaseFirestore db;
    private CollectionReference organizersRef;
    private CollectionReference eventsRef;
    private String organizerID = "testOrganizer123";
    private DocumentReference organizerDoc;
    private static final int TIMEOUT_SEC = 10;

    // --------------------------------------------------------------------------------------------------------------------------------------------- //

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        organizersRef = db.collection("organizers");
        eventsRef = db.collection("events");
        controller = new OrganizerController();
        organizerDoc = organizersRef.document(organizerID);

        // cleanup from any previous runs
        try {
            Tasks.await(organizerDoc.delete(), TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Ignore "not found" or network errors for setup
        }
    }

    @After
    public void tearDown() throws Exception {
        // cleanup after each test
        Tasks.await(organizerDoc.delete(), TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    // --------------------------------------------------------------------------------------------------------------------------------------------- //

    /**
     * Verify that {@link OrganizerController#createIfMissing(String, String, String)} creates a new organizer document
     * with default values when the document doesn't exist.
     */
    @Test
    public void testCreateIfMissing() throws Exception {
        String name = "Alice Organizer";
        String email = "alice@events.com";

        // Create new organizer in Firestore by calling the controller method with simple parameters
        Tasks.await(controller.createIfMissing(organizerID, name, email), TIMEOUT_SEC, TimeUnit.SECONDS);

        // Retrieve from Firestore
        DocumentSnapshot snap = Tasks.await(organizerDoc.get(), TIMEOUT_SEC, TimeUnit.SECONDS);
        assertTrue("Organizer document should exist", snap.exists());

        Organizer org = snap.toObject(Organizer.class);
        assertNotNull(org);
        assertEquals(organizerID, org.getUserID());
        assertEquals("Alice Organizer", org.getName());
        assertTrue(org.getRoles().contains(Role.ORGANIZER));
        assertNotNull(org.getOwnedEventIDs());
        assertTrue(org.getOwnedEventIDs().isEmpty());
    }

    /**
     * Tests that createIfMissing does NOT overwrite an existing organizer.
     */
    @Test
    public void testCreateIfMissing_DoesNotOverwriteExisting() throws Exception {
        Organizer existing = new Organizer();
        existing.setUserID(organizerID);
        existing.setName("Existing Name");
        existing.setEmail("existing@example.com");
        existing.setOwnedEventIDs(new ArrayList<>());

        Tasks.await(organizerDoc.set(existing), TIMEOUT_SEC, TimeUnit.SECONDS);

        // Try to "recreate" — should not overwrite
        Tasks.await(controller.createIfMissing(organizerID, "New Name", "new@example.com"), TIMEOUT_SEC, TimeUnit.SECONDS);

        Organizer fetched = Tasks.await(organizerDoc.get(), TIMEOUT_SEC, TimeUnit.SECONDS).toObject(Organizer.class);
        assertNotNull(fetched);
        assertEquals("Existing Name", fetched.getName());
        assertEquals("existing@example.com", fetched.getEmail());
    }

    /**
     * Tests that {@link OrganizerController#createEvent(String, Event)} creates a new event and updates organizer's owned list.
     */
    @Test
    public void testCreateEvent_AddsEventAndUpdatesOrganizer() throws Exception {
        Tasks.await(controller.createIfMissing(organizerID, "Alice", "alice@events.com"), TIMEOUT_SEC, TimeUnit.SECONDS);

        Event event = new Event();
        event.setName("Sample Test Event");
        event.setDescription("A test event for validation");
        event.setRegistrationStart(Timestamp.now());
        event.setRegistrationEnd(new Timestamp(Timestamp.now().getSeconds() + 3600, 0));
        event.setMaxAttendees(100);
        event.setMaxWaitingList(50);
        event.setGeolocationRequired(false);

        Tasks.await(controller.createEvent(organizerID, event), TIMEOUT_SEC, TimeUnit.SECONDS);

        DocumentSnapshot eventSnap = Tasks.await(eventsRef.document(event.getEventID()).get(), TIMEOUT_SEC, TimeUnit.SECONDS);
        assertTrue("Event document should exist", eventSnap.exists());
        Event savedEvent = eventSnap.toObject(Event.class);
        assertNotNull(savedEvent);
        assertEquals("Sample Test Event", savedEvent.getName());

        Organizer org = Tasks.await(organizerDoc.get(), TIMEOUT_SEC, TimeUnit.SECONDS).toObject(Organizer.class);
        assertNotNull(org);
        assertTrue(org.getOwnedEventIDs().contains(event.getEventID()));
    }

    /**
     * Tests that invalid events are rejected with proper validation messages.
     */
    @Test
    public void testCreateEvent_InvalidEventRejected() {
        Event invalid = new Event();
        invalid.setName(""); // invalid name
        invalid.setDescription("Missing proper title.");

        Exception ex = assertThrows(Exception.class, () -> {
            Tasks.await(controller.createEvent(organizerID, invalid));
        });

        assertTrue(ex.getMessage().contains("Event name cannot be empty"));
    }

}
