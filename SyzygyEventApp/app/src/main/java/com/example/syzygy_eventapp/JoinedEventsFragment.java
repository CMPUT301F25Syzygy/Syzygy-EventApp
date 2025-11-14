package com.example.syzygy_eventapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.ListenerRegistration;

/**
 * Shows the events the user has joined:
 *    - Upcoming events that the user is in the waiting list for, which are still open or awaiting lottery
 *    - History of events the user has interacted with or participated in
 *
 *    Uses EventSummaryListView for both lists
 */
public class JoinedEventsFragment extends Fragment {

    private EventSummaryListView upcomingListView;
    private EventSummaryListView historyListView;

    private EventController eventController;
    private ListenerRegistration allEventsListener;

    private String userID;
    private NavigationStackFragment navStack;

    // default constructor, which is required
    public JoinedEventsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // inflate this fragment's layout
        View view = inflater.inflate(R.layout.fragment_joined_events, container, false);

        // get a reference to the NavigationStack (parent fragment)
        navStack = (NavigationStackFragment) getParentFragment();

        // Initialize the two even list views
        upcomingListView = view.findViewById(R.id.upcoming_event_list);
        historyListView = view.findViewById(R.id.history_event_list);

        // set titles for each section
        upcomingListView.setTitle("Upcoming Events");
        historyListView.setTitle("Past Events");

        // load current user ID and event controller
        userID = AppInstallationId.get(requireContext());
        eventController = new EventController();

        // start listening for event updates
        startObserver();
        return view;
    }

    private void startObserver() {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (allEventsListener != null) {
            allEventsListener.remove();
            allEventsListener = null;
        }
    }
}