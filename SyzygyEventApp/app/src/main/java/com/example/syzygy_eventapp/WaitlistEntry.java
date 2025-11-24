package com.example.syzygy_eventapp;

import com.google.firebase.Timestamp;

/**
 * Represents an entry in a waitlist or event participant list.
 * Contains user information along with the relevant timestamps.
 */
public class WaitlistEntry {
    private String userId;
    private String userName;
    private Timestamp joinedAt;
    private Timestamp registrationDate;
    private Timestamp cancellationDate;

    // status will be one of: "waiting", "accepted", "pending", "rejected", "cancelled"
    private String status;

    // Required empty construcot
    public WaitlistEntry() {

    }

    public WaitlistEntry(String userId, String userName, Timestamp joinedAt, String status) {
        this.userId = userId;
        this.userName = userName;
        this.joinedAt = joinedAt;
        this.status = status;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Timestamp getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }

    public Timestamp getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(Timestamp registrationDate) { this.registrationDate = registrationDate; }

    public Timestamp getCancellationDate() { return cancellationDate; }
    public void setCancellationDate(Timestamp cancellationDate) { this.cancellationDate = cancellationDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}