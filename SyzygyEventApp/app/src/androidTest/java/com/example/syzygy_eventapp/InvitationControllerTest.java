package com.example.syzygy_eventapp;

import static org.junit.Assert.*;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.checkerframework.checker.units.qual.A;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for {@link InvitationController}.
 * Tests run against Firestore to verify database interactions.
 */
public class InvitationControllerTest {

    private static final int TIMEOUT_SEC = 10;

    private static FirebaseFirestore db;
    private static InvitationController controller;

    // Unique values per run to avoid collisions across CI jobs
    private static String event;
    private static String organizerID;
    private static String recipientA;
    private static String recipientB;

    static private final ArrayList<String> createdInviteIds = new ArrayList<>();

    @BeforeClass
    public static void setup() throws Exception {
        try {
            FirebaseApp.initializeApp(
                    InstrumentationRegistry.getInstrumentation().getTargetContext());
        } catch (IllegalStateException ignore) {
        }

        db = FirebaseFirestore.getInstance();
        controller = new InvitationController();

        String run = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        event = "event001" + run;
        organizerID = "user001" + run;
        recipientA = "user002" + run;
        recipientB = "user003" + run;
    }

    /**
     * Delete all the invitations created during this test.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        Tasks.await(
                BatchDeleter.deleteCollectionIds("invitations", createdInviteIds),
                30, TimeUnit.SECONDS);
    }

    private DocumentSnapshot getInvite(String invitation) throws Exception {
        return Tasks.await(
                db.collection("invitations").document(invitation).get(),
                TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    /**
     * Verify createInvites creates a single invitation document with expected defaults.
     */
    @Test
    public void testCreateInvite() throws Exception {
        List<String> invitations = Tasks.await(
                controller.createInvites(event, organizerID, Collections.singletonList(recipientA)),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        createdInviteIds.addAll(invitations);

        assertEquals(1, invitations.size());
        DocumentSnapshot snap = getInvite(invitations.get(0));
        assertTrue(snap.exists());

        assertEquals(event, snap.getString("event"));
        assertEquals(organizerID, snap.getString("organizerID"));
        assertEquals(recipientA, snap.getString("recipientID"));
        assertNull("accepted should start pending (null)", snap.get("accepted"));
        assertEquals(Boolean.FALSE, snap.getBoolean("cancelled"));
        // sendTime is serverTimestamp() and may not be set immediately on offline merges, but should exist online
    }

    /**
     * Verify createInvites handles multiple recipients in one call.
     */
    @Test
    public void testCreateManyInvites() throws Exception {
        List<String> invitations = Tasks.await(
                controller.createInvites(event, organizerID, Arrays.asList(recipientA, recipientB)),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        createdInviteIds.addAll(invitations);

        assertEquals(2, invitations.size());

        DocumentSnapshot a = getInvite(invitations.get(0));
        DocumentSnapshot b = getInvite(invitations.get(1));
        assertTrue(a.exists());
        assertTrue(b.exists());

        // Recipients could be in either order, so check both docs collectively
        var recipients = Arrays.asList(
                a.getString("recipientID"),
                b.getString("recipientID"));
        assertTrue(recipients.contains(recipientA));
        assertTrue(recipients.contains(recipientB));
    }

    /**
     * Verify accept marks accepted = true once and blocks a second response.
     */
    @Test
    public void testAccept() throws Exception {
        List<String> ids = Tasks.await(
                controller.createInvites(event, organizerID, Collections.singletonList(recipientA)),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        createdInviteIds.addAll(ids);
        String inviteId = ids.get(0);

        // First accept succeeds
        boolean wasAccepted = Tasks.await(controller.acceptInvite(inviteId),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        assertTrue(wasAccepted);

        DocumentSnapshot after = getInvite(inviteId);
        assertTrue(after.getBoolean("accepted"));
        assertNotNull(after.get("responseTime"));

        // overwrite invite as cancelled
        Tasks.await(controller.updateInvite(inviteId, new HashMap<>() {{
            put("accepted", false);
            put("responseTime", null);
            put("cancelled", true);
        }}), TIMEOUT_SEC, TimeUnit.SECONDS);

        // Second response (reject) should fail
        boolean wasAcceptedWhenCancelled = Tasks.await(controller.acceptInvite(inviteId),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        assertFalse(wasAcceptedWhenCancelled);

        // overwrite invite as declined
        Tasks.await(controller.updateInvite(inviteId, new HashMap<>() {{
            put("accepted", false);
            put("responseTime", Timestamp.now());
            put("cancelled", false);
            put("cancelTime", null);
        }}), TIMEOUT_SEC, TimeUnit.SECONDS);

        // Second response (reject) should fail
        boolean wasAcceptedWhenDeclined = Tasks.await(controller.acceptInvite(inviteId),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        assertFalse(wasAcceptedWhenDeclined);
    }

    /**
     * Verify reject marks accepted = false and sets responseTime.
     */
    @Test
    public void testDecline() throws Exception {
        List<String> ids = Tasks.await(
                controller.createInvites(event, organizerID, Collections.singletonList(recipientA)),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        createdInviteIds.addAll(ids);
        String inviteId = ids.get(0);

        // First accept succeeds
        boolean wasDeclined = Tasks.await(controller.declineInvite(inviteId),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        assertTrue(wasDeclined);

        DocumentSnapshot after = getInvite(inviteId);
        assertFalse(after.getBoolean("accepted"));
        assertNotNull(after.get("responseTime"));

        // overwrite invite as cancelled
        Tasks.await(controller.updateInvite(inviteId, new HashMap<>() {{
            put("accepted", false);
            put("responseTime", null);
            put("cancelled", true);
        }}), TIMEOUT_SEC, TimeUnit.SECONDS);

        // Second response (reject) should fail
        boolean wasDeclinedWhenCancelled = Tasks.await(controller.declineInvite(inviteId),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        assertFalse(wasDeclinedWhenCancelled);
    }

    /**
     * Verify cancel can only be performed by the organizer and blocks future responses.
     */
    @Test
    public void testCancel() throws Exception {
        List<String> ids = Tasks.await(
                controller.createInvites(event, organizerID, Collections.singletonList(recipientA)),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        createdInviteIds.addAll(ids);
        String inviteId = ids.get(0);

        // Organizer cancels
        boolean wasCancelled = Tasks.await(controller.cancelInvite(inviteId),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        assertTrue(wasCancelled);

        DocumentSnapshot afterCancel = getInvite(inviteId);
        assertEquals(Boolean.TRUE, afterCancel.getBoolean("cancelled"));
        assertNotNull(afterCancel.get("cancelTime"));

        Tasks.await(controller.updateInvite(inviteId, new HashMap<>() {{
            put("accepted", true);
            put("responseTime", Timestamp.now());
            put("cancelled", false);
            put("cancelTime", null);
        }}), TIMEOUT_SEC, TimeUnit.SECONDS);

        // Any later accept should fail
        boolean wasCancelledWhenAccepted = Tasks.await(controller.cancelInvite(inviteId),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        assertFalse(wasCancelledWhenAccepted);
    }
}
