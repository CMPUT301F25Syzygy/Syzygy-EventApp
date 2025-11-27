package com.example.syzygy_eventapp;

import java.util.List;
/**
 * Notification model class.
 */
public class Notification {
    private String message;
    private String title;
    private Event event;
    private Organizer organizer;
    private List<User> recipients;
    private String id;
    private long timestamp;


    public Notification(String title, String message, Event event,
                        Organizer organizer, List<User> recipients) {
        this.title = title;
        this.message = message;
        this.event = event;
        this.organizer = organizer;
        this.recipients = recipients;
    }

    /**
     * Default constructor for frameworks requiring this.
     */
    public Notification(){

    }

    //Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return message;
    }

    public void setDescription(String message) {
        this.message = message;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Organizer getOrganizer() {
        return organizer;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }

    public List<User> getRecipients() {
        return recipients;
    }

    public void addRecipient(User user) {
        if (!recipients.contains(user)) {
            recipients.add(user);
        }
    }

    public void removeRecipient(User user) {
        recipients.remove(user);
    }
}
