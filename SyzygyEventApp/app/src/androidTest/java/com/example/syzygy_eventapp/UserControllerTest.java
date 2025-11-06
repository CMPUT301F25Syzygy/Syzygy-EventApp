package com.example.syzygy_eventapp;

import static org.junit.Assert.*;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for {@link UserController}.
 * These tests run against Firestore and verify database interactions.
 */
public class UserControllerTest {
    private static final int TIMEOUT_SEC = 10;

    private static UserControllerInterface controller;
    private static final ArrayList<String> createdUserIds = new ArrayList<>();

    @BeforeClass
    public static void setup() throws Exception {
        // Initialize Firebase app
        try {
            FirebaseApp.initializeApp(
                    InstrumentationRegistry.getInstrumentation().getTargetContext());
        } catch (IllegalStateException ignore) {
        }

        controller = UserController.getInstance();
    }

    /**
     * Delete all the users created during this test.
     */
    @AfterClass
    public static void teardown() throws Exception {
        Tasks.await(
                BatchDeleter.deleteCollectionIds("users", createdUserIds),
                30, TimeUnit.SECONDS);
    }

    /**
     * Verify createEntrant(), createOrganizer() and, createAdmin() creates a new user with default values
     */
    @Test
    public void testCreateUser() throws Exception {
        String userID1 = newUserId();
        User user = Tasks.await(controller.createEntrant(userID1), TIMEOUT_SEC, TimeUnit.SECONDS);

        assertNotNull(user);

        // Verify user data
        assertNotNull(user.getName());
        assertTrue(user.getName().length() >= 8);
        assertNull(user.getEmail());
        assertNull(user.getPhone());
        assertNull(user.getPhotoURL());
        assertFalse(user.isPhotoHidden());
        assertFalse(user.isDemoted());
        assertEquals(Role.ENTRANT, user.getRole());

        String userID2 = newUserId();
        Organizer organizer = Tasks.await(controller.createOrganizer(userID2), TIMEOUT_SEC, TimeUnit.SECONDS);

        String userID3 = newUserId();
        Admin admin = Tasks.await(controller.createAdmin(userID3), TIMEOUT_SEC, TimeUnit.SECONDS);

        assertEquals(Role.ORGANIZER, organizer.getRole());
        assertEquals(Role.ADMIN, admin.getRole());
    }

    /**
     * Make sure getUser() retrieves the same user from the database
     */
    @Test
    public void testGetUser() throws Exception {
        // create user and set fields
        String userID = newUserId();
        User user = Tasks.await(controller.createEntrant(userID), TIMEOUT_SEC, TimeUnit.SECONDS);

        // get user
        User retrievedUser = Tasks.await(controller.getUser(userID), TIMEOUT_SEC, TimeUnit.SECONDS);

        assertEquals(userID, retrievedUser.getUserID());
        assertEquals(user.getName(), retrievedUser.getName());
    }

