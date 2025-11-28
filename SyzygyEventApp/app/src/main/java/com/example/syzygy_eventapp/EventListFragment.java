package com.example.syzygy_eventapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EventListView - Displays a list of events for browsing.
 * Responsibilities:
 * - Display a list of event links
 * - Open EventView or EventOrganizerView when event is clicked
 * - Search and filter for events
 *
 * Collaborators: Event, EventView, EventOrganizerView
 */
public class EventListFragment extends Fragment {

    private final NavigationStackFragment navStack;
    private final EventController eventController;
    private final boolean isOrganizerView;
    private final String organizerID;

    // UI Components
    private RecyclerView eventsRecyclerView;
    private EventListAdapter adapter;
    private ProgressBar loadingSpinner;
    private View emptyStateView;

    private ListenerRegistration eventListener;
    private List<Event> allEvents;
    private List<Event> filteredEvents;

    private String currentQuery;

    /**
     * Constructor for browsing all events (entrant view)
     */
    public EventListFragment(NavigationStackFragment navStack) {
        this(navStack, false, null);
    }

    /**
     * Constructor for viewing organizer's events
     */
    public EventListFragment(NavigationStackFragment navStack, String organizerID) {
        this(navStack, true, organizerID);
    }

    /**
     * Private constructor
     */
    private EventListFragment(NavigationStackFragment navStack, boolean isOrganizerView,
                              String organizerID) {
        super();
        this.navStack = navStack;
        this.isOrganizerView = isOrganizerView;
        this.organizerID = organizerID;
        this.eventController = EventController.getInstance();
        this.allEvents = new ArrayList<>();
        this.filteredEvents = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_list, container, false);

        // Initialize UI components
        initializeViews(view);

        // Set up RecyclerView
        setupRecyclerView();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Start observing events
        loadingSpinner.setVisibility(View.VISIBLE);

        if (isOrganizerView && organizerID != null) {
            // Observe only organizer's events
            eventListener = eventController.observeOrganizerEvents(
                    organizerID,
                    this::onEventsUpdated,
                    this::onError
            );
        } else {
            // Observe all events
            eventListener = eventController.observeAllEvents(this::onEventsUpdated);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // Stop observing when fragment is not visible
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
    }

    /**
     * Initialize all view components
     */
    private void initializeViews(View view) {
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        loadingSpinner = view.findViewById(R.id.loading_spinner);
        emptyStateView = view.findViewById(R.id.empty_state_view);
    }

    /**
     * Set up the RecyclerView with adapter
     */
    private void setupRecyclerView() {
        adapter = new EventListAdapter(filteredEvents, this::onEventClicked);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        eventsRecyclerView.setAdapter(adapter);
    }

    /**
     * Called when events are updated from Firestore
     */
    private void onEventsUpdated(List<Event> events) {
        if (!isAdded()) return; // Safety check

        loadingSpinner.setVisibility(View.GONE);

        this.allEvents = events;
        filterEvents(currentQuery);
    }

    /**
     * Filter events based on search query
     */
    private void filterEvents(String query) {
        currentQuery = query;
        if (query == null || query.trim().isEmpty()) {
            // No filter, show all events
            filteredEvents.clear();
            filteredEvents.addAll(allEvents);
        } else {
            // Filter by name, description, or location
            String lowerQuery = query.toLowerCase().trim();
            filteredEvents.clear();
            filteredEvents.addAll(
                    allEvents.stream()
                            .filter(event -> matchesQuery(event, lowerQuery))
                            .collect(Collectors.toList())
            );
        }

        // Update UI
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    /**
     * Check if an event matches the search query
     */
    private boolean matchesQuery(Event event, String query) {
        // Search in name
        if (event.getName() != null && event.getName().toLowerCase().contains(query)) {
            return true;
        }

        // Search in description
        if (event.getDescription() != null && event.getDescription().toLowerCase().contains(query)) {
            return true;
        }

        // Search in location
        if (event.getLocationName() != null && event.getLocationName().toLowerCase().contains(query)) {
            return true;
        }

        return false;
    }

    /**
     * Update empty state visibility
     */
    private void updateEmptyState() {
        if (filteredEvents.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            eventsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            eventsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Called when an event is clicked
     */
    private void onEventClicked(Event event) {
        String userID = AppInstallationId.get(requireContext());

        /**
         * TODO: This should probably open the edit event view instead of
         * going to the organizer fragment
         */

//        if (isOrganizerView && event.getOrganizerID().equals(userID)) {
//            // Open EventOrganizerView if user is the organizer
//            OrganizerFragment organizerFragment = new OrganizerFragment(navStack, event.getEventID());
//            navStack.pushScreen(organizerFragment);
//        } else {
//            // Open EventView for entrants
//            EventFragment eventFragment = new EventFragment(navStack, event.getEventID());
//            navStack.pushScreen(eventFragment);
//        }

        // Open EventView for everyone, implement the TODO above in the future
        EventFragment eventFragment = new EventFragment(navStack, event.getEventID());
        navStack.pushScreen(eventFragment);
    }

    /**
     * Handle errors from Firestore
     */
    private void onError(Exception e) {
        if (isAdded()) {
            loadingSpinner.setVisibility(View.GONE);
            Toast.makeText(requireContext(),
                    "Error loading events: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * RecyclerView Adapter for displaying events
     */
    private static class EventListAdapter extends RecyclerView.Adapter<EventViewHolder> {

        private final List<Event> events;
        private final OnEventClickListener clickListener;

        interface OnEventClickListener {
            void onEventClick(Event event);
        }

        EventListAdapter(List<Event> events, OnEventClickListener clickListener) {
            this.events = events;
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_item_event, parent, false);
            return new EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
            Event event = events.get(position);
            holder.bind(event, clickListener);
        }

        @Override
        public int getItemCount() {
            return events.size();
        }
    }

    /**
     * ViewHolder for individual event items
     */
    private static class EventViewHolder extends RecyclerView.ViewHolder {

        private final TextView eventNameText;
        private final TextView eventDescriptionText;
        private final TextView locationText;
        private final TextView waitingListText;
        private final ImageView posterThumbnail;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventNameText = itemView.findViewById(R.id.event_name_text);
            eventDescriptionText = itemView.findViewById(R.id.event_description_text);
            locationText = itemView.findViewById(R.id.location_text);
            waitingListText = itemView.findViewById(R.id.waiting_list_text);
            posterThumbnail = itemView.findViewById(R.id.poster_thumbnail);
        }

        void bind(Event event, EventListAdapter.OnEventClickListener clickListener) {
            eventNameText.setText(event.getName());

            // Description (truncated)
            if (event.getDescription() != null && event.getDescription().length() > 100) {
                eventDescriptionText.setText(event.getDescription().substring(0, 100) + "...");
            } else {
                eventDescriptionText.setText(event.getDescription());
            }

            // Location
            if (event.getLocationName() != null) {
                locationText.setText(event.getLocationName());
                locationText.setVisibility(View.VISIBLE);
            } else {
                locationText.setVisibility(View.GONE);
            }

            // Waiting list count
            int waitingListSize = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
            waitingListText.setText(waitingListSize + " on waiting list");

            // Poster thumbnail
            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Picasso.get()
                        .load(event.getPosterUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .resize(100, 100)
                        .centerCrop()
                        .into(posterThumbnail);
                posterThumbnail.setVisibility(View.VISIBLE);
            } else {
                posterThumbnail.setVisibility(View.GONE);
            }

            // Click listener
            itemView.setOnClickListener(v -> clickListener.onEventClick(event));
        }
    }
}