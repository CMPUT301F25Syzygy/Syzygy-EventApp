package com.example.syzygy_eventapp;

public class UserNotification {
    private String userId;
    private int notificationId;
    private boolean sent = false;

    public UserNotification() {

    }

    public UserNotification(String userId, int notificationId) {
        this.userId = userId;
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }
}
