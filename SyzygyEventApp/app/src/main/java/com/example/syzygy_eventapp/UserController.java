package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
     * Creates a new {@link User}, and checks it's ID doesn't collide in the database.
     * The user will have the default fields.
     * @return Task that completes when the document is created or if it already exists
     */
    public Task<Void> createUser() {
        String userID = String.valueOf(UUID.randomUUID());
        DocumentReference doc = usersRef.document(userID);

        return doc.get().continueWithTask(task -> {
            DocumentSnapshot snap = task.getResult();
            if (snap.exists()) {
                // the UUID collided, let's try again with a new one
                return createUser();
            }

            // create user with defaults
            User user = new User(userID);

            // Initial write
            return doc.set(user);
        });
    }

    /**
     * Retrieves a user from the database
     * Results in a IllegalArgumentException if the userID isn't in the database
     * @param userID the userID to get a {@link User} for
     * @return the {@link User} found in the database
     */
    public Task<User> getUser(String userID) {
        DocumentReference doc = usersRef.document(userID);

        return doc.get().continueWithTask(task -> {
            // snap can't be null because none of it's implementations can return null
            DocumentSnapshot snap = task.getResult();

            if (!snap.exists()) {
                return Tasks.forException(
                        new IllegalArgumentException("User: " + userID + " not found.")
                );
            }

            User user = snap.toObject(User.class);

            return Tasks.forResult(user);
        });
    }

    /**
     * Observes userID and pushes the current {@link User} on each change. <u>This should never be used outside of {@link User}.</u>
     * @param userID Document ID to observe
     * @param onUpdate Callback for changed versions of the {@link User}
     * @param onDelete Callback for when the user is deleted
     * @return ListenerRegistration for stopping the observation
     */
    public ListenerRegistration observeUser(String userID, Consumer<User> onUpdate, Runnable onDelete) {
        DocumentReference doc = usersRef.document(userID);

        return doc.addSnapshotListener((snap, error) -> {
           if (snap != null) {
               if (snap.exists()) {
                   User user = snap.toObject(User.class);
                   onUpdate.accept(user);
               } else {
                   onDelete.run();
               }
           }

           if (error != null) {
               System.err.println("Error on user snapshot listener: " + error.toString());
           }
        });
    }

    /**
     * Partially update a user's fields. <u>This should never be used outside of {@link User}.</u>
     * @param userID Document ID
     * @param fields A map of field names and new values
     * @return Task that completes when the merge is written
     */
    public Task<Void> updateFields(String userID, HashMap<String, Object> fields) {
        if (fields.isEmpty()) {
            // nothing to update
            return Tasks.forResult(null);
        }

        DocumentReference doc = usersRef.document(userID);

        return doc.get().continueWithTask(task -> {
            // snap can't be null because none of it's implementations can return null
            DocumentSnapshot snap = task.getResult();

            if (!snap.exists()) {
                return Tasks.forException(
                        new IllegalStateException("User: " + userID + " not found.")
                );
            }

            return doc.set(fields, SetOptions.merge());
        });
    }

    /**
     * Permanently delete this user.
     * @param userID The user document ID
     * @return Task that completes when the document is deleted
     */
    public Task<Void> deleteUser(String userID) {
        return usersRef.document(userID).delete();
    }
}
