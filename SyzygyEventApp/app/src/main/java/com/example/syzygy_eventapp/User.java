package com.example.syzygy_eventapp;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * User Model containing user profile data and active role.
 * Fields are automatically updated when Firebase DB changes.
 * Updates the Firebase DB when fields are set.
 * Once the user is deleted it is still valid to read, but it will throw IllegalStateException when written to.
 * Other classes like UserController and UserTest are responsible for initializing values upon creating a User.
 */
public class User {
    private Role role;
    /**
     * Unique identifier ID for the user.
     */
    private String userID;
    /**
     * Users name.
     */
    private @NonNull String name = "Untitled_" + ThreadLocalRandom.current().nextInt(1000, 10000);
    /**
     * Users contact info.
     */
    private String email;
    /**
     * Users contact info.
     */
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

    private boolean systemNotifications = true;

    private boolean organizerNotifications = true;

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
    public User() {
        this.setRole(Role.ENTRANT);
    }

    public User(String userID, @NonNull String name, String email, String phone, String photoURL, boolean photoHidden, boolean demoted, Role role) {
        this.userID = userID;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.photoURL = photoURL;
        this.photoHidden = photoHidden;
        this.demoted = demoted;
        this.role = role;
    }

    /**
     * Refreshes the user data from the database
     * @return a task that will complete after the refresh is done
     */
    public Task<Void> refresh() {
        return UserController.getInstance().getUser(userID).onSuccessTask((user) -> {
            this.name = user.name;
            this.email = user.email;
            this.phone = user.phone;
            this.photoURL = user.photoURL;
            this.photoHidden = user.photoHidden;
            this.demoted = user.demoted;

            return Tasks.forResult(null);
        });
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    @NonNull
    public String getUserID() {
        return userID;
    }

    public @NonNull String getName() {
        return name;
    }

    /**
     * Sets the user's name in the model and the database
     * @return a task that will complete when the DB has been updated
     */

    public Task<Void> setName(String name) {
        this.name = name;

        return updateDB(new HashMap<>() {{
            put("name", name);
        }});
    }

    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email in the model and the database
     * @return a task that will complete when the DB has been updated
     */

    public Task<Void> setEmail(String email) {
        this.email = email;

        return updateDB(new HashMap<>() {{
            put("email", email);
        }});
    }

    public String getPhotoURL() {
        return photoURL;
    }

    /**
     * Sets the user's profile picture in the model and the database
     * @return a task that will complete when the DB has been updated
     */

    public Task<Void> setPhotoURL(String photoURL) {
        this.photoURL = photoURL;

        return updateDB(new HashMap<>() {{
            put("photoURL", photoURL);
        }});
    }

    public boolean isPhotoHidden() {
        return photoHidden;
    }

    /**
     * Sets if the user's profile picture is hidden in the model and the database
     * @return a task that will complete when the DB has been updated
     */

    public Task<Void> setPhotoHidden(boolean photoHidden) {
        this.photoHidden = photoHidden;

        return updateDB(new HashMap<>() {{
            put("photoHidden", photoHidden);
        }});
    }

    public boolean isDemoted() {
        return demoted;
    }

    public void setRole(Role role) {
        this.role = role;
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

    /**
     * Promotes the user in the model and the database
     * @return a task that will complete when the DB has been updated
     */

    public String getPhone() {
        return phone;
    }

    /**
     * Sets the user's phone number in the model and the database
     * @return a task that will complete when the DB has been updated
     */

    public Task<Void> setPhone(String phone) {
        this.phone = phone;

        return updateDB(new HashMap<>() {{
            put("phone", phone);
        }});
    }

    public boolean isSystemNotifications() {
        return systemNotifications;
    }

    public Task<Void> setSystemNotifications(boolean systemNotifications) {
        this.systemNotifications = systemNotifications;
        return updateDB(new HashMap<>() {{
            put("systemNotifications", systemNotifications);
        }});
    }

    public boolean isOrganizerNotifications() {
        return organizerNotifications;
    }

    public Task<Void> setOrganizerNotifications(boolean organizerNotifications) {
        this.organizerNotifications = organizerNotifications;
        return updateDB(new HashMap<>() {{
            put("organizerNotifications", organizerNotifications);
        }});
    }


    public User demote() {
        return this;
    }

    public Organizer promote() {
        return new Organizer(userID, name, email, phone, photoURL, photoHidden, demoted, new ArrayList<>(), Role.ORGANIZER);
    }

    protected Task<Void> updateDB(HashMap<String, Object> fields) {
        if (userID == null) {
            return Tasks.forResult(null);
        }
        return UserController.getInstance().updateFields(userID, fields);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof User)) return false;
        User other = (User) obj;
        return userID != null && userID.equals(other.userID);
    }

    @Override
    public int hashCode() {
        return userID != null ? userID.hashCode() : 0;
    }

}