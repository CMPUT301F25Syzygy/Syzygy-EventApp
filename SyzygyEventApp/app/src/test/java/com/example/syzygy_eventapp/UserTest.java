package com.example.syzygy_eventapp;

import static org.junit.Assert.*;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.ListenerRegistration;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Unit tests for the {@link User} model class.
 * <p>
 * These tests verify that the User class correctly handles field initialization, getter/setter methods, role validation, and flag updates.
 * </p>
 */
public class UserTest {
    @Before
    public void setup() {
        UserController.overrideInstance(new UserControllerMock());
    }

    /**
     * Tests that the full constructor correctly initializes all fields and getters return the expected values.
     */
    @Test
    public void testConstructor() {
        User user = new User();

        assertNull(user.getUserID());
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
     * Tests that organizer is properly made from Admin.promote()
     */
    @Test
    public void testFromOrganizerDemote() {
        // make a base user to inherit from and compare against
        Organizer organizer = new Organizer(
                "U001",
                "Test Organizer",
                "organizer@example.com",
                "123-456-7890",
                "https://testphoto.com/test.jpg",
                false,
                false,
                new ArrayList<String>(){{
                    add("event1");
                }},
                Role.ORGANIZER
        );

        User user = organizer.demote();

        assertEquals(organizer.getUserID(), user.getUserID());
        assertEquals(organizer.getName(), user.getName());
        assertEquals(organizer.getEmail(), user.getEmail());
        assertEquals(Role.ENTRANT, user.getRole());

        assertEquals(User.class, user.getClass());
    }

    /**
     * Tests that setter methods correctly assign values and that getters return those values.
     */
    @Test
    public void testSettersAndGetters() {
        User user = new User();

        user.setUserID("Alice543");
        user.setName("Alice");
        user.setEmail("lost@wonderland.queen");
        user.setPhone("(980) 765-4321");
        user.setPhotoURL(null);
        user.setPhotoHidden(true);
        user.setRole(Role.ORGANIZER);

        assertEquals("Alice543", user.getUserID());
        assertEquals("Alice", user.getName());
        assertEquals("lost@wonderland.queen", user.getEmail());
        assertEquals("(980) 765-4321", user.getPhone());
        assertNull(user.getPhotoURL());
        assertTrue(user.isPhotoHidden());
        assertFalse(user.isDemoted());
        assertEquals(Role.ORGANIZER, user.getRole());
    }

    /**
     * Tests that users have the abilities of all their inferior roles.
     * ie, organizers can do everything entrants can do
     */
    @Test
    public void testHasAbilitiesOfRole() {
        User user = new User();
        user.setRole(Role.ENTRANT);
        assertTrue(user.hasAbilitiesOfRole(Role.ENTRANT));
        assertFalse(user.hasAbilitiesOfRole(Role.ORGANIZER));

        // promote role
        user = user.promote();
        assertTrue(user.hasAbilitiesOfRole(Role.ENTRANT));
        assertTrue(user.hasAbilitiesOfRole(Role.ORGANIZER));
        assertFalse(user.hasAbilitiesOfRole(Role.ADMIN));

        // demote
        user = user.demote();
        assertTrue(user.hasAbilitiesOfRole(Role.ENTRANT));
        assertFalse(user.hasAbilitiesOfRole(Role.ORGANIZER));
    }

    /**
     * Tests that roles can be promoted and demoted properly.
     */
    @Test
    public void testPromoteDemoteRole() {
        User user = new User();
        assertEquals(Role.ENTRANT, user.getRole());
        assertFalse(user.isDemoted());

        // promote role
        user = user.promote();
        assertEquals(Role.ORGANIZER, user.getRole());
        assertFalse(user.isDemoted());

        // demote
        user = user.demote();
        assertEquals(Role.ENTRANT, user.getRole());
        assertTrue(user.isDemoted());
    }

    /**
     * Tests that the {@code photoHidden} flag can be toggled and retrieved correctly.
     */
    @Test
    public void testHiddenFlag() {
        User user = new User();
        user.setPhotoHidden(false);

        assertFalse(user.isPhotoHidden());

        user.setPhotoHidden(true);

        assertTrue(user.isPhotoHidden());
    }
}
