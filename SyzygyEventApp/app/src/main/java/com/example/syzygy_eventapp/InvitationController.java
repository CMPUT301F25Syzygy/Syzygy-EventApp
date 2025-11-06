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

/**
 * Controller for reading/writing {@link Invitation} data in Firestore DB.
 * Firestore is the source of truth; views have real-time listeners and render {@link Invitation} models from snapshots. Writes (create/accept/reject/cancel) go through this controller.
 */
public class InvitationController {

    private final FirebaseFirestore db;
    private final CollectionReference invitationsRef;

    public InvitationController() {
        this.db = FirebaseFirestore.getInstance();
        this.invitationsRef = db.collection("invitations");
    }

    /**
     * Create one or many invitations for an event in a single Firestore batch write.
     * Each recipient gets its own document. Initial state: accepted == null (pending), sendTime = serverTimestamp(), cancelled = false, cancelTime = null.
     * @param event Event document ID
     * @param organizerID Organizer user ID creating the invitations
     * @param recipientIDs List of recipient user IDs
     * @return Task resolving to the list of created invitation document IDs
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Task<List<String>> createInvites(String event, String organizerID, List<String> recipientIDs) {
        if (event == null || event.isEmpty() || organizerID == null || organizerID.isEmpty() || recipientIDs == null || recipientIDs.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("event, organizerID, and recipientIDs are required"));
        }

        WriteBatch batch = db.batch();
        List<String> invitations = new ArrayList<>();

        for (String recipientID : recipientIDs) {
            if (recipientID == null || recipientID.isEmpty()) {
                return Tasks.forException(new IllegalArgumentException("recipientIDs must not contain null/empty values"));
            }

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
            data.put("cancelled", false);
            data.put("cancelTime", null);

            batch.set(doc, data);
        }

        return batch.commit().continueWith(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            return invitations;
        });
    }

    /**
     * Mark an invitation as accepted by its intended recipient.
     * Validates: invitation exists, not cancelled, caller is the recipient, and it is still pending.
     * Sets accepted = true and responseTime = serverTimestamp().
     * @param invitationID Invitation document ID (required, non-empty)
     * @param userID Acting user ID; must match recipientID (required, non-empty)
     * @return Task that completes when updated
     * @throws IllegalArgumentException on bad params
     * @throws SecurityException if user is not the recipient
     * @throws IllegalStateException if not found, cancelled, or already decided
     */

    public Task<Void> accept(String invitationID, String userID) {
        if (invitationID == null || invitationID.isEmpty() || userID == null || userID.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("invitationID and userID are required"));
        }

        DocumentReference doc = invitationsRef.document(invitationID);

