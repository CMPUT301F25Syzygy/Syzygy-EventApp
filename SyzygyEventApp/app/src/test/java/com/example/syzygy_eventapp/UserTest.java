package com.example.syzygy_eventapp;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for the {@link User} model class.
 * <p>
 *     These tests verify that the User class correctly handles field initialization, getter/setter methods, role validation, and flag updates.
 * </p>
 */
public class UserTest {

    /**
     * Tests that the full constructor correctly initializes all fields and getters return the expected values.
     */
    @Test
    public void testConstructorAndGetters() {
        User user = new User(
                "Test123",
                "Tester Testington",
                "test@testmail.com",
                "(123) 456-7890",
                "https://testphoto.com/test.jpg",
                false,
                false,
                Arrays.asList(Role.ENTRANT, Role.ORGANIZER),
                Role.ORGANIZER
        );

        assertEquals("Test123", user.getUserID());
        assertEquals("Tester Testington", user.getName());
        assertEquals("test@testmail.com", user.getEmail());
        assertEquals("(123) 456-7890", user.getPhone());
        assertEquals("https://testphoto.com/test.jpg", user.getPhotoURL());
        assertFalse(user.isPhotoHidden());
        assertFalse(user.isDemoted());
        assertTrue(user.getRoles().contains(Role.ENTRANT));
        assertTrue(user.getRoles().contains(Role.ORGANIZER));
        assertEquals(Role.ORGANIZER, user.getActiveRole());
    }

    /**
     * Tests that setter methods correctly assign values and that getters return those values.
     */
    @Test
    public void testSetters() {
        User user = new User();

        user.setUserID("user1");
        user.setName("Alice");
        user.setEmail("lost@wonderland.queen");
        user.setPhone("(980) 765-4321");
        user.setPhotoURL(null);
        user.setPhotoHidden(true);
        user.setDemoted(true);
        user.setRoles(Collections.singletonList(Role.ENTRANT));
        user.setActiveRole(Role.ENTRANT);

        assertEquals("user1", user.getUserID());
        assertEquals("Alice", user.getName());
        assertEquals("lost@wonderland.queen", user.getEmail());
        assertEquals("(980) 765-4321", user.getPhone());
        assertNull(user.getPhotoURL());
        assertTrue(user.isPhotoHidden());
        assertTrue(user.isDemoted());
        assertTrue(user.getRoles().contains(Role.ENTRANT));
        assertEquals(Role.ENTRANT, user.getActiveRole());
    }

    /**
     * Tests that {@link User#hasValidActiveRole()} returns true when the user's active role is included in their assigned roles.
     */
    @Test
    public void testValidActiveRole() {
        User user = new User();
        user.setRoles(Arrays.asList(Role.ENTRANT, Role.ORGANIZER));
        user.setActiveRole(Role.ORGANIZER);

        assertTrue(user.hasValidActiveRole());
    }

    /**
     * Tests that {@link User#hasValidActiveRole()} returns false when the active role is not part of the user's assigned roles.
     */
    @Test
    public void testInvalidActiveRole() {
        User user = new User();
        user.setRoles(Collections.singletonList(Role.ENTRANT));
        user.setActiveRole(Role.ADMIN);

        assertFalse(user.hasValidActiveRole());
    }

    /**
     * Tests that roles can be added and removed properly from the user's role set.
     */
    @Test
    public void testAddDeleteRoles() {
        User user = new User();
        List<Role> roles = new ArrayList<>();
        roles.add(Role.ENTRANT);
        user.setRoles(roles);

        // add new role
        user.getRoles().add(Role.ORGANIZER);
        assertTrue(user.getRoles().contains(Role.ORGANIZER));

        // demote
        boolean removed = user.getRoles().remove(Role.ORGANIZER);
        assertTrue("remove(Role.ORGANIZER) should return true", removed);
        assertFalse(user.getRoles().contains(Role.ORGANIZER));
    }

    /**
     * Tests that the {@code photoHidden} and {@code demoted} flags can be toggled and retrieved correctly.
     */
    @Test
    public void testHiddenAndDemotedFlags() {
        User user = new User();
        user.setPhotoHidden(false);
        user.setDemoted(false);

        user.setPhotoHidden(true);
        user.setDemoted(true);

        assertTrue(user.isPhotoHidden());
        assertTrue(user.isDemoted());
    }
}
