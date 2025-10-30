package com.example.syzygy_eventapp;


import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * A utility class for getting a unique ID
 * that is associated with the installation 
 * of this app.
 */
public final class AppInstallationId {
    /// The name of the SharedPreferences file.
    /// This can be used for testing purposes.
    public static final String PREFS_FILE = "device_id.xml";
    /// The key for the installation ID in SharedPreferences.
    /// This can be used for testing purposes.
    public static final String PREFS_INSTALLATION_ID = "device_id";

    /**
     * The constructor. Marked as private as this
     * class should not be instantiated.
     */
    private AppInstallationId() {}

    /**
     * Gets a unique ID that is associated with
     * the installation of this app.
     * @param context The context the application is currently in.
     *                This can be retrieved from activities, views,
     *                fragments, etc.
     * @return  A unique ID that is associated with
     *          the installation of this app.
     */
    public static String get(Context context) {
        // Get the SharedPreferences for this app, which is the
        // locally-stored data associated with this app on a given
        // Android device.
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);

        // Get the installationId from the SharedPreferences.
        String installationId = prefs.getString(PREFS_INSTALLATION_ID, null);

        // If there is no installationId in SharedPreferences, then
        // generate a new UUID and update it in SharedPreferences.
        if (installationId == null) {
            installationId = UUID.randomUUID().toString();
            prefs.edit().putString(PREFS_INSTALLATION_ID, installationId).apply();
        }

        // Return the installationId.
        return installationId;
    }
}
