package com.example.syzygy_eventapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import java.util.List;

/**
 * Event (model)
 *
 * Responsibilities:
 *  - Store name, description, and organizer info
 *  - Store event location and whether geolocation is required
 *  - Store event poster (as URL)
 *  - Store waiting list entrants
 *  - Store registration period (start/end)
 *  - Store limits such as max waiting list size and max attendees
 *  - Store QR code data (used by QR generator and scanner)
 *  - Track timestamps for creation and update
 *
 * Collaborators:
 *  - Invitation (for invites to this event)
 *  - Entrant (for users on the waiting list)
 *  - Organizer
 */
public class Event {

    // --- Basic Info ---
    private String eventID;
    private String name;
    private String description;
    private String organizerID;

    // --- Location ---
    private String locationName;          // e.g., "10230 Jasper Ave, Edmonton, AB"
    private GeoPoint locationCoordinates; // Firestore-compatible coordinates
    private boolean geolocationRequired;  // Whether location is required for this event

    // --- Poster / Media ---
    private String posterUrl;             // Stored in Firebase Storage, referenced by URL

    // --- Waiting List ---
    private List<String> waitingList;     // List of user IDs in waiting list
    private Integer maxWaitingList;       // null or 0 = unlimited

    // --- Registration Period ---
    private Timestamp registrationStart;
    private Timestamp registrationEnd;

    // --- Lottery / Capacity ---
    private Integer maxAttendees;         // Max entrants selected from lottery
    private boolean lotteryComplete;      // True when lottery is done

    // --- QR Code ---
    private String qrCodeData;            // Data embedded in generated QR code

    // --- Metadata ---
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // --- Required empty constructor for Firestore ---
    public Event() {}

    // --- Full constructor (optional, for easier testing / creation) ---
    public Event(String eventID, String name, String description, String organizerID,
                 String locationName, GeoPoint locationCoordinates, boolean geolocationRequired,
                 String posterUrl, List<String> waitingList, Integer maxWaitingList,
                 Timestamp registrationStart, Timestamp registrationEnd,
                 Integer maxAttendees, boolean lotteryComplete, String qrCodeData,
                 Timestamp createdAt, Timestamp updatedAt) {
        this.eventID = eventID;
        this.name = name;
        this.description = description;
        this.organizerID = organizerID;
        this.locationName = locationName;
        this.locationCoordinates = locationCoordinates;
        this.geolocationRequired = geolocationRequired;
        this.posterUrl = posterUrl;
        this.waitingList = waitingList;
        this.maxWaitingList = maxWaitingList;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
        this.maxAttendees = maxAttendees;
        this.lotteryComplete = lotteryComplete;
        this.qrCodeData = qrCodeData;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // --- Getters and Setters ---

    public String getEventID() { return eventID; }
    public void setEventID(String eventID) { this.eventID = eventID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOrganizerID() { return organizerID; }
    public void setOrganizerID(String organizerID) { this.organizerID = organizerID; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public GeoPoint getLocationCoordinates() { return locationCoordinates; }
    public void setLocationCoordinates(GeoPoint locationCoordinates) { this.locationCoordinates = locationCoordinates; }

    public boolean isGeolocationRequired() { return geolocationRequired; }
    public void setGeolocationRequired(boolean geolocationRequired) { this.geolocationRequired = geolocationRequired; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public List<String> getWaitingList() { return waitingList; }
    public void setWaitingList(List<String> waitingList) { this.waitingList = waitingList; }

    public Integer getMaxWaitingList() { return maxWaitingList; }
    public void setMaxWaitingList(Integer maxWaitingList) { this.maxWaitingList = maxWaitingList; }

    public Timestamp getRegistrationStart() { return registrationStart; }
    public void setRegistrationStart(Timestamp registrationStart) { this.registrationStart = registrationStart; }

    public Timestamp getRegistrationEnd() { return registrationEnd; }
    public void setRegistrationEnd(Timestamp registrationEnd) { this.registrationEnd = registrationEnd; }

    public Integer getMaxAttendees() { return maxAttendees; }
    public void setMaxAttendees(Integer maxAttendees) { this.maxAttendees = maxAttendees; }

    public boolean isLotteryComplete() { return lotteryComplete; }
    public void setLotteryComplete(boolean lotteryComplete) { this.lotteryComplete = lotteryComplete; }

    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
