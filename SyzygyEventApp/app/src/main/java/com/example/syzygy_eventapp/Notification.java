package com.example.syzygy_eventapp;

import com.google.firebase.Timestamp;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Notification model class.
 */
public class Notification {
    // max safe integer that can be stored in a double, thanks javascript <3
    private static final long MAX_SAFE_DOUBLE_INTEGER = 9007199254740991L; // 2 ^ 53 - 1

    private long id;
    private String title;
    private String description;
    private String eventId;
    private String organizerId;
    private Timestamp creationDate;
    private boolean sent;
    private boolean deleted;

    public Notification(String title, String description, String eventId, String organizerId) {
        this.id = ThreadLocalRandom.current().nextLong(MAX_SAFE_DOUBLE_INTEGER);
        this.title = title;
        this.description = description;
        this.eventId = eventId;
        this.organizerId = organizerId;
        this.creationDate = Timestamp.now();
    }

    /**
     * Default constructor for frameworks requiring this.
     */
    public Notification() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }
}
