package com.example.syzygy_eventapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Shows the events the user has joined:
 *    - Upcoming events that the user is in the waiting list for, which are still open or awaiting lottery
 *    - History of events the user has interacted with or participated in
 * <p>
 *    Uses EventSummaryListView for both lists, and real-time listeners keep them updated whenever
 *    event docs change on Firestore
 * </p>
 */
public class JoinedEventsFragment extends Fragment {

    private EventSummaryListView upcomingListView;
    private EventSummaryListView historyListView;

    private EventController eventController;
    private ListenerRegistration allEventsListener;

    private String userID;
    private NavigationStackFragment navStack;

    // required empty constructor
    public JoinedEventsFragment() {
        this.navStack = null;
    }

    public JoinedEventsFragment(NavigationStackFragment navStack) {
        this.navStack = navStack;
    }

    /**
     *  Inflates the layout, and initializes the UI components.
     *  Sets up the navigation reference, initializes the list views, loads current userID, and starts Firestore observation
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate this fragment's layout
        View view = inflater.inflate(R.layout.fragment_joined_events, container, false);

        // Initialize the two even list views
        upcomingListView = view.findViewById(R.id.upcoming_event_list);
        historyListView = view.findViewById(R.id.history_event_list);

        // Set titles for each section
        upcomingListView.setTitle("Upcoming Events");
        historyListView.setTitle("Past Events");

        // Load current user ID and event controller
        userID = AppInstallationId.get(requireContext());
        eventController = new EventController();

        // Start listening for event updates
        startObserver();
        return view;
    }

    /**
     * Starts the Firestore real-time listener that monitors all events
     *<p>
     * When events update, the callback will:
     *    - Filter events to show only the ones that the user is associated with
     *    - Splits the events into "upcoming" and "past"
     *    - Updates the list views, including click behavior, for upcoming events
     * </p>
     * ListenerRegistration is stored so it can be removed in onDestroyView
     */
    private void startObserver() {
        allEventsListener = eventController.observeAllEvents(events -> {

            List<Event> upcoming = new ArrayList<>();
            List<Event> past = new ArrayList<>();
            Date now = new Date();

            // Filter events for the current user
            for (Event event : events) {

                // Skip events that the user is NOT in the waiting list for
                if (event.getWaitingList() == null || !event.getWaitingList().contains(userID)) {
                    continue;
                }

                // An event will be considered as "past" if the lottery is complete OR if the registration end time is before now
                boolean isPast = event.isLotteryComplete() || (event.getRegistrationEnd() != null && event.getRegistrationEnd().toDate().before(now));

                if (isPast) {
                    past.add(event);
                }
                else {
                    upcoming.add(event);
                }
            }

            // Populate the upcoming events list. Upcoming events should be clickable and take the user to the event details
            upcomingListView.setItems(upcoming, false, v -> {
                Event clicked = (Event) v.getTag();
                navStack.pushScreen(new EventFragment(navStack, clicked.getEventID()));
            });

            // Populate the past event list. I don't think past events need to be clickable, but this part can be modified if needed
            historyListView.setItems(past, false, v -> {});
        }, error -> {

        });
    }

    /**
     * Cleans up the real-time listeners when this fragment's view is destroyed to prevent memory leaks and stale listeners when switching screens.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (allEventsListener != null) {
            allEventsListener.remove();
            allEventsListener = null;
        }
    }
}