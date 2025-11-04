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
                Role.ORGANIZER
        );

        assertEquals("Test123", user.getUserID());
        assertEquals("Tester Testington", user.getName());
        assertEquals("test@testmail.com", user.getEmail());
        assertEquals("(123) 456-7890", user.getPhone());
        assertEquals("https://testphoto.com/test.jpg", user.getPhotoURL());
        assertFalse(user.isPhotoHidden());
        assertFalse(user.isDemoted());
        assertEquals(Role.ORGANIZER, user.getRole());
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
        user.setRole(Role.ENTRANT);

        assertEquals("user1", user.getUserID());
        assertEquals("Alice", user.getName());
        assertEquals("lost@wonderland.queen", user.getEmail());
        assertEquals("(980) 765-4321", user.getPhone());
        assertNull(user.getPhotoURL());
        assertTrue(user.isPhotoHidden());
        assertTrue(user.isDemoted());
        assertEquals(Role.ENTRANT, user.getRole());
    }

    /**
     * Tests that roles can be added and removed properly.
     */
    @Test
    public void testSetRole() {
        User user = new User();
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
        User user = new User();
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
        User user = new User();
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
        User user = new User();
        user.setPhotoHidden(false);

        assertFalse(user.isPhotoHidden());

        user.setPhotoHidden(true);

        assertTrue(user.isPhotoHidden());
    }
}
