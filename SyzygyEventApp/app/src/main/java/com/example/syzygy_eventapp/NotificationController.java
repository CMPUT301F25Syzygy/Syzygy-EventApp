package com.example.syzygy_eventapp;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Controller responsible for managing notifications stored in Firebase Firestore.
 */
public class NotificationController {
    private final FirebaseFirestore db;

    public NotificationController(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Gets all notifications stored in the Firestore "notifications" collection.
     * @return a Firestore Task containing a list of Notification objects
     */
    public Task<List<Notification>> getAllNotifications() {
        return db.collection("notifications")
                .get()
                .continueWith(task -> {
                    List<Notification> result = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            Notification n = doc.toObject(Notification.class);
                            if (n != null) {
                                result.add(n);
                            }
                        }
                    }
                    return result;
                });
    }

    /**
     * Gets all notifications intended for a specific user from databse.
     *
     * @param userId the ID of the user
     * @return a Firestore Task containing a list of notifications for the user
     */
    public Task<List<Notification>> getNotificationsForUser(String userId) {
        return db.collection("userNotifications")
                .get()
                .continueWith(task -> {
                    List<Notification> result = new ArrayList<>();
                    //Kind of inefficient if collection large...replace in future maybe
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            Notification n = doc.toObject(Notification.class);
                            if (n != null) {
                                for (User u : n.getRecipients()) {
                                    if (u.getUserID().equals(userId)) {
                                        result.add(n);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    return result;
                });
    }

    /**
     * Deletes a notification from the "notifications" collection and removes
     * all entries in the "userNotifications" collection that reference it.
     *
     * @param notificationId the ID of the notification to delete
     * @return a Task representing success or failure
     */
    public Task<Void> deleteNotification(String notificationId) {

        DocumentReference notifRef =
                db.collection("notifications").document(notificationId);

        return notifRef.get().continueWithTask(task -> {

            if (!task.isSuccessful() || !task.getResult().exists()) {
                return Tasks.forException(new Exception("Notification not found"));
            }

            //Query all userNotifications entries for this notification
            Query query = db.collection("userNotifications")
                    .whereEqualTo("notificationID", notificationId);

            return query.get().continueWithTask(queryTask -> {

                if (!queryTask.isSuccessful()) {
                    return Tasks.forException(Objects.requireNonNull(queryTask.getException()));
                }

                List<Task<Void>> deleteLinks = new ArrayList<>();

                // Delete every userNotifications doc that matches
                for (DocumentSnapshot doc : queryTask.getResult()) {
                    deleteLinks.add(doc.getReference().delete());
                }
                // Delete the notification itself after all links removed
                return Tasks.whenAll(deleteLinks)
                        .continueWithTask(x -> notifRef.delete());
            });
        });
    }


    /**
     * Adds an organizer-created notification to Firestore under the
     * "notifications" collection and duplicates it.
     *
     * @param notification the notification to upload
     * @return a Firestore Task showing completion status
     */
    public Task<Void> postOrganizerNotification(@NonNull Notification notification) {
        String notifId = db.collection("notifications").document().getId();
        notification.setId(notifId);

        DocumentReference notifRef =
                db.collection("notifications").document(notifId);

        return notifRef.set(notification).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(Objects.requireNonNull(task.getException()));
            }

            List<Task<Void>> tasks = new ArrayList<>();

            for (User user : notification.getRecipients()) {

                //Make a copy of the notification with only this user
                Notification userNotif = new Notification(
                        notification.getTitle(),
                        notification.getDescription(),
                        notification.getEvent(),
                        notification.getOrganizer(),
                        List.of(user)
                );
                userNotif.setId(notifId);
                userNotif.setTimestamp(System.currentTimeMillis());

                DocumentReference userNotifRef = db.collection("userNotifications").document();
                tasks.add(userNotifRef.set(userNotif));
            }

            return Tasks.whenAll(tasks);
        });
    }

}
