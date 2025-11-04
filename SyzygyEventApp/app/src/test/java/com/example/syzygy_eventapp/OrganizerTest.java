package com.example.syzygy_eventapp;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for the {@link Organizer} model class.
 * <p>
 *     These tests verify correct inheritance from {@link User}, and proper management of owned event IDs.
 * </p>
 */

public class OrganizerTest {

    /**
     * Tests that the default constructor initializes empty event list and inherited fields.
     */

    @Test
    public void testDefaultConstructor() {
        Organizer organizer = new Organizer();

        assertNotNull("Owned event list should not be null", organizer.getOwnedEventIDs());
        assertTrue("Owned event list should start empty", organizer.getOwnedEventIDs().isEmpty());

        // inherited from User
        assertNull(organizer.getUserID());
        assertNull(organizer.getName());
        assertNull(organizer.getEmail());
    }

    /**
     * Tests that the constructor using a base User copies its fields correctly.
     */
    @Test
    public void testConstructorFromUser() {
        // make a base user to inherit from and compare against
        User baseUser = new User(
                "U001",
                "Test Organizer",
                "organizer@example.com",
                "123-456-7890",
                "https://testphoto.com/test.jpg",
                false,
                false,
                Arrays.asList(Role.ORGANIZER),
                Role.ORGANIZER
        );

        Organizer organizer = new Organizer(baseUser);

        assertEquals(baseUser.getUserID(), organizer.getUserID());
        assertEquals(baseUser.getName(), organizer.getName());
        assertEquals(baseUser.getEmail(), organizer.getEmail());
        assertEquals(baseUser.getRoles(), organizer.getRoles());
        assertEquals(baseUser.getActiveRole(), organizer.getActiveRole());

        assertNotNull(organizer.getOwnedEventIDs());
        assertTrue(organizer.getOwnedEventIDs().isEmpty());
    }

    /**
     * Tests that setter and getter for ownedEventIDs work properly.
     */
    @Test
    public void testSetAndGetOwnedEventIDs() {
        Organizer organizer = new Organizer();

        List<String> ids = new ArrayList<>();
        ids.add("event1");
        ids.add("event2");

        organizer.setOwnedEventIDs(ids);

        assertEquals(2, organizer.getOwnedEventIDs().size());
        assertTrue(organizer.getOwnedEventIDs().contains("event1"));
    }

    /**
     * Tests addOwnedEventID only adds valid and unique IDs.
     */
    @Test
    public void testAddOwnedEventID() {
        Organizer organizer = new Organizer();

        // should add
        organizer.addOwnedEventID("E001");
        // duplicate, should not add
        organizer.addOwnedEventID("E001");
        // invalid, should ignore
        organizer.addOwnedEventID("");
        // also invalid, should ignore
        organizer.addOwnedEventID(null);
        // should add
        organizer.addOwnedEventID("E002");

        List<String> ids = organizer.getOwnedEventIDs();

        assertEquals(2, ids.size());
        assertTrue(ids.contains("E001"));
        assertTrue(ids.contains("E002"));
    }

    /**
     * Tests removeOwnedEventID properly removes an existing ID.
     */
    @Test
    public void testRemoveOwnedEventID() {
        Organizer organizer = new Organizer();
        organizer.addOwnedEventID("A123");
        organizer.addOwnedEventID("B456");

        organizer.removeOwnedEventID("A123");

        assertEquals(1, organizer.getOwnedEventIDs().size());
        assertFalse(organizer.getOwnedEventIDs().contains("A123"));
        assertTrue(organizer.getOwnedEventIDs().contains("B456"));
    }

}