    /**
     * Make sure getUser() throws when the userID doesn't exist
     */
    @Test
    public void testGetUserException() throws Exception {
        // get userID that does not exist
        String userID = newUserId();

        try {
            Tasks.await(controller.getUser(userID), TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (Exception ex) {
            Throwable cause = ex.getCause(); // Firebase wraps exceptions
            assertNotNull(cause);
            assertTrue(cause instanceof IllegalArgumentException);
        }
    }

    /**
     * Make sure updateFields() changes the data in the database
     */
    @Test
    public void testUpdateFields() throws Exception {
        // create user and set fields
        String userID = newUserId();
        User user = Tasks.await(controller.createEntrant(userID), TIMEOUT_SEC, TimeUnit.SECONDS);

        Task<Void> updateTask = controller.updateFields(userID, new HashMap<>() {{
            put("name", "Test Testington");
            put("email", "tester@gmail.com");
        }});

        Tasks.await(updateTask, TIMEOUT_SEC, TimeUnit.SECONDS);

        User retrievedUser = Tasks.await(controller.getUser(userID), TIMEOUT_SEC, TimeUnit.SECONDS);

        assertEquals(userID, retrievedUser.getUserID());
        assertEquals("Test Testington", retrievedUser.getName());
        assertEquals("tester@gmail.com", retrievedUser.getEmail());
    }

    /**
     * Make sure updateFields() throw an exception if the user does not exist
     */
    @Test
    public void testUpdateFieldsException() throws Exception {
        // get userID that does not exist
        String userID = newUserId();

        try {
            Task<Void> updateTask = controller.updateFields(userID, new HashMap<>() {{
                put("name", "Test Testington");
                put("email", "tester@gmail.com");
            }});

            Tasks.await(updateTask, TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (Exception ex) {
            Throwable cause = ex.getCause(); // Firebase wraps exceptions
            assertNotNull(cause);
            assertTrue(cause instanceof IllegalArgumentException);
        }
    }

    /**
     * Make sure that setters on user will update the database through updateFields()
     */
    @Test
    public void testUserSetters() throws Exception {
        // create user and set fields
        String userID = newUserId();
        User user = Tasks.await(controller.createEntrant(userID), TIMEOUT_SEC, TimeUnit.SECONDS);

        Task<Void> setNameTask = user.setName("Test Testington");
        Task<Void> setEmailTask = user.setEmail("tester@gmail.com");

        Tasks.await(Tasks.whenAllComplete(setNameTask, setEmailTask), TIMEOUT_SEC, TimeUnit.SECONDS);

        // retrieve user from DB
        User retrievedUser = Tasks.await(controller.getUser(userID), TIMEOUT_SEC, TimeUnit.SECONDS);

        assertEquals(userID, retrievedUser.getUserID());
        assertEquals("Test Testington", retrievedUser.getName());
        assertEquals("tester@gmail.com", retrievedUser.getEmail());
    }

    /**
     * Make sure setUserRole() changes the role of a user
     */
    @Test
    public void testSetUserRolePromote() throws Exception {
        // create user and set fields
        String userID = newUserId();
        User entrant = Tasks.await(controller.createEntrant(userID), TIMEOUT_SEC, TimeUnit.SECONDS);

        // set role
        Task<User> setAdminTask = controller.setUserRole(userID, Role.ADMIN);
        User admin = Tasks.await(setAdminTask, TIMEOUT_SEC, TimeUnit.SECONDS);

        assertEquals(entrant.getName(), admin.getName());
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals(Admin.class, admin.getClass());

        // get user
        User retrievedUser = Tasks.await(controller.getUser(userID), TIMEOUT_SEC, TimeUnit.SECONDS);

        assertEquals(entrant.getName(), retrievedUser.getName());
        assertEquals(Role.ADMIN, retrievedUser.getRole());
        assertEquals(Admin.class, retrievedUser.getClass());
    }

    /**
     * Make sure setUserRole() changes the role of a user
     */
    @Test
    public void testSetUserRoleDemote() throws Exception {
        // create user and set fields
        String userID = newUserId();
        User admin = Tasks.await(controller.createAdmin(userID), TIMEOUT_SEC, TimeUnit.SECONDS);

        // set role
        Task<User> setAdminTask = controller.setUserRole(userID, Role.ORGANIZER);
        User organizer = Tasks.await(setAdminTask, TIMEOUT_SEC, TimeUnit.SECONDS);

        assertEquals(admin.getName(), organizer.getName());
        assertEquals(Role.ORGANIZER, organizer.getRole());
        assertEquals(Organizer.class, organizer.getClass());

        // get user
        User retrievedUser = Tasks.await(controller.getUser(userID), TIMEOUT_SEC, TimeUnit.SECONDS);

        assertEquals(admin.getName(), retrievedUser.getName());
        assertEquals(Role.ORGANIZER, retrievedUser.getRole());
        assertEquals(Organizer.class, retrievedUser.getClass());
    }

    /**
     * Make sure that deleteUser() removes a user from the database
     */
    @Test
    public void testDeleteUser() throws Exception {
        // create user
        String userID = newUserId();
        User user = Tasks.await(controller.createEntrant(userID), TIMEOUT_SEC, TimeUnit.SECONDS);

        Task<Void> setNameTask = user.setName("Test Testington");
        Task<Void> setEmailTask = user.setEmail("tester@gmail.com");

        Tasks.await(Tasks.whenAllComplete(setNameTask, setEmailTask), TIMEOUT_SEC, TimeUnit.SECONDS);


        // delete user
        Tasks.await(controller.deleteUser(userID), TIMEOUT_SEC, TimeUnit.SECONDS);

        // try to get user
        try {
            Tasks.await(controller.getUser(userID),  TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (Exception ex) {
            Throwable cause = ex.getCause(); // Firebase wraps exceptions
            assertNotNull(cause);
            assertTrue(cause instanceof IllegalArgumentException);
        }
    }


    /**
     * Tests that the refresh pulls data with the UserController properly.
     */
    @Test
    public void testRefresh() throws Exception {
        // create user
        String userID = newUserId();
        User user = Tasks.await(controller.createEntrant(userID), TIMEOUT_SEC, TimeUnit.SECONDS);

        user.setName("Alice");
        user.setEmail("lost@wonderland.queen");
        user.setPhone("(980) 765-4321");

        // update database
        Task<Void> updateTask = controller.updateFields(user.getUserID(), new HashMap<>() {{
            put("name", "Test Testington");
            put("email", "tester@gmail.com");
        }});

        Tasks.await(updateTask, TIMEOUT_SEC, TimeUnit.SECONDS);

        // refresh user
        Tasks.await(user.refresh(), TIMEOUT_SEC, TimeUnit.SECONDS);;

        assertEquals("Test Testington", user.getName());
        assertEquals("tester@gmail.com", user.getEmail());
        assertEquals("(980) 765-4321", user.getPhone());
    }

    private String newUserId() {
        String id = String.valueOf(UUID.randomUUID());
        createdUserIds.add(id);
        return id;
    }
}
