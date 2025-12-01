package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
     *
     * @param event        Event document ID
     * @param organizerID  Organizer user ID creating the invitations
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
     * Updates fields on an invite
     *
     * @param invitationID Invitation document ID
     * @param fields The fields in the database to update
     * @return Task that completes when updated
     * @throws IllegalStateException if not found
     */

    public Task<Void> updateInvite(String invitationID, HashMap<String, Object> fields) {
        return updateInvite(invitationID, fields, (snap) -> {
            return true;
        }).onSuccessTask((nothing) -> {
            return Tasks.forResult(null);
        });
    }

    /**
     * Updates fields on an invite, under some condition
     *
     * @param invitationID Invitation document ID
     * @param fields The fields in the database to update
     * @param condition The condition under which the update should be made, takes a DocumentSnapshot of the invite
     * @return Task that completes when updated, results to true if updated
     * @return Task that completes when updated, results to true if updated
     * @throws IllegalStateException if not found
     */

    public Task<Boolean> updateInvite(String invitationID, HashMap<String, Object> fields, Predicate<DocumentSnapshot> condition) {
        DocumentReference doc = invitationsRef.document(invitationID);

        return doc.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }

            DocumentSnapshot snap = task.getResult();
            if (!snap.exists()) {
                return Tasks.forException(new IllegalStateException("Invitation: " + invitationID + " not found."));
            }

            if (condition.test(snap)) {
                return doc.set(fields, SetOptions.merge()).onSuccessTask((nothing) -> {
                    return Tasks.forResult(true);
                });
            } else {
                return Tasks.forResult(false);
            }
        });
    }

    /**
     * Mark an invitation as accepted by its recipient, as long as it hasn't been cancelled or decliend.
     * Sets accepted = true and responseTime = serverTimestamp().
     *
     * @param invitationID Invitation document ID (required, non-empty)
     * @return Task that completes when updated, results to true if updated
     * @throws IllegalStateException if not found
     */
    public Task<Boolean> acceptInvite(String invitationID) {
        return updateInvite(invitationID, new HashMap<>(){{
            put("accepted", true);
            put("responseTime", FieldValue.serverTimestamp());
        }}, (snap) -> {
            boolean isDecliend = snap.get("responseTime") != null && !snap.getBoolean("accepted");
            return !snap.getBoolean("cancelled") && !isDecliend;
        });
    }

    /**
     * Mark an invitation as declined by its recipient, as long as it hasn't been cancelled.
     * Sets accepted = false and responseTime = serverTimestamp().
     *
     * @param invitationID Invitation document ID (required, non-empty)
     * @return Task that completes when updated, results to true if updated
     * @throws IllegalStateException if not found
     */
    public Task<Boolean> declineInvite(String invitationID) {
        return updateInvite(invitationID, new HashMap<>(){{
            put("accepted", false);
            put("responseTime", FieldValue.serverTimestamp());
        }}, (snap) -> {
            return !snap.getBoolean("cancelled");
        });
    }

    /**
     * Mark an invitation as cancelled, as long as it hasn't been responded to.
     * Sets cancelled = true and cancelTime = serverTimestamp().
     *
     * @param invitationID Invitation document ID (required, non-empty)
     * @return Task that completes when updated, results to true if updated
     * @throws IllegalStateException if not found
     */
    public Task<Boolean> cancelInvite(String invitationID) {
        return updateInvite(invitationID, new HashMap<>(){{
            put("cancelled", true);
            put("cancelTime", FieldValue.serverTimestamp());
        }}, (snap) -> {
            return snap.get("responseTime") == null && !snap.getBoolean("cancelled");
        });
    }

    /**
     * Observe invitations to an event in real time.
     * Caller must hold the returned ListenerRegistration and remove it appropriately.
     *
     * @param eventId  eventID of the event to observe
     * @param onChange Callback invoked with the latest list of Invitation objects
     */
    public ListenerRegistration observeEventInvites(String eventId, Consumer<List<Invitation>> onChange) {
        return observeInvites(Filter.equalTo("event", eventId), onChange);
    }

    /**
     * Observe *all* invitations for a given event in real time.
     * Caller must hold the returned ListenerRegistration and remove it appropriately.
     *
     * @param onChange Callback invoked with the latest list of Invitation objects
     */
    public ListenerRegistration observeAllInvites(Consumer<List<Invitation>> onChange) {
        return observeInvites(null, onChange);
    }

    /**
     * Observe invitations for any filter in real time.
     * Caller must hold the returned ListenerRegistration and remove it appropriately.
     *
     * @param filter   Filter to select which invites to observe
     * @param onChange Callback invoked with the latest list of Invitation objects
     */
    public ListenerRegistration observeInvites(Filter filter, Consumer<List<Invitation>> onChange) {
        Query query = invitationsRef;
        if (filter != null) {
            query = query.where(filter);
        }

        return query.addSnapshotListener((snap, error) -> {
            if (error != null) {
                System.err.println(error);
                return;
            }

            List<Invitation> matchingInvites = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Invitation invite = doc.toObject(Invitation.class);
                    if (invite != null) {
                        invite.setInvitation(doc.getId());
                        matchingInvites.add(invite);
                    }
                }
            }

            onChange.accept(matchingInvites);
        });
    }

    /**
     * Retrieves a invite from the database
     * Results in a IllegalArgumentException if the userID isn't in the database
     *
     * @param inviteID the inviteID to get a {@link Invitation} for
     * @return the {@link User} found in the database
     */
    public Task<Invitation> getInvite(String inviteID) {
        DocumentReference doc = invitationsRef.document(inviteID);

        return doc.get().continueWithTask(task -> {
            // snap can't be null because none of it's implementations can return null
            DocumentSnapshot snap = task.getResult();

            if (!snap.exists()) {
                return Tasks.forException(
                        new IllegalArgumentException("Invite: " + inviteID + " not found.")
                );
            }

            Invitation invite = snap.toObject(Invitation.class);

            return Tasks.forResult(invite);
        });
    }

    /**
     * Get invitations to an event.
     *
     * @param eventId eventID of the event to get
     */
    public Task<List<Invitation>> getEventInvites(String eventId) {
        return getInvites(Filter.equalTo("event", eventId));
    }

    /**
     * Get *all* invitations for a given event.
     */
    public Task<List<Invitation>> getAllInvites() {
        return getInvites(null);
    }

    /**
     * Get invitations for any filter in real time.
     *
     * @param filter Filter to select which invites to observe
     */
    public Task<List<Invitation>> getInvites(Filter filter) {
        Query query = invitationsRef;
        if (filter != null) {
            query = query.where(filter);
        }

        return query.get().continueWithTask((task) -> {
            QuerySnapshot snap = task.getResult();

            List<Invitation> matchingInvites = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Invitation invite = doc.toObject(Invitation.class);
                    if (invite != null) {
                        invite.setInvitation(doc.getId());
                        matchingInvites.add(invite);
                    }
                }
            }

            return Tasks.forResult(matchingInvites);
        });
    }

    public Task<Void> deleteInvite(String invitationId) {
        if (invitationId == null || invitationId.isEmpty()) {
            return Tasks.forException(
                    new IllegalArgumentException("invitationId is required"));
        }

        return invitationsRef.document(invitationId).delete();
    }

    /**
     * Observe the invitation (if any) for a specific event/recipient in real time.
     *
     * @param eventId     Event document ID
     * @param recipientId Recipient user ID
     * @param onChange    Callback with the latest Invitation, or null if none
     * @return ListenerRegistration that must be removed when no longer needed
     */
    public ListenerRegistration observeUserEventInvite(
            String eventId,
            String recipientId,
            java.util.function.Consumer<Invitation> onChange
    ) {
        // 🔹 Only look at non-cancelled invites for this event + user
        Filter filter = Filter.and(
                Filter.equalTo("event", eventId),
                Filter.equalTo("recipientID", recipientId),
                Filter.equalTo("cancelled", false)
        );

        return observeInvites(filter, invites -> {
            if (invites == null || invites.isEmpty()) {
                onChange.accept(null);
                return;
            }

            Invitation bestPending = null;
            Invitation newest = null;

            for (Invitation inv : invites) {
                if (inv == null) {
                    continue;
                }

                com.google.firebase.Timestamp sendTime = inv.getSendTime();
                com.google.firebase.Timestamp responseTime = inv.getResponseTime();
                boolean hasResponded = (responseTime != null);

                // Track newest non-cancelled invite overall
                if (newest == null || isAfter(sendTime, newest.getSendTime())) {
                    newest = inv;
                }

                // Pending = non-cancelled + no response
                if (!hasResponded) {
                    if (bestPending == null || isAfter(sendTime, bestPending.getSendTime())) {
                        bestPending = inv;
                    }
                }
            }

            // Prefer the newest pending invite; otherwise fallback to newest non-cancelled invite
            Invitation chosen = (bestPending != null) ? bestPending : newest;
            onChange.accept(chosen);
        });
    }

    private boolean isAfter(com.google.firebase.Timestamp a, com.google.firebase.Timestamp b) {
        if (a == null) {
            return false;
        }
        if (b == null) {
            return true;
        }
        return a.compareTo(b) > 0;
    }

}
