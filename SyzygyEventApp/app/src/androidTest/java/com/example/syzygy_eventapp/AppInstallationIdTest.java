package com.example.syzygy_eventapp;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for the AppInstallationId class.
 * These tests verify that the installation ID is
 * generated, persisted, and retrieved correctly.
 */
@RunWith(AndroidJUnit4.class)
public class AppInstallationIdTest {

    /// The context for the tests.
    private Context context;
    /// The SharedPreferences reference used in the tests.
    private SharedPreferences prefs;

    /**
     * Sets up the test environment before each test runs.
     */
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = context.getSharedPreferences(AppInstallationId.PREFS_FILE, Context.MODE_PRIVATE);

        // Clear preferences before each test to simulate a fresh install.
        prefs.edit().clear().commit();
    }

    /**
     * Tests that a new ID is generated if none exists.
     */
    @Test
    public void generatesNewIdIfNoneExists() {
        String id = AppInstallationId.get(context);

        assertNotNull("Generated ID should not be null", id);
        assertTrue("Generated ID should look like a UUID", id.matches("[0-9a-fA-F\\-]{36}"));

        // Verify it was saved to SharedPreferences.
        String stored = prefs.getString(AppInstallationId.PREFS_INSTALLATION_ID, null);
        assertEquals("Stored ID should match returned ID", id, stored);
    }

    /**
     * Tests that the same ID is returned on subsequent calls.
     */
    @Test
    public void returnsSameIdOnSubsequentCalls() {
        String first = AppInstallationId.get(context);
        String second = AppInstallationId.get(context);

        assertEquals("Subsequent calls should return the same ID", first, second);
    }

    /**
     * Tests that the ID is properly persisted in SharedPreferences.
     */
    @Test
    public void isProperlyPersisted() {
        String first = AppInstallationId.get(context);

        // Create a new SharedPreferences reference, like a new app instance would.
        SharedPreferences newPrefs = context.getSharedPreferences("device_id.xml", Context.MODE_PRIVATE);
        String stored = newPrefs.getString("device_id", null);

        assertNotNull("Stored ID should exist", stored);
        assertEquals("Stored ID should match first generated ID", first, stored);
    }
}
