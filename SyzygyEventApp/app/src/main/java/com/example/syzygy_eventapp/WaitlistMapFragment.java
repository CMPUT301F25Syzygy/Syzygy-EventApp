package com.example.syzygy_eventapp;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Fragment that displays entrant locations for an event on an OSMdroid map (Couldn't use the google maps API, and I liked the functionality of this)
 * <p>
 * The map clusters nearby entrants by rounding their coordinates, then displays a marker showing how many entrants joined from that approximate area.
 * </p>
 */
public class WaitlistMapFragment extends Fragment {

    private static final String TAG = "WaitlistMapFragment";
    private Event event;
    private NavigationStackFragment navStack;
    private EventController eventController;
    private MapView mapView;
    private ListenerRegistration locationListener;

    // Constructor
    public WaitlistMapFragment(Event event, NavigationStackFragment navStack) {
        this.event = event;
        this.navStack = navStack;
        this.eventController = EventController.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Load/initialize the osmdroid using shared preferences
        Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));

        View view = inflater.inflate(R.layout.fragment_waitlist_map, container, false);

        // Set up the map view
        mapView = view.findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(10.0);

        // Back button
        view.findViewById(R.id.back_button).setOnClickListener(v -> {
            if (navStack != null) {
                navStack.popScreen();
            }
        });

        // Start listening for enterant location updates
        loadEntrantLocations();

        return view;
    }

    /**
     * Listen to Firestore updates for entrant locations and update the map whenever new data arrives.
     */
    private void loadEntrantLocations() {
        locationListener = eventController.observeEntrantLocations(
                event.getEventID(),
                // Success callback
                this::displayLocationsOnMap,
                error -> {
                    // Error callback
                    Log.e(TAG, "Error loading locations", error);
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Error loading locations: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Displays a set of entrant locations on the map.
     * Nearby points are clustered together by rounding coordinates to 2 decimal places.
     *
     * @param locations A list of maps containing entrant data (must include a "location" GeoPoint).
     */
    private void displayLocationsOnMap(List<Map<String, Object>> locations) {
        if (mapView == null || !isAdded()) return;

        // Remove old markers
        mapView.getOverlays().clear();

        // Group locations by proximity for clustering by just rounding lat/lon to 2 decimal places
        Map<String, Integer> locationCounts = new HashMap<>();

        for (Map<String, Object> data : locations) {
            GeoPoint geoPoint = (GeoPoint) data.get("location");
            if (geoPoint != null) {
                // Simple clustering by rounding to 2 decimal places
                String key = String.format("%.2f,%.2f", geoPoint.getLatitude(), geoPoint.getLongitude());
                locationCounts.put(key, locationCounts.getOrDefault(key, 0) + 1);
            }
        }

        // Add markers for each cluster
        org.osmdroid.util.GeoPoint firstPosition = null; // Use fully qualified name
        for (Map.Entry<String, Integer> entry : locationCounts.entrySet()) {
            String[] coords = entry.getKey().split(",");
            double lat = Double.parseDouble(coords[0]);
            double lon = Double.parseDouble(coords[1]);
            org.osmdroid.util.GeoPoint position = new org.osmdroid.util.GeoPoint(lat, lon); // Fully qualified

            if (firstPosition == null) firstPosition = position;

            int count = entry.getValue();
            String title = count == 1 ? "1 entrant" : count + " entrants";

            Marker marker = new Marker(mapView);
            marker.setPosition(position);
            marker.setTitle(title);
            marker.setSnippet("Joined from this area");
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);
        }

        // Center camera on first location
        if (firstPosition != null) {
            mapView.getController().setCenter(firstPosition);
        }

        mapView.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationListener != null) {
            locationListener.remove();
        }
    }
}