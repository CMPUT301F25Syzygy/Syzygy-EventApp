package com.example.syzygy_eventapp;

import java.util.List;

public class Notification {
    private String date;
    private String message;
    private String title;
    private Event event;
    private Organizer organizer;
    private List<User> recipients;

    public Notification(String title, String message, Event event,
                        Organizer organizer, List<User> recipients) {
        this.title = title;
        this.message = message;
        this.event = event;
        this.organizer = organizer;
        this.recipients = recipients;
    }
}
