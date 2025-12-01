package com.example.syzygy_eventapp;

import com.google.firebase.Timestamp;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Notification model class.
 */
public class Notification {
    private int id = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
    private String title;
    private String description;
    private String eventId;
    private String organizerId;
    private Timestamp creationDate = Timestamp.now();
    private boolean sent = false;
    private boolean deleted = false;

    public Notification(String title, String description, String eventId, String organizerId) {
        this.title = title;
        this.description = description;
        this.eventId = eventId;
        this.organizerId = organizerId;
    }

    /**
     * Default constructor for frameworks requiring this.
     */
    public Notification() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }
}
