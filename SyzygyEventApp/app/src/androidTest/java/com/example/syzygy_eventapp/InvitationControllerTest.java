package com.example.syzygy_eventapp;

import static org.junit.Assert.*;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for {@link InvitationController}.
 * Tests run against Firestore to verify database interactions.
 */
public class InvitationControllerTest {

    private static final int TIMEOUT_SEC = 10;

    private FirebaseFirestore db;
    private InvitationControllerInterface controller;

    // Unique values per run to avoid collisions across CI jobs
    private String event;
    private String organizerID;
    private String recipientA;
    private String recipientB;

    @Before
    public void setUp() throws Exception {
        try {
            FirebaseApp.initializeApp(
                    InstrumentationRegistry.getInstrumentation().getTargetContext());
        } catch (IllegalStateException ignore) {}

        db = FirebaseFirestore.getInstance();
        controller = InvitationController.getInstance();

        String run = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        event = "event001" + run;
        organizerID = "user001" + run;
        recipientA  = "user002" + run;
        recipientB  = "user003" + run;

        cleanupInvites();
    }

    @After
    public void tearDown() throws Exception {
        cleanupInvites();
    }

    private void cleanupInvites() throws Exception {
        var byEvent = Tasks.await(
                db.collection("invitations").whereEqualTo("event", event).get(),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        for (var d : byEvent.getDocuments()) {
            Tasks.await(d.getReference().delete(), TIMEOUT_SEC, TimeUnit.SECONDS);
        }

        var byRecipients = Tasks.await(
                db.collection("invitations")
                        .whereIn("recipientID", Arrays.asList(recipientA, recipientB))
                        .get(),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        for (var d : byRecipients.getDocuments()) {
            Tasks.await(d.getReference().delete(), TIMEOUT_SEC, TimeUnit.SECONDS);
        }
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
                controller.createInvites(event, organizerID, Arrays.asList(recipientA)),
                TIMEOUT_SEC, TimeUnit.SECONDS);

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
                controller.createInvites(event, organizerID, Arrays.asList(recipientA)),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        String inviteId = ids.get(0);

        // First accept succeeds
        Tasks.await(controller.accept(inviteId, recipientA),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        DocumentSnapshot after = getInvite(inviteId);
        assertEquals(Boolean.TRUE, after.getBoolean("accepted"));
        assertNotNull(after.get("responseTime"));

        // Second response (reject) should fail
        Exception ex = assertThrows(Exception.class, () ->
                Tasks.await(controller.reject(inviteId, recipientA),
                        TIMEOUT_SEC, TimeUnit.SECONDS));
        assertNotNull(ex);
    }

    /**
     * Verify reject marks accepted = false and sets responseTime.
     */
    @Test
    public void testReject() throws Exception {
        List<String> ids = Tasks.await(
                controller.createInvites(event, organizerID, Arrays.asList(recipientA)),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        String inviteId = ids.get(0);

        Tasks.await(controller.reject(inviteId, recipientA),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        DocumentSnapshot after = getInvite(inviteId);
        assertEquals(Boolean.FALSE, after.getBoolean("accepted"));
        assertNotNull(after.get("responseTime"));
    }

    /**
     * Verify cancel can only be performed by the organizer and blocks future responses.
     */
    @Test
    public void testCancel() throws Exception {
        List<String> ids = Tasks.await(
                controller.createInvites(event, organizerID, Arrays.asList(recipientA)),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        String inviteId = ids.get(0);

        // Non-organizer should fail
        Exception ex1 = assertThrows(Exception.class, () ->
                Tasks.await(controller.cancel(inviteId, "someone_else"),
                        TIMEOUT_SEC, TimeUnit.SECONDS));
        assertNotNull(ex1);

        // Organizer cancels
        Tasks.await(controller.cancel(inviteId, organizerID),
                TIMEOUT_SEC, TimeUnit.SECONDS);
        DocumentSnapshot afterCancel = getInvite(inviteId);
        assertEquals(Boolean.TRUE, afterCancel.getBoolean("cancelled"));
        assertNotNull(afterCancel.get("cancelTime"));

        // Any later accept should fail
        Exception ex2 = assertThrows(Exception.class, () ->
                Tasks.await(controller.accept(inviteId, recipientA),
                        TIMEOUT_SEC, TimeUnit.SECONDS));
        assertNotNull(ex2);
    }
}
