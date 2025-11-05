package com.example.syzygy_eventapp;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the {@link Organizer} model class.
 * <p>
 *     These tests verify correct inheritance from {@link User}, and proper management of owned event IDs.
 * </p>
 */

public class OrganizerTest {
    @Before
    public void setup() {
        UserController.overrideInstance(new UserControllerMock());
    }

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
        assertNotNull(organizer.getName());
        assertNull(organizer.getEmail());
    }

    /**
     * Tests that organizer is properly made from User.promote()
     */
    @Test
    public void testFromEntrantPromote() {
        // make a base user to inherit from and compare against
        User user = new User(
                "U001",
                "Test Organizer",
                "organizer@example.com",
                "123-456-7890",
                "https://testphoto.com/test.jpg",
                false,
                false,
                Role.ENTRANT
        );

        Organizer organizer = user.promote();

        assertEquals(user.getUserID(), organizer.getUserID());
        assertEquals(user.getName(), organizer.getName());
        assertEquals(user.getEmail(), organizer.getEmail());
        assertEquals(Role.ORGANIZER, organizer.getRole());

        assertNotNull(organizer.getOwnedEventIDs());
        assertTrue(organizer.getOwnedEventIDs().isEmpty());

        assertEquals(Organizer.class, organizer.getClass());
    }

    /**
     * Tests that organizer is properly made from Admin.promote()
     */
    @Test
    public void testFromAdminDemote() {
        // make a base user to inherit from and compare against
        Admin admin = new Admin(
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
                Role.ADMIN
        );

        Organizer organizer = admin.demote();

        assertEquals(admin.getUserID(), organizer.getUserID());
        assertEquals(admin.getName(), organizer.getName());
        assertEquals(admin.getEmail(), organizer.getEmail());
        assertEquals(Role.ORGANIZER, organizer.getRole());

        assertEquals(admin.getOwnedEventIDs(), organizer.getOwnedEventIDs());

        assertEquals(Organizer.class, organizer.getClass());
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

    /**
     * Tests testHasOwnedEventID properly checks for event IDs.
     */
    @Test
    public void testHasOwnedEventID() {
        Organizer organizer = new Organizer();

        List<String> ids = new ArrayList<>();
        ids.add("event1");
        ids.add("event2");

        organizer.setOwnedEventIDs(ids);


        assertTrue(organizer.hasOwnedEventID("event1"));
        assertFalse(organizer.hasOwnedEventID("event5"));
    }
}
