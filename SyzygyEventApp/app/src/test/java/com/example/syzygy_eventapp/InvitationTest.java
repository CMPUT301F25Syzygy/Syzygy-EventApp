package com.example.syzygy_eventapp;

import static org.junit.Assert.*;
import org.junit.Test;

import com.google.firebase.Timestamp;

public class InvitationTest {

    @Test
    public void testConstructorAndGetters() {
        Timestamp sendTime = new Timestamp(100L, 0);
        Timestamp responseTime = new Timestamp(200L, 0);

        Invitation invite = new Invitation("testInvite", "testEvent", "user001", "user002", true, sendTime, responseTime);

        assertEquals("testInvite", invite.getInvitation());
        assertEquals("testEvent", invite.getEvent());
        assertEquals("user001", invite.getOrganizerID());
        assertEquals("user002", invite.getRecipientID());
        assertTrue(invite.getAccepted());
        assertEquals(sendTime, invite.getSendTime());
        assertEquals(responseTime, invite.getResponseTime());
    }
}
