package com.example.syzygy_eventapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.google.firebase.firestore.GeoPoint;

public class Maps {
    public static boolean openInMaps(Activity activity, Event event) {
        GeoPoint locationCoordinates = event.getLocationCoordinates();
        String locationName = event.getLocationName();

        if (locationName == null) {
            Toast.makeText(activity, "Location not available", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Create a URI for the location
        Uri mapUri;
        if (locationCoordinates != null) {
            // Use coordinates if available for more accurate location
            double lat = locationCoordinates.getLatitude();
            double lng = locationCoordinates.getLongitude();
            mapUri = Uri.parse("geo:" + lat + "," + lng + "?q=" + Uri.encode(locationName));
        } else {
            // Fall back to address search
            mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(locationName));
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        // Check if Google Maps is installed
        if (mapIntent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(mapIntent);
        } else {
            // Fall back to web browser
            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" +
                            Uri.encode(locationName)));
            activity.startActivity(webIntent);
        }

        return true;
    }
}
