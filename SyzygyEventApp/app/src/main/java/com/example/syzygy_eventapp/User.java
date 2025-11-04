package com.example.syzygy_eventapp;

import static com.example.syzygy_eventapp.Role.ENTRANT;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * User Model containing user profile data and active role. Should match and be synced with Firebase DB.
 * Other classes like UserController and UserTest are responsible for initializing values upon creating a User.
 */
public class User {

    /**
     * Unique identifier ID for the user.
     */
    final private String userID;
    /**
     * Users name and contact info.
     */
    private String name;
    private String email;
    private String phone;
    /**
     * Users profile picture URL.
     */
    private String photoURL;
    /**
     * True if the profile picture is disabled or hidden.
     */
    private boolean photoHidden = false;
    /**
     * True if the user has ever been demoted.
     */
    private boolean demoted = false;
    /**
     * Role of the user, including the functionality of all of it's subroles implicitly
     * For example, all organizers are also entrants, so we don't need to store that separately
     * All admins are also organizers and entrants.
     */
    private Role role = ENTRANT;

    /**
     * Default Constructor used for creating users normally.
     * Starts with these default fields.
     * <ul>
     *   <li>role = ENTRANT</li>
     *   <li>photoHidden = false</li>
     *   <li>demoted = false</li>
     *   <li>name = "Untitled_0000" (random number ever time)</li>
     *   <li>all other fields left blank (null)</li>
     * </ul>
     */
    public User(String userID) {
        this.userID = userID;
        int randomNumber = ThreadLocalRandom.current().nextInt(1000, 10000);
        this.name = "Untitled_" + randomNumber;
    }

    /**
     * Constructor with all values, used mostly for testing.
     */
    public User(String userID, String name, String email, String phone, String photoURL, boolean photoHidden, boolean demoted, Role role) {
        this.userID = userID;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.photoURL = photoURL;
        this.photoHidden = photoHidden;
        this.demoted = demoted;
        this.role = role;
    }

    public String getUserID() {
        return userID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoURL() {
        return photoURL;
    }

    public void setPhotoURL(String photoURL) {
        this.photoURL = photoURL;
    }

    public boolean isPhotoHidden() {
        return photoHidden;
    }

    public void setPhotoHidden(boolean photoHidden) {
        this.photoHidden = photoHidden;
    }

    public boolean isDemoted() {
        return demoted;
    }

    public Role getRole() {
        return role;
    }

    /**
     * Tests if the user has the abilities of another role
     *
     * @param role the role to test
     * @return if the user has the abilities of the role
     */
    public boolean hasAbilitiesOfRole(Role role) {
        return !role.hasHigherAuthority(this.role);
    }

    public void promote() {
        this.role = role.promote();
    }

    public void demote() {
        this.role = role.demote();
        this.demoted = true;
    }

    /**
     * Changes the user's role, and detects if they have been demoted
     *
     * @param role the role to change the user to
     */
    public void setRole(Role role) {
        if (this.role != null && this.role.hasHigherAuthority(role)) {
            this.demoted = true;
        }
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
