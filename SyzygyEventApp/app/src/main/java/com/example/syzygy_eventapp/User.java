package com.example.syzygy_eventapp;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * User Model containing user profile data and active role.
 * Fields are automatically updated when Firebase DB changes.
 * Updates the Firebase DB when fields are set.
 * Once the user is deleted it is still valid to read, but it will throw IllegalStateException when written to.
 * Other classes like UserController and UserTest are responsible for initializing values upon creating a User.
 */
public class User {
    /**
     * Unique identifier ID for the user.
     */
    final private @NonNull String userID;
    /**
     * Users name.
     */
    private @NonNull String name;
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
    /**
     * Role of the user, including the functionality of all of it's subroles *implicitly*
     * For example, all organizers are also entrants, so we don't need to store that separately
     * All admins are also organizers and entrants.
     */
    private Role role = Role.ENTRANT;

    /**
     * No-argument constructor to allow deserialization from Firebase, not useful otherwise
     */
    public User() {
        this.userID = "";
        this.name = "";
    }

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
    public User(@NonNull String userID) {
        this.userID = userID;
        int randomNumber = ThreadLocalRandom.current().nextInt(1000, 10000);
        this.name = "Untitled_" + randomNumber;
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
            this.role = user.role;

            return Tasks.forResult(null);
        });
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

    public Task<Void> promote() {
        role = role.promote();

        return updateDB(new HashMap<>() {{
            put("role", role);
        }});
    }

    /**
     * Demotes the user in the model and the database
     * @return a task that will complete when the DB has been updated
     */

    public Task<Void> demote() {
        role = role.demote();
        demoted = true;

        return updateDB(new HashMap<>() {{
            put("role", role);
            put("demoted", demoted);
        }});
    }

    /**
     * Changes the user's role, and detects if they have been demoted
     *
     * @param role the role to change the user to
     * @return a task that will complete when the DB has been updated
     */
    public Task<Void> setRole(Role role) {
        if (this.role != null && this.role.hasHigherAuthority(role)) {
            this.demoted = true;
        }
        this.role = role;

        return updateDB(new HashMap<>() {{
            put("role", role);
            put("demoted", demoted);
        }});
    }

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

    private Task<Void> updateDB(HashMap<String, Object> fields) {
        return UserController.getInstance().updateFields(userID, fields);
    }
}