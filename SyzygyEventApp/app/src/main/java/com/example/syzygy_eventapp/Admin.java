package com.example.syzygy_eventapp;


import androidx.annotation.NonNull;

import java.util.List;

/**
 * Placeholder class to be populated later
 */
public class Admin extends Organizer {
    /** Default constructor */
    public Admin() {
        super();
        this.setRole(Role.ADMIN);
    }

    /** Constructor with all values. */
    public Admin(String userID, @NonNull String name, String email, String phone, String photoURL, boolean photoHidden, boolean demoted, List<String> ownedEventIDs, Role role) {
        super(userID, name, email, phone, photoURL, photoHidden, demoted, ownedEventIDs, role);
    }

    @Override
    public Organizer demote() {
        return new Organizer(this.getUserID(), this.getName(), this.getEmail(), this.getPhone(), this.getPhotoURL(), this.isPhotoHidden(), true, this.getOwnedEventIDs(), Role.ORGANIZER);
    }

    @Override
    public Admin promote() {
        return this;
    }
}
