package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Controller for reading/writing {@link Event, Invitation} data in Firestore DB.
 * EventViews will call this class to create, update, and observe events.
 * Firestore is the source of truth; views have real-time listeners.
 */
public class EventController {

    private final FirebaseFirestore db;
    private final CollectionReference eventsRef;

    public EventController() {
        this.db = FirebaseFirestore.getInstance();
        this.eventsRef = db.collection("events");
    }

    //-----------------------
    // EVENT CREATION
    //-----------------------
    /**
     * Create a new event with provided details.
     * @param event Event object with the details filled in.
     * @return Task that completes when the document is created.
     * @throws IllegalArgumentException if any required fields are missing.
     */
    public Task<String> createEvent(Event event) {
        if (event == null || event.getName() == null || event.getOrganizerID() == null) {
            return Tasks.forException(new IllegalArgumentException("Event name and organizerID are required"));
        }

        // Create a new document in the events collection
        DocumentReference doc = eventsRef.document();
        String eventID = doc.getId();
        event.setEventID(eventID);

        // Set defaults if not provided
        if (event.getWaitingList() == null) {
            event.setWaitingList(new ArrayList<>());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("eventID", eventID);
        data.put("name", event.getName());
        data.put("description", event.getDescription());
        data.put("organizerID", event.getOrganizerID());
        data.put("locationName", event.getLocationName());
        data.put("locationCoordinates", event.getLocationCoordinates());
        data.put("geolocationRequired", event.isGeolocationRequired());
        data.put("posterUrl", event.getPosterUrl());
        data.put("waitingList", event.getWaitingList());
        data.put("maxWaitingList", event.getMaxWaitingList());
        data.put("registrationStart", event.getRegistrationStart());
        data.put("registrationEnd", event.getRegistrationEnd());
        data.put("maxAttendees", event.getMaxAttendees());
        data.put("lotteryComplete", false); // Always start as false
        data.put("qrCodeData", event.getQrCodeData());
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        return doc.set(data).continueWith(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            return eventID;
        });
    }

    //-----------------------
    // EVENT OBSERVERS
    //-----------------------
    /**
     * Real time observers; observes a single event in real time
     * @param eventID Event document ID to observe
     * @param onEventChange Callback invoked with the latest Event object
     * @param onError Callback invoked on listener errors
     * @return ListenerRegistration that must be removed when no longer needed
     * @throws IllegalArgumentException if eventID is null/empty
     */
    public ListenerRegistration observeEvent(String eventID, Consumer<Event> onEventChange, Consumer<Exception> onError) {
        if (eventID == null || eventID.isEmpty()) {
            throw new IllegalArgumentException("eventID is required");
        }
        DocumentReference doc = eventsRef.document(eventID);
        return doc.addSnapshotListener((snap, error) -> {
            // snap can't be null because none of it's implementations can return null
            if (error != null) {
                onError.accept(error);
                return;
            }
            if (snap == null || !snap.exists()) {
                onError.accept(new IllegalStateException("Event: " + eventID + " not found."));
                return;
            }
            // Convert to Event object
            Event event = snap.toObject(Event.class);
            if (event != null) {
                if (event.getEventID() == null) {
                    event.setEventID(snap.getId());
                    onEventChange.accept(event);
                }
                onEventChange.accept(event);
            }
        });

    }

