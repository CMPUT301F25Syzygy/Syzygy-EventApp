package com.example.syzygy_eventapp;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;


/**
 * Unit tests for Event class
 */
public class EventTest {

    private Event event;

    @Before
    public void setUp() {
        // Create a sample event for testing
        event = new Event();
    }

    @Test
    public void testDefaultConstructor() {
        assertNotNull(event);
        assertNull(event.getEventID());
        assertNull(event.getName());
        assertNull(event.getWaitingList());
    }

    @Test
    public void testSetAndGetEventID() {
        String testID = "event123";
        event.setEventID(testID);
        assertEquals(testID, event.getEventID());
    }

    @Test
    public void testSetAndGetName() {
        String testName = "Summer Concert";
        event.setName(testName);
        assertEquals(testName, event.getName());
    }

    @Test
    public void testSetAndGetDescription() {
        String desc = "An amazing outdoor concert";
        event.setDescription(desc);
        assertEquals(desc, event.getDescription());
    }

    @Test
    public void testSetAndGetOrganizerID() {
        String organizerID = "organizer456";
        event.setOrganizerID(organizerID);
        assertEquals(organizerID, event.getOrganizerID());
    }

    @Test
    public void testSetAndGetLocation() {
        String locationName = "City Park";
        GeoPoint coords = new GeoPoint(53.5461, -113.4938); // Edmonton

        event.setLocationName(locationName);
        event.setLocationCoordinates(coords);

        assertEquals(locationName, event.getLocationName());
        assertEquals(coords, event.getLocationCoordinates());
    }

    @Test
    public void testGeolocationRequired() {
        event.setGeolocationRequired(true);
        assertTrue(event.isGeolocationRequired());

        event.setGeolocationRequired(false);
        assertFalse(event.isGeolocationRequired());
    }

    @Test
    public void testWaitingList() {
        ArrayList<String> waitingList = new ArrayList<>(Arrays.asList("user1", "user2", "user3"));
        event.setWaitingList(waitingList);

        assertEquals(3, event.getWaitingList().size());
        assertTrue(event.getWaitingList().contains("user1"));
    }

    @Test
    public void testMaxWaitingListUnlimited() {
        event.setMaxWaitingList(null);
        assertNull(event.getMaxWaitingList()); // null = unlimited
    }

    @Test
    public void testMaxWaitingListLimited() {
        event.setMaxWaitingList(100);
        assertEquals(Integer.valueOf(100), event.getMaxWaitingList());
    }

    @Test
    public void testMaxAttendees() {
        event.setMaxAttendees(50);
        assertEquals(Integer.valueOf(50), event.getMaxAttendees());
    }

    @Test
    public void testLotteryComplete() {
        event.setLotteryComplete(false);
        assertFalse(event.isLotteryComplete());

        event.setLotteryComplete(true);
        assertTrue(event.isLotteryComplete());
    }

    @Test
    public void testTimestamps() {
        Timestamp now = new Timestamp(new Date());

        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        event.setRegistrationStart(now);
        event.setRegistrationEnd(now);

        assertEquals(now, event.getCreatedAt());
        assertEquals(now, event.getUpdatedAt());
        assertEquals(now, event.getRegistrationStart());
        assertEquals(now, event.getRegistrationEnd());
    }

    @Test
    public void testPosterUrl() {
        String posterUrl = "https://example.com/poster.jpg";
        event.setPosterUrl(posterUrl);
        assertEquals(posterUrl, event.getPosterUrl());
    }

    @Test
    public void testQRCodeData() {
        String qrData = "event://event123";
        event.setQrCodeData(qrData);
        assertEquals(qrData, event.getQrCodeData());
    }

    @Test
    public void testFullConstructor() {
        String eventID = "event123";
        String name = "Test Event";
        String description = "Test Description";
        String organizerID = "org456";
        String locationName = "Test Location";
        GeoPoint coords = new GeoPoint(0.0, 0.0);
        boolean geolocationRequired = true;
        String posterUrl = "https://example.com/poster.jpg";
        ArrayList<String> waitingList = new ArrayList<>(Arrays.asList("user1"));
        ArrayList<String> invites = new ArrayList<>(Arrays.asList("example-invite6346", "example-invite8561"));
        Integer maxWaitingList = 100;
        Timestamp now = new Timestamp(new Date());
        Integer maxAttendees = 50;
        boolean lotteryComplete = false;
        String qrCodeData = "event://event123";

        Event fullEvent = new Event(
                eventID, name, description, organizerID, now,
                locationName, coords, geolocationRequired,
                posterUrl, waitingList, invites, maxWaitingList,
                now, now, maxAttendees, lotteryComplete,
                qrCodeData, now, now
        );

        assertEquals(eventID, fullEvent.getEventID());
        assertEquals(name, fullEvent.getName());
        assertEquals(description, fullEvent.getDescription());
        assertEquals(organizerID, fullEvent.getOrganizerID());
        assertEquals(locationName, fullEvent.getLocationName());
        assertEquals(coords, fullEvent.getLocationCoordinates());
        assertEquals(geolocationRequired, fullEvent.isGeolocationRequired());
        assertEquals(posterUrl, fullEvent.getPosterUrl());
        assertEquals(1, fullEvent.getWaitingList().size());
        assertEquals(2, fullEvent.getInvites().size());
        assertEquals(maxWaitingList, fullEvent.getMaxWaitingList());
        assertEquals(maxAttendees, fullEvent.getMaxAttendees());
        assertEquals(lotteryComplete, fullEvent.isLotteryComplete());
        assertEquals(qrCodeData, fullEvent.getQrCodeData());
    }
}

