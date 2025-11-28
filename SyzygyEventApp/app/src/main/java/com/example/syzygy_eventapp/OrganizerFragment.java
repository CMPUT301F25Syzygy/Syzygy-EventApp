package com.example.syzygy_eventapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Displays the Organize Events Fragment
 */
public class OrganizerFragment extends Fragment {
    private NavigationStackFragment navStack;
    private OrganizerEventSummaryListView organizerSummaryListViewUpcoming;
    private OrganizerEventSummaryListView organizerSummaryListViewHistory;
    private String userID;
    private ListenerRegistration eventsListener;

    // required empty constructor
    public OrganizerFragment() {
        this.navStack = null;
    }

    OrganizerFragment(NavigationStackFragment navStack) {
        this.navStack = navStack;
    }

    /**
     * Inflates the layout and intializes all UI elements.
     * Sets up the search bar, QR scan button, and attaches a real-time listener to Firestore
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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_organizer, container, false);

        // Initialize the summary lists
        organizerSummaryListViewUpcoming = view.findViewById(R.id.upcoming_event_list);
        organizerSummaryListViewUpcoming.setTitle("Upcoming Events");
        organizerSummaryListViewHistory = view.findViewById(R.id.history_event_list);
        organizerSummaryListViewHistory.setTitle("Past Events");

        Button createEventButton = view.findViewById(R.id.create_event_button);

        // Load current user ID and event controller
        userID = AppInstallationId.get(requireContext());

        createEventButton.setOnClickListener(v -> {
            UserController.getInstance().getUser(userID)
                    .addOnSuccessListener(user -> {
                       navStack.pushScreen(
                               new OrganizerEventEditDetailsFragment(userID, navStack));
                    })
                    .addOnFailureListener(e -> {
                        Log.e("OrganizerFragment", "Failed to get user", e);
                    });
        });

        startEventObserver();

        return view;
    }

    private void startEventObserver() {
        EventController.getInstance().observeAllEvents(events -> {
            List<Event> upcoming = new ArrayList<>();
            List<Event> past = new ArrayList<>();
            Date now = new Date();

            // Filter events for the current organizer
            for (Event event : events) {

                if (!event.getOrganizerID().equals(userID)) {
                    continue;
                 }

                // An event will be considered as "past" if the lottery is complete OR if the registration end time is before now
                boolean isPast = event.isLotteryComplete() || (event.getRegistrationEnd() != null && event.getRegistrationEnd().toDate().before(now));

                if (isPast) {
                    past.add(event);
                } else {
                    upcoming.add(event);
                }
            }

            // Populate the upcoming events list.
            organizerSummaryListViewUpcoming.setItems(upcoming, false, this::eventClickedCallback);

            // Populate the past event list.
            organizerSummaryListViewHistory.setItems(past, false, this::eventClickedCallback);
        });
    }

    private void eventClickedCallback(View view) {
        Event event = (Event) view.getTag();
        navStack.pushScreen(new EventOrganizerDetailsView(event, navStack));
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