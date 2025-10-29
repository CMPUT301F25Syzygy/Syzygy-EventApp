package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class InvitationController {

    private final FirebaseFirestore db;
    private final CollectionReference invitationsRef;

    public InvitationController() {
        this.db = FirebaseFirestore.getInstance();
        this.invitationsRef = db.collection("invitations");
    }

    /**
     * Possible to pass a Collections.singletonList of one user/recipient or Arrays.asList of many users/recipients.
     * @param event
     * @param organizerID
     * @param recipientIDs
     * @return
     */
    public Task<List<String>> createInvites(String event, String organizerID, List<String> recipientIDs) {
        if (event == null || event.isEmpty() || organizerID == null || organizerID.isEmpty() || recipientIDs == null || recipientIDs.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("event, organizerID, and recipientIDs are required"));
        }

        WriteBatch batch = db.batch();
        List<String> invitations = new ArrayList<>();

        for (String recipientID : recipientIDs) {
            DocumentReference doc = invitationsRef.document();
            String invitation = doc.getId();
            invitations.add(invitation);

            Map<String, Object> data = new HashMap<>();
            data.put("invitation", invitation);
            data.put("event", event);
            data.put("organizerID", organizerID);
            data.put("recipientID", recipientID);
            data.put("accepted", null);
            data.put("sendTime", FieldValue.serverTimestamp());
            data.put("responseTime", null);

            batch.set(doc, data);
        }

        return batch.commit().continueWith(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            return invitations;
        });
    }

    public Task<Void> accept(String invitationID, String userID) {
        if (invitationID == null || invitationID.isEmpty() || userID == null || userID.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("invitationID and userID are required"));
        }

        DocumentReference doc = invitationsRef.document(invitationID);

        return doc.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(Objects.requireNonNull(task.getException()));
            }

            var snap = task.getResult();
            if (snap == null || !snap.exists()) {
                return Tasks.forException(new IllegalStateException("Invitation: " + invitationID + " not found."));
            }

            String recipient = snap.getString("recipientID");
            Boolean accepted = snap.getBoolean("accepted");

            if (recipient == null || !recipient.equals(userID)) {
                return Tasks.forException(new SecurityException("Only the recipient can accept this invitation."));
            }

            if (accepted != null) {
                return Tasks.forException(new IllegalStateException("Response already given."));
            }

            return doc.update("accepted", true, "responseTime", FieldValue.serverTimestamp());
        });
    }

    public Task<Void> reject(String invitationID, String userID) {
        if (invitationID == null || invitationID.isEmpty() || userID == null || userID.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("invitationID and userID are required"));
        }

        DocumentReference doc = invitationsRef.document(invitationID);

        return doc.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(Objects.requireNonNull(task.getException()));
            }

            var snap = task.getResult();
            if (snap == null || !snap.exists()) {
                return Tasks.forException(new IllegalStateException("Invitation: " + invitationID + " not found."));
            }

            String recipient = snap.getString("recipientID");
            Boolean accepted = snap.getBoolean("accepted");

            if (recipient == null || !recipient.equals(userID)) {
                return Tasks.forException(new SecurityException("Only the recipient can reject this invitation."));
            }

            if (accepted != null) {
                return Tasks.forException(new IllegalStateException("Response already given."));
            }

            return doc.update("accepted", false, "responseTime", FieldValue.serverTimestamp());
        });
    }

    /**
     * Observe all invitations for a given event in real time.
     * <p>
     * Caller MUST hold the returned ListenerRegistration and call {@code remove()} in the Fragment's lifecycle
     * (e.g., in onStop()) to avoid leaks. The callback receives the full list on every change; your UI can
     * partition into pending/accepted/rejected and compute counts client-side.
     * </p>
     *
     * @param event  Event document ID to observe (required, non-empty)
     * @param onChange Callback invoked with the latest list of Invitation objects
     * @param onError  Callback invoked on listener errors
     * @return ListenerRegistration to remove() when no longer needed
     * @throws IllegalArgumentException if eventId is null/empty
     */
    public ListenerRegistration observeEventInvitations(String event, Consumer<List<Invitation>> onChange, Consumer<Exception> onError) {
        if (event == null || event.isEmpty()) {
            throw new IllegalArgumentException("event is required");
        }

        return invitationsRef.whereEqualTo("event", event).addSnapshotListener((snap, error) -> {
            if (error != null) {
                onError.accept(error);
                return;
            }

            List<Invitation> list = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Invitation inv = doc.toObject(Invitation.class);
                    if (inv != null) {
                        if (inv.getInvitation() == null) {
                            inv.setInvitation(doc.getId());
                        }
                        list.add(inv);
                    }
                }
            }

            onChange.accept(list);
        });
    }
}
