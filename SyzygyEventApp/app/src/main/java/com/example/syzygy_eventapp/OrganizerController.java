package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.util.*;
import java.util.function.Consumer;


/**
 * Controller for reading and writing {@link Organizer} data in Firestore DB.
 * <p>
 *     The OrganizerController allows organizers to create new events and validate event info
 *     OrganizerView classes will use this to sync Organizer data between the app and Firestore.
 * </p>
 */
public class OrganizerController {

    private final FirebaseFirestore db;
    private final CollectionReference organizersRef;
    private final CollectionReference eventsRef;

    /**
     * Default constructor initializing Firestore and its collections
     */
    public OrganizerController() {
        this.db = FirebaseFirestore.getInstance();
        this.organizersRef = db.collection("organizers");
        this.eventsRef = db.collection("events");
    }

    /**
     * Create a new organizer profile in Firestore if missing.
     * @param organizerID Unique organizer ID (same as UserID)
     * @param name Organizer display name
     * @param email Organizer email
     * @return Task that completes when the document is created (or already exists)
     */
    public Task<Void> createIfMissing(String organizerID, String name, String email) {
        DocumentReference doc = organizersRef.document(organizerID);

        return doc.get().continueWithTask(task -> {
            DocumentSnapshot snap = task.getResult();
            if (snap != null && snap.exists()) {
                // already exists, don't need to create again
                return Tasks.forResult(null);
            }

            // create organizer with defaults
            Organizer organizer = new Organizer();
            organizer.setUserID(organizerID);
            organizer.setName(name);
            organizer.setEmail(email);
            organizer.setOwnedEventIDs(new ArrayList<>());

            // ensure roles are initialized
            organizer.setRoles(Arrays.asList(Role.ORGANIZER));
            organizer.setActiveRole(Role.ORGANIZER);

            // initial write
            return doc.set(organizer);
        });
    }

    /**
     * Create a new event under this organizer and store it in Firestore.
     * @param organizerID The organizer creating the event
     * @param event The event object to be created
     * @return Task that completes when both the event and organizer’s list are updated
     */

    public Task<Void> createEvent(String organizerID, Event event) {
        if (event == null || organizerID == null || organizerID.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Invalid organizer or event."));
        }

        // Assign an ID if missing
        if (event.getEventID() == null || event.getEventID().isEmpty()) {
            event.setEventID(eventsRef.document().getId());
        }

        // Validate event info
        String validationError = validateEvent(event);
        if (validationError != null) {
            return Tasks.forException(new IllegalArgumentException(validationError));
        }

        // Set timestamps for when the event was created
        event.setCreatedAt(Timestamp.now());
        event.setUpdatedAt(Timestamp.now());

        DocumentReference eventDoc = eventsRef.document(event.getEventID());
        DocumentReference organizerDoc = organizersRef.document(organizerID);

        // Step 1: add event to "events" collection
        return eventDoc.set(event).onSuccessTask(aVoid ->
                // Step 2: add event ID to organizer's ownedEvents array
                organizerDoc.update("ownedEventIDs", FieldValue.arrayUnion(event.getEventID()))
        );
    }

    /**
     * Observes changes to a specific organizer document.
     * @param organizerID The ID of the organizer to listen to
     * @param onOrganizer Callback invoked when the organizer data changes
     * @param onError Callback invoked if there’s a Firestore error
     * @return ListenerRegistration for stopping observation
     */
    public ListenerRegistration observeOrganizer(String organizerID, Consumer<Organizer> onOrganizer, Consumer<Exception> onError) {
        DocumentReference doc = organizersRef.document(organizerID);
        // setup a snapshot listener
        return doc.addSnapshotListener((snap, error) -> {
            // error
            if (error != null) {
                onError.accept(error);
                return;
            }
            // no document, stop processing
            if (snap == null || !snap.exists()) {
                return;
            }
            // Convert snapshot to Organizer
            Organizer organizer = snap.toObject(Organizer.class);
            if (organizer != null) {
                onOrganizer.accept(organizer);
            }
        });
    }

    /**
     * Validates an event’s required fields before it’s created.
     * @param event The event to validate
     * @return Null if valid, or an error message string if invalid
     */
    private String validateEvent(Event event) {
        // no name
        if (event.getName() == null || event.getName().trim().isEmpty()) {
            return "Event name cannot be empty.";
        }
        // no desc.
        if (event.getDescription() == null || event.getDescription().trim().isEmpty()) {
            return "Event description cannot be empty.";
        }
        // check the registration period
        Timestamp start = event.getRegistrationStart();
        Timestamp end = event.getRegistrationEnd();
        if (start != null && end != null) {
            if (end.compareTo(start) < 0) {
                return "Registration end must be after start.";
            }
        }
        // waiting list limits
        if (event.getMaxWaitingList() != null && event.getMaxWaitingList() < 0) {
            return "Max waiting list size cannot be negative.";
        }
        // attendee limit
        if (event.getMaxAttendees() != null && event.getMaxAttendees() < 0) {
            return "Max attendees cannot be negative.";
        }
        // geolocation (IF REQUIRED)
        if (event.isGeolocationRequired()) {
            if (event.getLocationCoordinates() == null &&
                    (event.getLocationName() == null || event.getLocationName().trim().isEmpty())) {
                return "Location is required for this event.";
            }
        }
        // valid
        return null;
    }

    /**
     * Gets all Event objects owned by the given organizer. Loads the organizer’s list of event IDs, then gets each Event from Firestore.
     * @param organizerID The ID of the organizer
     * @return Task that resolves to a list of Event objects (possibly empty if none)
     */
    public Task<List<Event>> getOwnedEvents(String organizerID) {
        DocumentReference organizerDoc = organizersRef.document(organizerID);

        return organizerDoc.get().continueWithTask(task -> {
            DocumentSnapshot snap = task.getResult();

            if (snap == null || !snap.exists()) {
                return Tasks.forException(new IllegalStateException("Organizer not found: " + organizerID));
            }

            Organizer organizer = snap.toObject(Organizer.class);
            if (organizer == null || organizer.getOwnedEventIDs() == null || organizer.getOwnedEventIDs().isEmpty()) {
                return Tasks.forResult(new ArrayList<>());
            }

            List<Task<DocumentSnapshot>> fetchTasks = new ArrayList<>();
            for (String eventID : organizer.getOwnedEventIDs()) {
                fetchTasks.add(eventsRef.document(eventID).get());
            }

            return Tasks.whenAllSuccess(fetchTasks).continueWith(allTask -> {
                List<Event> events = new ArrayList<>();
                for (Object obj : allTask.getResult()) {
                    DocumentSnapshot doc = (DocumentSnapshot) obj;
                    Event event = doc.toObject(Event.class);
                    if (event != null) {
                        events.add(event);
                    }
                }
                return events;
            });
        });
    }

}
