package com.example.syzygy_eventapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Organizer model class that extends the user. Will aggregate all events owned by the organizer.
 * OrganizerController and OrganizerTest will initialize values upon creating an Organizer.
 * Firestore syncing is handled by OrganizerController
 */
public class Organizer extends User {

    /** List of events IDs owned by this organizer (not storing whole Event objects). */
    // Full Event objects are already stored in Events
    // Organizer owns list of EventIDs, Event has organizerID
    private List<String> ownedEventIDs;

    /** Default constructor */
    public Organizer() {
        super();
        this.ownedEventIDs = new ArrayList<>();
    }

    /** Constructor with all values. */
    public Organizer(User baseUser) {
        super(
                baseUser.getUserID(),
                baseUser.getName(),
                baseUser.getEmail(),
                baseUser.getPhone(),
                baseUser.getPhotoURL(),
                baseUser.isPhotoHidden(),
                baseUser.isDemoted(),
                baseUser.getRoles(),
                baseUser.getActiveRole()
        );
        this.ownedEventIDs = new ArrayList<>();
    }

    public List<String> getOwnedEventIDs() { return ownedEventIDs; }

    public void setOwnedEventIDs(List<String> ownedEventIDs) { this.ownedEventIDs = ownedEventIDs; }

    /** Add a new event ID to the organizer's owned list (LOCAL MODEL ONLY). */
    public void addOwnedEventID(String eventID) {
        if (eventID != null && !eventID.trim().isEmpty() && !ownedEventIDs.contains(eventID)) {
            ownedEventIDs.add(eventID);
        }
    }

    /** Remove an event ID from the owned list (LOCAL MODEL ONLY). */
    public void removeOwnedEventID(String eventID) {
        ownedEventIDs.remove(eventID);
    }
}
