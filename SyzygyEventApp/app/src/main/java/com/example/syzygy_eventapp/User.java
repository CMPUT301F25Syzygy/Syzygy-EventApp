package com.example.syzygy_eventapp;

import java.util.List;

/**
 * User Model containing user profile data and active role. Should match and be synced with Firebase DB.
 * Other classes like UserController and UserTest are responsible for initializing values upon creating a User.
 */
public class User {

    /** Unique identifier ID for the user. */
    private String userID;
    /** Users name and contact info. */
    private String name;
    private String email;
    private String phone;
    /** Users profile picture URL. */
    private String photoURL;
    /** True if the profile picture is disabled or hidden. */
    private boolean photoHidden;
    /** True if the user has ever been demoted. */
    private boolean demoted;
    /** List of assigned roles for the user. */
    private List<Role> roles;
    /**
     * The users active role determines their current nav bar, views, privileges, etc.
     * Should be present in the "roles" list.
     */
    private Role activeRole;

    /** Default Constructor used for creating users normally. */
    public User() {
    }

    /** Constructor with all values, used mostly for testing. */
    public User(String userID, String name, String email, String phone, String photoURL, boolean photoHidden, boolean demoted, List<Role> roles, Role activeRole) {
        this.userID = userID;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.photoURL = photoURL;
        this.photoHidden = photoHidden;
        this.demoted = demoted;
        this.roles = roles;
        this.activeRole = activeRole;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
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

    public void setDemoted(boolean demoted) {
        this.demoted = demoted;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public Role getActiveRole() {
        return activeRole;
    }

    public void setActiveRole(Role activeRole) {
        this.activeRole = activeRole;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Ensures the users active role is actually one of their assigned roles.
     * @return true if activeRole and roles are not null and roles has the active role in the list.
     */
    public boolean hasValidActiveRole() {
        return activeRole != null && roles != null && roles.contains(activeRole);
    }
}
