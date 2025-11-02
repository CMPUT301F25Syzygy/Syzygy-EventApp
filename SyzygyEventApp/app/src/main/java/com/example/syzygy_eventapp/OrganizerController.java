package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;

import java.util.*;
import java.util.function.Consumer;

// WIP: Needs to be updated when Event and EventController is actually implemented
// I'm "giving myself" certain functions and assuming Events exist

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
            organizer.setOwnedEvents(new ArrayList<>());

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

        DocumentReference eventDoc = eventsRef.document(event.getEventID());
        DocumentReference organizerDoc = organizersRef.document(organizerID);

        // Step 1: add event to "events" collection
        return eventDoc.set(event).onSuccessTask(aVoid ->
                // Step 2: add event ID to organizer's ownedEvents array
                organizerDoc.update("ownedEvents", FieldValue.arrayUnion(event.getEventID()))
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
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            return "Event title cannot be empty.";
        }
        // no desc.
        if (event.getDescription() == null || event.getDescription().trim().isEmpty()) {
            return "Event description cannot be empty.";
        }
        // no date
        if (event.getDate() == null) {
            return "Event date is required.";
        }
        // valid
        return null;
    }

}