        return doc.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(Objects.requireNonNull(task.getException()));
            }

            DocumentSnapshot snap = task.getResult();
            if (snap == null || !snap.exists()) {
                return Tasks.forException(new IllegalStateException("Invitation: " + invitationID + " not found."));
            }

            String recipient = snap.getString("recipientID");
            Boolean accepted = snap.getBoolean("accepted");
            Boolean cancelled = snap.getBoolean("cancelled");

            if (Boolean.TRUE.equals(cancelled)) {
                return Tasks.forException(new IllegalStateException("Invitation has been cancelled."));
            }

            if (recipient == null || !recipient.equals(userID)) {
                return Tasks.forException(new SecurityException("Only the recipient can accept this invitation."));
            }

            if (accepted != null) {
                return Tasks.forException(new IllegalStateException("Response already given."));
            }

            return doc.update("accepted", true, "responseTime", FieldValue.serverTimestamp());
        });
    }

    /**
     * Mark an invitation as rejected by its intended recipient.
     * Validates: invitation exists, not cancelled, caller is the recipient, and it is still pending.
     * Sets accepted = false and responseTime = serverTimestamp().
     * @param invitationID Invitation document ID (required, non-empty)
     * @param userID Acting user ID; must match recipientID (required, non-empty)
     * @return Task that completes when updated
     * @throws IllegalArgumentException on bad params
     * @throws SecurityException if user is not the recipient
     * @throws IllegalStateException if not found, cancelled, or already decided
     */
    public Task<Void> reject(String invitationID, String userID) {
        if (invitationID == null || invitationID.isEmpty() || userID == null || userID.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("invitationID and userID are required"));
        }

        DocumentReference doc = invitationsRef.document(invitationID);

        return doc.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(Objects.requireNonNull(task.getException()));
            }

            DocumentSnapshot snap = task.getResult();
            if (snap == null || !snap.exists()) {
                return Tasks.forException(new IllegalStateException("Invitation: " + invitationID + " not found."));
            }

            String recipient = snap.getString("recipientID");
            Boolean accepted = snap.getBoolean("accepted");
            Boolean cancelled = snap.getBoolean("cancelled");

            if (Boolean.TRUE.equals(cancelled)) {
                return Tasks.forException(new IllegalStateException("Invitation has been cancelled."));
            }

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
     * Set an invitation to cancelled so it can't be interacted with.
     * Only the original organizer may cancel.
     * Sets cancelled = true and cancelTime = serverTimestamp().
     * @param invitation The Firestore document ID of the invitation.
     * @param organizerID  The organizer performing the cancel action.
     * @return Task that completes when the cancel flag and time are updated.
     */
    public Task<Void> cancel(String invitation, String organizerID) {
        if (invitation == null || invitation.isEmpty() || organizerID == null || organizerID.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("invitationID and organizerID are required"));
        }

        DocumentReference doc = invitationsRef.document(invitation);

        return doc.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(Objects.requireNonNull(task.getException()));
            }

            DocumentSnapshot snap = task.getResult();
            if (snap == null || !snap.exists()) {
                return Tasks.forException(new IllegalStateException("Invitation: " + invitation + " not found."));
            }

            String organizerOnDoc = snap.getString("organizerID");
            if (organizerOnDoc == null || !organizerOnDoc.equals(organizerID)) {
                return Tasks.forException(new SecurityException("Only the organizer can cancel this invitation."));
            }

            return doc.update("cancelled", true, "cancelTime", FieldValue.serverTimestamp());
        });
    }

    /**
     * Observe all invitations for a given event in real time.
     * Caller must hold the returned ListenerRegistration and remove it appropriately.
     * @param event Event document ID to observe
     * @param onChange Callback invoked with the latest list of Invitation objects
     * @param onError Callback invoked on listener errors
     * @return ListenerRegistration that must be removed when no longer needed
     * @throws IllegalArgumentException if event is null/empty
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

    /**
     * Observe all pending (accepted == null and not cancelled) invitations for a recipient in real time.
     * @param recipientID User ID of the invitation recipient.
     * @param onChange    Callback invoked with the latest list of pending invitations.
     * @param onError     Callback invoked on Firestore listener errors.
     * @return ListenerRegistration that must be removed when no longer needed.
     */
    public ListenerRegistration observePending(String recipientID, Consumer<List<Invitation>> onChange, Consumer<Exception> onError) {
        if (recipientID == null || recipientID.isEmpty()) {
            throw new IllegalArgumentException("recipientID is required");
        }

        return invitationsRef.whereEqualTo("recipientID", recipientID).whereEqualTo("accepted", null).addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        onError.accept(e);
                        return;
                    }

                    List<Invitation> list = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Boolean canceled = doc.getBoolean("cancelled");
                            if (Boolean.TRUE.equals(canceled)) {
                                continue; // skip cancelled invites
                            }

                            Invitation inv = doc.toObject(Invitation.class);
                            if (inv != null && inv.getInvitation() == null) {
                                inv.setInvitation(doc.getId());
                            }
                            if (inv != null) {
                                list.add(inv);
                            }
                        }
                    }

                    onChange.accept(list);
                });
    }
}
