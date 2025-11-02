package com.example.syzygy_eventapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Organizer model class that extends the user. Will aggregate all events owned by the organizer.
 * OrganizerController and OrganizerTest will initialize values upon creating an Organizer.
 * Firestore syncing is handled by OrganizerController
 */
public class Organizer extends User {

    /** List of events owned by this organizer. */
    private List<Event> ownedEvents;

    /** Default constructor */
    public Organizer() {
        super();
        this.ownedEvents = new ArrayList<>();
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
        this.ownedEvents = new ArrayList<>();
    }

    public List<Event> getOwnedEvents() { return ownedEvents; }

    public void setOwnedEvents(List<Event> ownedEvents) { this.ownedEvents = ownedEvents; }

    /** Add a new event to the organizer's owned list (LOCAL MODEL ONLY). */
    public void addOwnedEvent(Event event) {
        if (event != null && !ownedEvents.contains(event)) {
            ownedEvents.add(event);
        }
    }
}
