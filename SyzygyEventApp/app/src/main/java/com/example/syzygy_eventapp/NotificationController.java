package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;


/**
 * Controller responsible for managing notifications stored in Firebase Firestore.
 */
public class NotificationController {
    // A single global instance shared by the whole program
    private static NotificationController singletonInstance = null;

    private final CollectionReference notifsRef;
    private final CollectionReference userNotifsRef;

    /**
     * Gets a single global instance of the NotificationController
     *
     * @return a NotificationController singleton
     */
    public static NotificationController getInstance() {
        if (singletonInstance == null)
            singletonInstance = new NotificationController();

        return singletonInstance;
    }

    private NotificationController() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        this.notifsRef = db.collection("notifications");
        this.userNotifsRef = db.collection("userNotifications");
    }

    /**
     * Observe notifications for a user in real time.
     * Caller must hold the returned ListenerRegistration and remove it appropriately.
     *
     * @param userId   The id of the user who's notifications we are observering
     * @param onChange Callback invoked with the latest list of Notification objects
     */
    public ListenerRegistration observeUserNotifications(String userId, Consumer<List<Notification>> onChange) {
        Set<Long> notifIds = new HashSet<>();

        // handle when the set of notifications for the user changes somehow
        Consumer<List<Notification>> handleChangedNotifs = (notifs) -> {
            List<Notification> matchingNotifs = new ArrayList<>();

            for (Notification notif : notifs) {
                if (notifIds.contains(notif.getId())) {
                    matchingNotifs.add(notif);
                }
            }

            onChange.accept(matchingNotifs);
        };

        // listen for changes in notifications assigned to user
        Filter userNotifFilter = Filter.equalTo("userId", userId);
        ListenerRegistration notifListener = observeUserNotifications(userNotifFilter,
                (userNotifs) -> {
                    notifIds.clear();

                    for (UserNotification userNotif : userNotifs) {
                        notifIds.add(userNotif.getNotificationId());
                    }

                    getAllNotifications().addOnSuccessListener(handleChangedNotifs::accept);
                });

        ListenerRegistration userNotifListener = observeNotifications(null, handleChangedNotifs);

        // this counts as a ListenerRegistration
        return () -> {
            notifListener.remove();
            userNotifListener.remove();
        };
    }

    /**
     * Observe notifications for any filter in real time.
     * Caller must hold the returned ListenerRegistration and remove it appropriately.
     *
     * @param filter   Filter to select which invites to observe
     * @param onChange Callback invoked with the latest list of Notification objects
     */
    public ListenerRegistration observeNotifications(Filter filter, Consumer<List<Notification>> onChange) {
        Query query = notifsRef;
        if (filter != null) {
            query = query.where(filter);
        }

        return query.addSnapshotListener((snap, error) -> {
            if (error != null) {
                System.err.println(error);
                return;
            }

            List<Notification> matchingNotifs = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Notification notif = doc.toObject(Notification.class);
                    if (notif != null) {
                        matchingNotifs.add(notif);
                    }
                }
            }

            onChange.accept(matchingNotifs);
        });
    }

    /**
     * Observe UserNotifications for any filter in real time.
     * Caller must hold the returned ListenerRegistration and remove it appropriately.
     *
     * @param filter   Filter to select which invites to observe
     * @param onChange Callback invoked with the latest list of UserNotifications objects
     */
    public ListenerRegistration observeUserNotifications(Filter filter, Consumer<List<UserNotification>> onChange) {
        Query query = userNotifsRef;
        if (filter != null) {
            query = query.where(filter);
        }

        return query.addSnapshotListener((snap, error) -> {
            if (error != null) {
                System.err.println(error);
                return;
            }

            List<UserNotification> matchingUserNotifs = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    UserNotification userNotif = doc.toObject(UserNotification.class);
                    if (userNotif != null) {
                        matchingUserNotifs.add(userNotif);
                    }
                }
            }

            onChange.accept(matchingUserNotifs);
        });
    }

    /**
     * Marks the notification as deleted so clients can remove it from the system tray.
     *
     * @param notificationId the ID of the notification to delete
     * @return a Task representing success or failure
     */
    public Task<Void> deleteNotification(long notificationId) {
        DocumentReference notifRef = notifsRef.document(Long.toString(notificationId));

        return notifRef.set(new HashMap<>() {{
            put("deleted", true);
        }}, SetOptions.merge());
    }


    /**
     * Adds an notification to Firestore under the "notifications" collection.
     *
     * @param notification the notification to upload
     * @return a Firestore Task showing completion status
     */
    public Task<Void> postNotification(Notification notification, List<String> recipientIds) {
        DocumentReference notifRef = notifsRef.document(Long.toString(notification.getId()));

        List<Task<Void>> tasks = new ArrayList<>();

        // create UserNotifications for each target user
        for (String recipientId : recipientIds) {
            UserNotification userNotif = new UserNotification(recipientId, notification.getId());

            DocumentReference userNotifRef = userNotifsRef.document();
            tasks.add(userNotifRef.set(userNotif));
        }

        // create Notification after every UserNotifications so it is valid
        return Tasks.whenAll(tasks).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(Objects.requireNonNull(task.getException()));
            }

            return notifRef.set(notification);
        });
    }

    public Task<List<Notification>> getAllNotifications() {
        return notifsRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }

            QuerySnapshot snap = task.getResult();

            List<Notification> notifs = new ArrayList<>();
            for(DocumentSnapshot docSnap : snap.getDocuments()) {
                notifs.add(docSnap.toObject(Notification.class));
            }

            return Tasks.forResult(notifs);
        });
    }
}
