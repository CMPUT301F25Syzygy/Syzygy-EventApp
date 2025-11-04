package com.example.syzygy_eventapp;

import static org.junit.Assert.*;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for {@link UserController}.
 * These tests run against Firestore and verify database interactions.
 */
public class UserControllerTest {

    private static final int TIMEOUT_SEC = 10;

    private FirebaseFirestore db;
    private UserController controller;
    private String testUserId;
    private DocumentReference userDoc;

    @Before
    public void setUp() throws Exception {
        // Initialize Firebase app
        try {
            FirebaseApp.initializeApp(
                    InstrumentationRegistry.getInstrumentation().getTargetContext());
        } catch (IllegalStateException ignore) {}

        db = FirebaseFirestore.getInstance();
        controller = UserController.getInstance();

        // unique ID so test runs don't conflict
        testUserId = "uc_test_" + UUID.randomUUID();
        userDoc = db.collection("users").document(testUserId);

        // ensure a clean slate
        Tasks.await(userDoc.delete(), TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws Exception {
        // cleanup after each test
        Tasks.await(userDoc.delete(), TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    /**
     * Verify createIfMissing creates a new user with default values when the document doesn't exist.
     */
    @Test
    public void testCreateIfMissing() throws Exception {
        // Ensure it doesn’t exist before
        DocumentSnapshot before = Tasks.await(userDoc.get(), TIMEOUT_SEC, TimeUnit.SECONDS);
        assertFalse(before.exists());

        Tasks.await(controller.createIfMissing(testUserId, "Tester", "tester@example.com"),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        DocumentSnapshot after = Tasks.await(userDoc.get(), TIMEOUT_SEC, TimeUnit.SECONDS);
        assertTrue(after.exists());

        User user = after.toObject(User.class);
        assertNotNull(user);

        // Verify user data
        assertEquals(testUserId, user.getUserID());
        assertEquals("Tester", user.getName());
        assertEquals("tester@example.com", user.getEmail());
        assertFalse(user.isPhotoHidden());
        assertFalse(user.isDemoted());
        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().contains(Role.ENTRANT));
        assertEquals(Role.ENTRANT, user.getActiveRole());
    }

    /**
     * Verify that {@link UserController#updateProfile(String, String, String, Boolean, String)} correctly performs a partial merge update on existing user data.
     * <p>
     *     Only non-null parameters should be updated. Fields passed as {@code null}
     *     should remain unchanged in Firestore. This test ensures selective updates
     *     are applied without overwriting other data.
     * </p>
     * @throws Exception if Firestore operations fail or timeout
     */
    @Test
    public void testUpdateProfile() throws Exception {
        // Create a fresh user with default data
        Tasks.await(controller.createIfMissing(testUserId, "Tester", "tester@example.com"),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        // Update only 'name' and 'photoHidden'
        // Other fields remain null and should be ignored
        Tasks.await(controller.updateProfile(
                        testUserId,
                        "Tester Renamed",   // new name
                        null,               // email unchanged
                        true,               // photoHidden set true
                        null                // photoURL unchanged
                ),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        // Fetch updated document
        DocumentSnapshot snap = Tasks.await(userDoc.get(), TIMEOUT_SEC, TimeUnit.SECONDS);
        assertTrue(snap.exists());

        User user = snap.toObject(User.class);
        assertNotNull(user);

        // Confirm changed fields
        assertEquals("Tester Renamed", user.getName());
        assertTrue(user.isPhotoHidden());

        // Confirm unchanged fields
        assertEquals("tester@example.com", user.getEmail());
        assertFalse(user.isDemoted());
        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().contains(Role.ENTRANT));
        assertEquals(Role.ENTRANT, user.getActiveRole());
    }

    /**
     * Verify that {@link UserController#setActiveRole(String, Role)} correctly updates the user's active role only if it is already one of their assigned roles.
     * <p>
     *     This ensures that a user cannot switch to an invalid or unassigned role,
     *     and that Firestore properly reflects the new activeRole after update.
     * </p>
     * @throws Exception if Firestore operations fail or timeout
     */
    @Test
    public void testSetActiveRole() throws Exception {
        // Create user with default role ENTRANT
        Tasks.await(controller.createIfMissing(testUserId, "Tester", "tester@example.com"),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        // Add ORGANIZER role directly to simulate promotion
        Tasks.await(userDoc.update("roles", java.util.Arrays.asList(Role.ENTRANT.name(), Role.ORGANIZER.name())),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        // Set activeRole to ORGANIZER (a valid role for this user)
        Tasks.await(controller.setActiveRole(testUserId, Role.ORGANIZER),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        // Firestore should now store ORGANIZER as the active role
        DocumentSnapshot updatedSnap = Tasks.await(userDoc.get(), TIMEOUT_SEC, TimeUnit.SECONDS);
        User user = updatedSnap.toObject(User.class);
        assertNotNull(user);
        assertEquals(Role.ORGANIZER, user.getActiveRole());

        // Try setting an invalid role not assigned to the user (ADMIN)
        Exception ex = assertThrows(Exception.class, () -> {
            Tasks.await(controller.setActiveRole(testUserId, Role.ADMIN),
                    TIMEOUT_SEC, TimeUnit.SECONDS);
        });

        // Firestore should not change the active role
        DocumentSnapshot unchangedSnap = Tasks.await(userDoc.get(), TIMEOUT_SEC, TimeUnit.SECONDS);
        User unchangedUser = unchangedSnap.toObject(User.class);
        assertNotNull(unchangedUser);
        assertEquals(Role.ORGANIZER, unchangedUser.getActiveRole());
    }

    /**
     * Verify that setRoles overwrites the roles set and keeps {@code activeRole} valid:
     * <ul>
     *   <li>If current activeRole is removed, it falls back to ENTRANT if present, otherwise the first role in the new set.</li>
     *   <li>Rejects empty role sets.</li>
     * </ul>
     * @throws Exception if Firestore operations fail or timeout
     */
    @Test
    public void testSetRoles() throws Exception {
        // Create user with defaults (roles={ENTRANT}, activeRole=ENTRANT)
        Tasks.await(controller.createIfMissing(testUserId, "Tester", "tester@example.com"),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        // Remove ENTRANT and set roles to {ORGANIZER, ADMIN}
        // EnumSet gives deterministic iteration order based on enum declaration.
        Tasks.await(controller.setRoles(testUserId,
                        java.util.EnumSet.of(Role.ORGANIZER, Role.ADMIN)),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        // Roles overwritten, activeRole falls back to first available (ORGANIZER)
        DocumentSnapshot snap1 = Tasks.await(userDoc.get(), TIMEOUT_SEC, TimeUnit.SECONDS);
        User user1 = snap1.toObject(User.class);
        assertNotNull(user1);
        assertTrue(user1.getRoles().contains(Role.ORGANIZER));
        assertTrue(user1.getRoles().contains(Role.ADMIN));
        assertFalse(user1.getRoles().contains(Role.ENTRANT));
        assertEquals(Role.ORGANIZER, user1.getActiveRole()); // fallback chosen

        // Set roles to {ENTRANT, ADMIN} — activeRole ORGANIZER is now invalid, should fall back to ENTRANT
        Tasks.await(controller.setRoles(testUserId,
                        java.util.EnumSet.of(Role.ENTRANT, Role.ADMIN)),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        // Roles updated and activeRole set to ENTRANT
        DocumentSnapshot snap2 = Tasks.await(userDoc.get(), TIMEOUT_SEC, TimeUnit.SECONDS);
        User user2 = snap2.toObject(User.class);
        assertNotNull(user2);
        assertTrue(user2.getRoles().contains(Role.ENTRANT));
        assertTrue(user2.getRoles().contains(Role.ADMIN));
        assertFalse(user2.getRoles().contains(Role.ORGANIZER));
        assertEquals(Role.ENTRANT, user2.getActiveRole());

        // Attempt to set an empty role set, should throw
        Exception ex = assertThrows(Exception.class, () -> {
            Tasks.await(controller.setRoles(testUserId, java.util.Collections.emptySet()),
                    TIMEOUT_SEC, TimeUnit.SECONDS);
        });
        assertNotNull(ex);
    }

    /**
     * Verify that {@link UserController#deleteProfile(String)} removes the user document.
     * <p>
     *     Creates a user, deletes it, then asserts the document no longer exists.
     * </p>
     * @throws Exception if Firestore operations fail or timeout
     */
    @Test
    public void testDeleteProfile() throws Exception {
        // Create user so there is something to delete
        Tasks.await(controller.createIfMissing(testUserId, "Tester", "tester@example.com"),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        DocumentSnapshot before = Tasks.await(userDoc.get(), TIMEOUT_SEC, TimeUnit.SECONDS);
        assertTrue(before.exists());

        // Delete the document
        Tasks.await(controller.deleteProfile(testUserId),
                TIMEOUT_SEC, TimeUnit.SECONDS);

        // The document should no longer exist
        DocumentSnapshot after = Tasks.await(userDoc.get(), TIMEOUT_SEC, TimeUnit.SECONDS);
        assertFalse(after.exists());
    }
}
