package com.example.syzygy_eventapp;

import static org.junit.Assert.*;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.ListenerRegistration;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Unit tests for the {@link User} model class.
 * <p>
 * These tests verify that the User class correctly handles field initialization, getter/setter methods, role validation, and flag updates.
 * </p>
 */
public class UserTest {
    public User mockUser() {
        User user = new User("Test123");

        user.setName("Tester Testington");
        user.setEmail("test@testmail.com");
        user.setPhone("(123) 456-7890");
        user.setPhotoURL("https://testphoto.com/test.jpg");
        user.setPhotoHidden(false);
        user.setRole(Role.ENTRANT);

        return user;
    }

    private class UserControllerMock implements UserControllerInterface {
        public Task<User> createUser() {
            return Tasks.forResult(mockUser());
        }

        public Task<User> getUser(String userID) {
            return Tasks.forResult(mockUser());
        }

        public ListenerRegistration observeUser(String userID, Consumer<User> onUpdate, Runnable onDelete) {
            return () -> {};
        }

        public Task<Void> updateFields(String userID, HashMap<String, Object> fields) {
            return Tasks.forResult(null);
        }

        public Task<Void> deleteUser(String userID) {
            return Tasks.forResult(null);
        }
    }

    @Before
    public void setup() {
        UserController.overrideInstance(new UserControllerMock());
    }

    /**
     * Tests that the full constructor correctly initializes all fields and getters return the expected values.
     */
    @Test
    public void testConstructor() {
        User user = new User("Test123");

        assertNotNull(user.getName());
        assertTrue(user.getName().length() >= 8);
        assertNull(user.getEmail());
        assertNull(user.getPhone());
        assertNull(user.getPhotoURL());
        assertFalse(user.isPhotoHidden());
        assertFalse(user.isDemoted());
        assertEquals(Role.ENTRANT, user.getRole());
    }

    /**
     * Tests that setter methods correctly assign values and that getters return those values.
     */
    @Test
    public void testSettersAndGetters() {
        User user = new User("Alice543");

        user.setName("Alice");
        user.setEmail("lost@wonderland.queen");
        user.setPhone("(980) 765-4321");
        user.setPhotoURL(null);
        user.setPhotoHidden(true);
        user.setRole(Role.ORGANIZER);

        assertEquals("Alice", user.getName());
        assertEquals("lost@wonderland.queen", user.getEmail());
        assertEquals("(980) 765-4321", user.getPhone());
        assertNull(user.getPhotoURL());
        assertTrue(user.isPhotoHidden());
        assertFalse(user.isDemoted());
        assertEquals(Role.ORGANIZER, user.getRole());
    }

    /**
     * Tests that roles can be added and removed properly.
     */
    @Test
    public void testSetRole() {
        User user = new User("Tester745");
        user.setRole(Role.ENTRANT);
        assertEquals(Role.ENTRANT, user.getRole());
        assertFalse(user.isDemoted());

        // promote role
        user.setRole(Role.ORGANIZER);
        assertEquals(Role.ORGANIZER, user.getRole());
        assertFalse(user.isDemoted());

        // demote
        user.setRole(Role.ENTRANT);
        assertEquals(Role.ENTRANT, user.getRole());
        assertTrue(user.isDemoted());
    }

    /**
     * Tests that users have the abilities of all their inferior roles.
     * ie, organizers can do everything entrants can do
     */
    @Test
    public void testHasAbilitiesOfRole() {
        User user = new User("Testington999");
        user.setRole(Role.ENTRANT);
        assertTrue(user.hasAbilitiesOfRole(Role.ENTRANT));
        assertFalse(user.hasAbilitiesOfRole(Role.ORGANIZER));

        // promote role
        user.promote();
        assertTrue(user.hasAbilitiesOfRole(Role.ENTRANT));
        assertTrue(user.hasAbilitiesOfRole(Role.ORGANIZER));
        assertFalse(user.hasAbilitiesOfRole(Role.ADMIN));

        // demote
        user.demote();
        assertTrue(user.hasAbilitiesOfRole(Role.ENTRANT));
        assertFalse(user.hasAbilitiesOfRole(Role.ORGANIZER));
    }

    /**
     * Tests that roles can be promoted and demoted properly.
     */
    @Test
    public void testPromoteDemoteRole() {
        User user = new User("john444");
        user.setRole(Role.ENTRANT);
        assertEquals(Role.ENTRANT, user.getRole());
        assertFalse(user.isDemoted());

        // promote role
        user.promote();
        assertEquals(Role.ORGANIZER, user.getRole());
        assertFalse(user.isDemoted());

        // demote
        user.demote();
        assertEquals(Role.ENTRANT, user.getRole());
        assertTrue(user.isDemoted());
    }

    /**
     * Tests that the {@code photoHidden} flag can be toggled and retrieved correctly.
     */
    @Test
    public void testHiddenFlag() {
        User user = new User("josh457");
        user.setPhotoHidden(false);

        assertFalse(user.isPhotoHidden());

        user.setPhotoHidden(true);

        assertTrue(user.isPhotoHidden());
    }
}
