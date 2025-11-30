package com.example.syzygy_eventapp;

public class ImageWrapper {
    /// The image, encoded in Base64
    private String imageBase64 = null;

    /// The source type of the image, either User or Event
    private ImageSourceType imageSourceType = null;

    /// The ID of the User the image is sourced from.
    /// Null if imageSourceType is not User.
    private String userID = null;

    /// The ID of the Event the image is sourced from.
    /// Null if imageSourceType is not Event.
    private String eventID = null;

    public String getImageBase64() {
        return this.imageBase64;
    }

    public ImageSourceType getImageSourceType() {
        return this.imageSourceType;
    }

    public String getUserID() {
        return this.userID;
    }

    public String getEventID() {
        return this.eventID;
    }

    public ImageWrapper(User user) {
        this.imageBase64 = user.getPhotoURL();
        this.imageSourceType = ImageSourceType.USER;
        this.userID = user.getUserID();
        this.eventID = null;
    }

    public ImageWrapper(Event event) {
        this.imageBase64 = event.getPosterUrl();
        this.imageSourceType = ImageSourceType.EVENT;
        this.userID = null;
        this.eventID = event.getEventID();
    }

    public enum ImageSourceType {
        USER,
        EVENT
    }
}