    /**
     * Observe all events in real time.
     * @param onChange Callback invoked with the latest list of Event objects
     * @param onError Callback invoked on listener errors
     * @return ListenerRegistration that must be removed when no longer needed
     */
    public ListenerRegistration observeAllEvents(Consumer<List<Event>> onChange, Consumer<Exception> onError) {
        return eventsRef.addSnapshotListener((snap, error) -> {
            if (error != null) {
                onError.accept(error);
                return;
            }

            List<Event> events = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Event event = doc.toObject(Event.class);
                    if (event != null) {
                        event.setEventID(doc.getId());
                        events.add(event);
                    }
                }
            }
            onChange.accept(events);
        });
    }

    /**
     * Observe all events owned by an organizer in real time.
     * @param organizerID Organizer document ID to observe
     * @param onChange Callback invoked with the latest list of Event objects
     * @param onError Callback invoked on listener errors
     * @return ListenerRegistration that must be removed when no longer needed
     * @throws IllegalArgumentException if organizerID is null/empty
     */
    public ListenerRegistration observeOrganizerEvents(String organizerID, Consumer<List<Event>> onChange, Consumer<Exception> onError) {
        if (organizerID == null || organizerID.isEmpty()) {
            throw new IllegalArgumentException("organizerID is required");
        }

        return eventsRef.whereEqualTo("organizerID", organizerID)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        onError.accept(error);
                        return;
                    }

                    List<Event> events = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                event.setEventID(doc.getId());
                                events.add(event);
                            }
                        }
                    }
                    onChange.accept(events);
                });
    }

    /**
     * Observe entrant locations for an event in real time.
     * @param eventID Event document ID
     * @param onChange Callback with list of location data maps
     * @param onError Error callback
     * @return ListenerRegistration to remove when done
     */
    public ListenerRegistration observeEntrantLocations(String eventID, Consumer<List<Map<String, Object>>> onChange, Consumer<Exception> onError) {
        if (eventID == null || eventID.isEmpty()) {
            throw new IllegalArgumentException("eventID is required");
        }

        return db.collection("events").document(eventID)
                .collection("entrantLocations")
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        onError.accept(error);
                        return;
                    }

                    List<Map<String, Object>> locations = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            locations.add(doc.getData());
                        }
                    }
                    onChange.accept(locations);
                });
    }

    //-----------------------
    // WAITING LIST OPERATIONS
    //-----------------------
    /**
     * Get the current number of Entrants on a waiting list for an event.
     * @param eventID Event document ID
     * @return Task that completes with the count of users on the waiting list
     * @throws IllegalArgumentException if eventID is null/empty
     */
    public Task<Integer> getWaitingListSize(String eventID) {
        if (eventID == null || eventID.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("eventID is required"));
        }
        return eventsRef.document(eventID).get().continueWith(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            DocumentSnapshot snap = task.getResult();
            if (snap == null || !snap.exists()) {
                throw new IllegalStateException("Event: " + eventID + " not found.");
            }
            Event event = snap.toObject(Event.class);
            if (event == null || event.getWaitingList() == null) {
                return 0;
            }
            return event.getWaitingList().size();
        });
    }

    /**
     * Add a user to the waiting list for an event.
     * @param eventID Event document ID
     * @param userID User document ID
     * @return Task that completes when the user is added to the waiting list
     * @throws IllegalArgumentException if any parameter is null/empty
     */
    public Task<Void> addToWaitingList(String eventID, String userID, GeoPoint userLocation) {
        if (eventID == null || eventID.isEmpty() || userID == null || userID.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("eventID and userID are required"));
        }
        DocumentReference doc = eventsRef.document(eventID);
        return doc.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(Objects.requireNonNull(task.getException()));
            }
            DocumentSnapshot snap = task.getResult();
            if (snap == null || !snap.exists()) {
                return Tasks.forException(new IllegalStateException("Event: " + eventID + " not found."));
            }
            Event event = snap.toObject(Event.class);
            if (event == null) {
                return Tasks.forException(new IllegalStateException("Event failed to load."));
            }
            List<String> waitingList = event.getWaitingList();
            if (waitingList == null) {
                waitingList = new ArrayList<>();
            }
            // Check if user already on list
            if (waitingList.contains(userID)) {
                return Tasks.forException(new IllegalStateException("User already on waiting list"));
            }
            // Check if waiting list is full
            if (event.getMaxWaitingList() != null && waitingList.size() >= event.getMaxWaitingList()) {
                return Tasks.forException(new IllegalStateException("Waiting list is full"));
            }
            waitingList.add(userID);

            Map<String, Object> updates = new HashMap<>();
            updates.put("waitingList", waitingList);
            updates.put("updatedAt", FieldValue.serverTimestamp());

            // Store location data in a subcollection if provided
            Task<Void> updateTask = doc.update(updates);

            if (userLocation != null) {
                return updateTask.continueWithTask(t -> {
                    if (!t.isSuccessful()) {
                        return Tasks.forException(Objects.requireNonNull(t.getException()));
                    }
                    // Store location in subcollection
                    Map<String, Object> locationData = new HashMap<>();
                    locationData.put("userID", userID);
                    locationData.put("location", userLocation);
                    locationData.put("joinedAt", FieldValue.serverTimestamp());

                    return db.collection("events").document(eventID)
                            .collection("entrantLocations").document(userID).set(locationData);
                });
            }

            return updateTask;

            // return doc.update("waitingList", waitingList, "updatedAt", FieldValue.serverTimestamp());
        });

    }

    /**
     * Remove a user from the waiting list for an event.
     * @param eventID Event document ID
     * @param userID User document ID
     * @return Task that completes when the user is removed from the waiting list
     * @throws IllegalArgumentException if any parameter is null/empty
     */
    public Task<Void> removeFromWaitingList(String eventID, String userID) {
        if (eventID == null || eventID.isEmpty() || userID == null || userID.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("eventID and userID are required"));
        }
        DocumentReference doc = eventsRef.document(eventID);
        return doc.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(Objects.requireNonNull(task.getException()));
            }
            DocumentSnapshot snap = task.getResult();
            if (snap == null || !snap.exists()) {
                return Tasks.forException(new IllegalStateException("Event: " + eventID + " not found."));
            }
            Event event = snap.toObject(Event.class);
            if (event == null) {
                return Tasks.forException(new IllegalStateException("Event failed to load."));
            }
            List<String> waitingList = event.getWaitingList();
            if (waitingList == null || !waitingList.contains(userID)) {
                return Tasks.forException(new IllegalStateException("User not on waiting list"));
            }
            waitingList.remove(userID);
            return doc.update("waitingList", waitingList, "updatedAt", FieldValue.serverTimestamp());
        });
    }

    //-----------------------
    // EVENT UPDATES OR DELETE
    //-----------------------
    /**
     * Update event details.
     * @param eventID Event document ID
     * @param updates Map of field names to new values
     * @return Task that completes when the event is updated
     * @throws IllegalArgumentException if any parameter is null/empty
     */
    public Task<Void> updateEvent(String eventID, Map<String, Object> updates) {
        if (eventID == null || eventID.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("eventID is required"));
        }
        if (updates == null || updates.isEmpty()) {
            return Tasks.forResult(null);
        }
        // Always update the updatedAt timestamp
        updates.put("updatedAt", FieldValue.serverTimestamp());
        return eventsRef.document(eventID).update(updates);
    }

    public Task<Void> deleteEvent(String eventID) {
        if (eventID == null || eventID.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("eventID is required"));
        }
        return eventsRef.document(eventID).delete();
    }
}

