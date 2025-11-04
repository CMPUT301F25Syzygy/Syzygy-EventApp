package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Controller for reading/writing {@link User} data in Firestore DB.
 * <p>
 *     UserViews will call this class to create, update, and delete users and to get changes from the User model.
 *     Firestore has the true data, and snapshots are converted to {@link User} and delivered back to the UserView.
 * </p>
 */
public class UserController {

    // A single global instance shared by the whole program
    public static UserController singletonInstance = null;

    private final CollectionReference usersRef;

    private UserController() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        this.usersRef = db.collection("users");
    }

    /**
     * Gets a single global instance of the UserController
     * @return a UserController singleton
     */
    public static UserController getInstance() {
        if (singletonInstance == null)
            singletonInstance = new UserController();

        return singletonInstance;
    }

    /**
     * If users{userID} is missing, create it with defaults.
     * <ul>
     *   <li>roles = {ENTRANT}</li>
     *   <li>activeRole = ENTRANT</li>
     *   <li>photoHidden = false</li>
     *   <li>demoted = false</li>
     *   <li>name/email set if provided</li>
     * </ul>
     * @param userID Unique user ID
     * @param name Display name to set on creation
     * @param email Email to set on creation
     * @return Task that completes when the document is created or if it already exists
     */
    public Task<Void> createIfMissing(String userID, String name, String email) {
        DocumentReference doc = usersRef.document(userID);

        return doc.get().continueWithTask(task -> {
            DocumentSnapshot snap = task.getResult();
            if (snap != null && snap.exists()) {
                // it already exists, do nothing
                return Tasks.forResult(null);
            }

            // create user with defaults
            User user = new User();
            user.setUserID(userID);
            user.setName(name);
            user.setEmail(email);
            user.setRole(Role.ENTRANT);
            user.setPhotoHidden(false);

            // Initial write
            return doc.set(user);
        });
    }

    /**
     * Observes users{userID} and pushes the current {@link User} on each change.
     * @param userID Document ID to observe
     * @param onUser Callback for parsed {@link User} objects
     * @param onError Callback for Firestore errors during listening
     * @return ListenerRegistration for stopping the observation
     */
    public ListenerRegistration observeUser(String userID, Consumer<User> onUser, Consumer<Exception> onError) {
        DocumentReference doc = usersRef.document(userID);
        return doc.addSnapshotListener((snap, error) -> {
           if (error != null) {
               onError.accept(error);
               return;
           }

           // If doc doesn't exist yet, createMissing will handle it
           if (snap == null || !snap.exists()) {
               return;
           }

           User user = snap.toObject(User.class);

           if (user != null) {
               onUser.accept(user);
           }
        });
    }

    /**
     * Partially update profile fields. Pass null to leave a field unchanged.
     * @param userID Document ID
     * @param name New name or null to ignore
     * @param email New email or null to ignore
     * @param photoHidden New flag or null to ignore
     * @param photoURL New photo URL or null to ignore
     * @return Task that completes when the merge is written
     */
    public Task<Void> updateProfile(String userID, String name, String email, Boolean photoHidden, String photoURL) {
        Map<String, Object> updates = new HashMap<>();

        if (name != null) {
            updates.put("name", name);
        }
        if (email != null) {
            updates.put("email", email);
        }
        if (photoHidden != null) {
            updates.put("photoHidden", photoHidden);
        }
        if (photoURL != null) {
            updates.put("photoURL", photoURL);
        }
        if (updates.isEmpty()) {
            // nothing to update
            return Tasks.forResult(null);
        }

        // merge so only fields with new values are touched
        return usersRef.document(userID).set(updates, SetOptions.merge());
    }

    /**
     * Overwrite the user's {@code role} and updates the database.
     * @param userID The user document ID
     * @param newRole New role
     * @return Task that completes when roles are written
     */
    public Task<Void> setRole(String userID, Role newRole) {
        DocumentReference doc = usersRef.document(userID);

        return doc.get().continueWithTask(task -> {
            // snap can't be null because none of it's implementations can return null
            DocumentSnapshot snap = task.getResult();

            if (!snap.exists()) {
                return Tasks.forException(
                        new IllegalStateException("User: " + userID + " not found.")
                );
            }

            // can't be null since we already checked the document exists
            User user = snap.toObject(User.class);
            assert user != null;

            // Update roles on the model
            user.setRole(newRole);

            Map<String, Object> updates = new HashMap<>();
            updates.put("role", newRole);
            updates.put("demoted", user.isDemoted());

            return doc.set(updates, SetOptions.merge());
        });
    }

    /**
     * Permanently delete this user.
     * @param userID The user document ID
     * @return Task that completes when the document is deleted
     */
    public Task<Void> deleteProfile(String userID) {
        return usersRef.document(userID).delete();
    }
}
