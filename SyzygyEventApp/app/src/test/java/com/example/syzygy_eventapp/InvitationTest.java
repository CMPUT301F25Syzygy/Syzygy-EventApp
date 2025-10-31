package com.example.syzygy_eventapp;

import static org.junit.Assert.*;
import org.junit.Test;

import com.google.firebase.Timestamp;

/**
 * Unit tests for the {@link Invitation} model class.
 * <p>
 *     These tests verify that the Invitation class correctly handles field initialization, and getter/setter methods.
 * </p>
 */
public class InvitationTest {

    /**
     * Tests that the full constructor correctly initializes all fields and getters return the expected values.
     */
    @Test
    public void testConstructorAndGetters() {
        Timestamp sendTime = new Timestamp(100L, 0);
        Timestamp responseTime = new Timestamp(200L, 0);
        Timestamp cancelTime = new Timestamp(300L, 0);

        Invitation invite = new Invitation("testInvite", "testEvent", "user001", "user002", true, sendTime, responseTime, false, cancelTime);

        assertEquals("testInvite", invite.getInvitation());
        assertEquals("testEvent", invite.getEvent());
        assertEquals("user001", invite.getOrganizerID());
        assertEquals("user002", invite.getRecipientID());
        assertTrue(invite.getAccepted());
        assertEquals(sendTime, invite.getSendTime());
        assertEquals(responseTime, invite.getResponseTime());
        assertFalse(invite.getCancelled());
        assertEquals(cancelTime, invite.getCancelTime());
    }

    /**
     * Tests that setter methods correctly assign values and that getters return those values.
     */
    @Test
    public void testSetters() {
        Timestamp sendTime = new Timestamp(0L, 123);
        Timestamp responseTime = new Timestamp(1L, 456);
        Timestamp cancelTime = new Timestamp(2L, 789);

        Invitation invite = new Invitation();

        invite.setInvitation("testInvite2");
        invite.setEvent("testEvent2");
        invite.setOrganizerID("user003");
        invite.setRecipientID("user004");
        invite.setAccepted(false);
        invite.setSendTime(sendTime);
        invite.setResponseTime(responseTime);
        invite.setCancelled(true);
        invite.setCancelTime(cancelTime);

        assertEquals("testInvite2", invite.getInvitation());
        assertEquals("testEvent2", invite.getEvent());
        assertEquals("user003", invite.getOrganizerID());
        assertEquals("user004", invite.getRecipientID());
        assertFalse(invite.getAccepted());
        assertEquals(sendTime, invite.getSendTime());
        assertEquals(responseTime, invite.getResponseTime());
        assertTrue(invite.getCancelled());
        assertEquals(cancelTime, invite.getCancelTime());
    }

    /**
     * Tests that the accepted variable supports null (pending), true (accepted), and false (rejected).
     */
    @Test
    public void testAccepted() {
        Invitation invite = new Invitation();

        // Pending
        invite.setAccepted(null);
        assertNull(invite.getAccepted());

        // Accepted
        invite.setAccepted(true);
        assertTrue(invite.getAccepted());

        // Rejected
        invite.setAccepted(false);
        assertFalse(invite.getAccepted());
    }
}
