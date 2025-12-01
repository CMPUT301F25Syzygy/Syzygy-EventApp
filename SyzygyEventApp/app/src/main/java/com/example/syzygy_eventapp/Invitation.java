package com.example.syzygy_eventapp;

import com.google.firebase.Timestamp;

/**
 * Invitation Model representing an organizer's invite sent to a user/entrant for a specific event.
 * Synced with DB. Other classes are responsible for initializing values upon creation.
 */
public class Invitation {

    private String invitation;
    private String event;
    private String organizerID;
    private String recipientID;

    /** The Boolean "accepted" equals true if the invite has been accepted, false if rejected, and null if pending. */
    private Boolean accepted;
    private Boolean cancelled;

    private Timestamp sendTime;
    private Timestamp responseTime;
    private Timestamp cancelTime;

    /** Default Constructor used for creating invitations normally. */
    public Invitation() {
    }

    /** Constructor with all values, used mostly for testing. */
    public Invitation(String invitation, String event, String organizerID, String recipientID, Boolean accepted, Timestamp sendTime, Timestamp responseTime, Boolean cancelled, Timestamp cancelTime) {
        this.invitation = invitation;
        this.event = event;
        this.organizerID = organizerID;
        this.recipientID = recipientID;
        this.accepted = accepted;
        this.sendTime = sendTime;
        this.responseTime = responseTime;
        this.cancelled = cancelled;
        this.cancelTime = cancelTime;
    }

    public String getInvitation() {
        return invitation;
    }

    public void setInvitation(String invitation) {
        this.invitation = invitation;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getOrganizerID() {
        return organizerID;
    }

    public void setOrganizerID(String organizerID) {
        this.organizerID = organizerID;
    }

    public String getRecipientID() {
        return recipientID;
    }

    public void setRecipientID(String recipientID) {
        this.recipientID = recipientID;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public Timestamp getSendTime() {
        return sendTime;
    }

    public void setSendTime(Timestamp sendTime) {
        this.sendTime = sendTime;
    }

    public Timestamp getResponseTime() {
        return responseTime;
    }

    public boolean hasResponse() {
        return responseTime != null;
    }


    public void setResponseTime(Timestamp responseTime) {
        this.responseTime = responseTime;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Timestamp getCancelTime() {
        return cancelTime;
    }

    public void setCancelTime(Timestamp cancelTime) {
        this.cancelTime = cancelTime;
    }
}
