package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Controller for reading/writing {@link User} data in Firestore DB.
 * <p>
 *     UserViews will call this class to create, update, and delete users and to get changes from the User model.
 *     Firestore has the true data, and snapshots are converted to {@link User} and delivered back to the UserView.
 * </p>
 */

public class UserController implements UserControllerInterface {
    // A single global instance shared by the whole program
    private static UserControllerInterface singletonInstance = null;

    private final CollectionReference usersRef;

    private UserController() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        this.usersRef = db.collection("users");
    }

    /**
     * Gets a single global instance of the UserController
     * @return a UserController singleton
     */
    public static UserControllerInterface getInstance() {
        if (singletonInstance == null)
            singletonInstance = new UserController();

        return singletonInstance;
    }

    /**
     * Override singleton with an external instance, likely for testing
     * @param instance the external instance
     */

    protected static void overrideInstance(UserControllerInterface instance) {
        singletonInstance = instance;
    }

    /**
     * Calls {@link #createUserFromClass(Supplier)} to create a {@link User} (entrant).
     * @return A new entrant with default fields.
     */
    public Task<User> createEntrant() {
        return createUserFromClass(User::new);
    }

    /**
     * Calls {@link #createUserFromClass(Supplier)} to create an {@link Organizer}.
     * @return A new organizer with default fields.
     */
    public Task<Organizer> createOrganizer() {
        return createUserFromClass(Organizer::new);
    }

    /**
     * Calls {@link #createUserFromClass(Supplier)} to create an {@link Admin}.
     * @return A new admin with default fields.
     */
    public Task<Admin> createAdmin() {
        return createUserFromClass(Admin::new);
    }

    /**
     * Creates a new {@link User}, or any subclass of it, and checks it's ID doesn't collide in the database.
     * The user will have the default fields.
     * @return Task that completes when the document is created or if it already exists
     */
    private <T extends User> Task<T> createUserFromClass(Supplier<T> constructor) {
        String userID = String.valueOf(UUID.randomUUID());
        DocumentReference doc = usersRef.document(userID);

        return doc.get().continueWithTask(task -> {
            DocumentSnapshot snap = task.getResult();
            if (snap.exists()) {
                // the UUID collided, let's try again with a new one
                return createUserFromClass(constructor);
            }

            // create a user
            T user = constructor.get();
            user.setUserID(userID);

            // Initial write
            return doc.set(user).continueWithTask((nothing) -> {
                return Tasks.forResult(user);
            });
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

            User user = buildUser(snap);

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
                   User user = buildUser(snap);
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
                        new IllegalArgumentException("User: " + userID + " not found.")
                );
            }

            return doc.set(fields, SetOptions.merge());
        });
    }

    public Task<User> setUserRole(String userID, Role role) {
        DocumentReference doc = usersRef.document(userID);

        return doc.get().continueWithTask(task -> {
            DocumentSnapshot snap = task.getResult();
            if (!snap.exists()) {
                return Tasks.forException(
                        new IllegalArgumentException("User: " + userID + " not found."));
            }

            User user = buildUser(snap);

            while (user.getRole() != role) {
                if (user.getRole().hasHigherAuthority(role)) {
                    user = user.demote();
                } else {
                    user = user.promote();
                }
            }

            // Write updated user
            User finalUser = user;
            return doc.set(user).onSuccessTask((nothing) -> {
                return Tasks.forResult(finalUser);
            });
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

    private User buildUser(DocumentSnapshot snap) {
        Role role = snap.get("role", Role.class);

        if (role == Role.ENTRANT) {
            return snap.toObject(User.class);
        } else if (role == Role.ORGANIZER) {
            return snap.toObject(Organizer.class);
        } else {
            assert role == Role.ADMIN;
            return snap.toObject(Admin.class);
        }
    }
}
