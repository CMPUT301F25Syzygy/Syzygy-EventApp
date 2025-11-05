package com.example.syzygy_eventapp;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
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
    private List<String> ownedEventIDs = new ArrayList<>();

    /** Default constructor */
    public Organizer() {
        super();
        this.setRole(Role.ORGANIZER);
    }

    /** Constructor with all values. */
    public Organizer(String userID, @NonNull String name, String email, String phone, String photoURL, boolean photoHidden, boolean demoted, List<String> ownedEventIDs, Role role) {
        super(userID, name, email, phone, photoURL, photoHidden, demoted, role);
        this.ownedEventIDs = ownedEventIDs;
    }

    public List<String> getOwnedEventIDs() {
        return ownedEventIDs;
    }

    public Task<Void> setOwnedEventIDs(List<String> ownedEventIDs) {
        this.ownedEventIDs = ownedEventIDs;

        return updateDB(new HashMap<>() {{
            put("ownedEventIDs", ownedEventIDs);
        }});
    }

    /** Add a new event ID to the organizer's owned list and update DB. */
    public Task<Void> addOwnedEventID(String eventID) {
        if (!ownedEventIDs.contains(eventID)) {
            ownedEventIDs.add(eventID);
            return updateDB(new HashMap<>() {{
                put("ownedEventIDs", ownedEventIDs);
            }});
        } else {
            return Tasks.forResult(null);
        }
    }

    /** Remove an event ID from the owned list and update DB. */
    public Task<Void> removeOwnedEventID(String eventID) {
        ownedEventIDs.remove(eventID);

        return updateDB(new HashMap<>() {{
            put("ownedEventIDs", ownedEventIDs);
        }});
    }

    /** Add a new event ID to the organizer's owned list and update DB. */
    public boolean hasOwnedEventID(String eventID) {
        return ownedEventIDs.contains(eventID);
    }

    @Override
    public User demote() {
        return new User(this.getUserID(), this.getName(), this.getEmail(), this.getPhone(), this.getPhotoURL(), this.isPhotoHidden(), true, Role.ENTRANT);
    }

    @Override
    public Organizer promote() {
        return new Admin(this.getUserID(), this.getName(), this.getEmail(), this.getPhone(), this.getPhotoURL(), this.isPhotoHidden(), this.isDemoted(), this.getOwnedEventIDs(), Role.ADMIN);
    }
}
