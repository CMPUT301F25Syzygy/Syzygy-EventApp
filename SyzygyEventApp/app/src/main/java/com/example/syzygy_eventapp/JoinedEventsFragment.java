package com.example.syzygy_eventapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

    private InvitationController invitationController;
    private ListenerRegistration invitesListener;

    private List<Event> lastEvents = new ArrayList<>();
    private List<Invitation> userInvites = new ArrayList<>();

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
        eventController = EventController.getInstance();
        invitationController = new InvitationController();

        // Start listening for event updates
        startObservers();
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
    private void startObservers() {
        allEventsListener = eventController.observeAllEvents(events -> {
            lastEvents = events;
            recomputeLists();
        });

        invitesListener = invitationController.observeInvites(
                com.google.firebase.firestore.Filter.equalTo("recipientID", userID),
                invites -> {
                    userInvites = invites != null ? invites : new ArrayList<>();
                    recomputeLists();
                }
        );
    }

    private void recomputeLists() {
        if (lastEvents == null || upcomingListView == null || historyListView == null) {
            return;
        }

        View root = getView();
        if (root == null) {
            return;
        }

        List<Event> upcoming = new ArrayList<>();
        List<Event> past = new ArrayList<>();
        Date now = new Date();

        java.util.HashSet<String> userEventIds = new java.util.HashSet<>();

        for (Event event : lastEvents) {
            List<String> waiting = event.getWaitingList();
            if (waiting != null && waiting.contains(userID)) {
                userEventIds.add(event.getEventID());
            }
        }

        for (Invitation inv : userInvites) {
            if (inv == null) {
                continue;
            }

            Boolean cancelled = inv.getCancelled();
            if (Boolean.TRUE.equals(cancelled)) {
                continue;
            }

            String eventId = inv.getEvent();
            if (eventId != null) {
                userEventIds.add(eventId);
            }
        }

        for (Event event : lastEvents) {
            if (!userEventIds.contains(event.getEventID())) {
                continue;
            }

            Date eventDate = null;

            if (event.getRegistrationEnd() != null) {
                eventDate = event.getRegistrationEnd().toDate();
            } else if (event.getEventTime() != null) {
                eventDate = event.getEventTime().toDate();
            }

            boolean isPast = eventDate != null && eventDate.before(now);

            if (isPast) {
                past.add(event);
            } else {
                upcoming.add(event);
            }
        }

        TextView countText = root.findViewById(R.id.event_count_text);
        int totalEvents = upcoming.size() + past.size();
        if (totalEvents == 1) {
            countText.setText("You're registered for 1 event");
        } else {
            countText.setText("You're registered for " + totalEvents + " events");
        }

        TextView upcomingEmpty = root.findViewById(R.id.upcoming_empty_message);
        TextView historyEmpty = root.findViewById(R.id.history_empty_message);

        upcomingEmpty.setVisibility(upcoming.isEmpty() ? View.VISIBLE : View.GONE);
        historyEmpty.setVisibility(past.isEmpty() ? View.VISIBLE : View.GONE);

        upcomingListView.setItems(upcoming, false, v -> {
            Event clicked = (Event) v.getTag();
            navStack.pushScreen(new EventFragment(navStack, clicked.getEventID()));
        });

        historyListView.setItems(past, false, v -> {});
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
        if (invitesListener != null) {
            invitesListener.remove();
            invitesListener = null;
        }
    }
}