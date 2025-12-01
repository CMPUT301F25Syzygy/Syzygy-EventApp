package com.example.syzygy_eventapp;

import android.Manifest;
import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Fragment displaying a list of events for the administrator to manage.
 */
public class AdminEventListFragment extends Fragment {

    /// Firestore listener for event data
    private ListenerRegistration eventsListener;

    /// Navigation stack fragment
    private NavigationStackFragment navStack;

    /// Event list views
    private EventSummaryListView upcomingEventList;
    private EventSummaryListView pastEventList;

    /// Required empty constructor
    public AdminEventListFragment() {
        this.navStack = null;
    }

    /**
     * Constructor with navigation stack
     */
    AdminEventListFragment(NavigationStackFragment navStack) {
        this.navStack = navStack;
    }

    /**
     * Inflates the fragment layout and binds views.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_admin_event_list, container, false);

        this.upcomingEventList = root.findViewById(R.id.upcoming_event_list);
        this.pastEventList = root.findViewById(R.id.past_event_list);

        this.upcomingEventList.setTitle("Upcoming Events");
        this.pastEventList.setTitle("Past Events");

        return root;
    }

    /**
     * Sets up the back button menu when the fragment starts
     */
    @Override
    public void onStart() {
        super.onStart();

        // Back button menu
        navStack.setScreenNavMenu(R.menu.back_nav_menu, (i) -> {
            navStack.popScreen();
            return true;
        });

        startEventObserver();
    }

    /**
     * Starts observing all events from the EventController and updates the UI accordingly.
     */
    private void startEventObserver() {
        EventController.getInstance().observeAllEvents(events -> {
            List<Event> upcoming = new ArrayList<>();
            List<Event> past = new ArrayList<>();
            Date now = new Date();

            // Split events into past and upcoming
            for (Event event : events) {

                // An event will be considered as "past" if the lottery is complete OR if the registration end time is before now
                boolean isPast = event.isLotteryComplete() || (event.getRegistrationEnd() != null && event.getRegistrationEnd().toDate().before(now));

                if (isPast) {
                    past.add(event);
                } else {
                    upcoming.add(event);
                }
            }

            // Populate the upcoming events list.
            upcomingEventList.setItems(
                    upcoming,
                    true,
                    this::eventClickedCallback,
                    this::eventRemoveBannerButtonClickedCallback,
                    this::eventDeleteButtonClickedCallback
            );

            // Populate the past event list.
            pastEventList.setItems(
                    past,
                    true,
                    this::eventClickedCallback,
                    this::eventRemoveBannerButtonClickedCallback,
                    this::eventDeleteButtonClickedCallback
            );
        });
    }

    /**
     * Callback when an event is clicked. Navigates to the EventFragment for the selected event.
     */
    private void eventClickedCallback(View view) {
        Event event = (Event) view.getTag();
        navStack.pushScreen(new EventFragment(navStack, event.getEventID()));
    }

    /**
     * Callback when the delete event button is clicked. Prompts for confirmation before deleting the event.
     */
    private void eventDeleteButtonClickedCallback(View view) {
        Event event = (Event) view.getTag();

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // User confirmed
                    EventController.getInstance().deleteEvent(event.getEventID());
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled (do nothing)
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Callback when the remove banner button is clicked. Prompts for confirmation before removing the event poster image.
     */
    private void eventRemoveBannerButtonClickedCallback(View view) {
        Event event = (Event) view.getTag();

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Event Poster Image")
                .setMessage("Are you sure you want to remove this event's poster image?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    // User confirmed
                    event.setPosterUrl(null);
                    EventController.getInstance().updateEvent(event);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled (do nothing)
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Cleans up the Firestore listener to avoid leaks and duplicate listeners
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }
    }
}
