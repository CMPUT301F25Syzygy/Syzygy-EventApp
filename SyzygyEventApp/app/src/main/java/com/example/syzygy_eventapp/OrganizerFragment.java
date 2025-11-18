//package com.example.syzygy_eventapp;
//
//import android.os.Bundle;
//
//import androidx.fragment.app.Fragment;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//
///**
// * A empty placeholder for OrganizerFragment
// */
//public class OrganizerFragment extends Fragment {
//
//    // Creating a custom constructor so that EventListFragment can pass params
//    private final NavigationStackFragment navStack;
//    private final String eventID;
//
//    // Custom constructor to match how it's called
//    public OrganizerFragment(NavigationStackFragment navStack, String eventID) {
//        this.navStack = navStack;
//        this.eventID = eventID;
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_organizer, container, false);
//    }
//}



package com.example.syzygy_eventapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Displays the Find Events screen.
 * <p>
 *     Provides:
 *     <ul>
 *         <li>Search bar that filters open events by soonest (registration date closest to farthest) or popularity. </li>
 *         <li>A button to open the QR code scanner</li>
 *         <li>An {@link EventSummaryListView} that displays all currently open events</li>
 * </ul>
 * </p>
 *
 * Only events that meet the following criteria are shown:
 * <ul>
 *     <li>Are not past their registration deadline</li>
 *     <li>Have not completed their lottery</li>
 * </ul>
 *
 * Clicking an event opens the Event Details page using the {@link NavigationStackFragment}
 */
public class OrganizerFragment extends Fragment {
    private NavigationStackFragment navStack;
    private OrganizerEventSummaryListView organizerSummaryListViewUpcoming;
    private OrganizerEventSummaryListView organizerSummaryListViewHistory;
    private List<Event> allEvents = new ArrayList<>();
    private ListenerRegistration eventsListener;

    // TODO: Add this when EditEventFragment exists
    // private EditEventFragment editEventFragment;

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
        organizerSummaryListViewHistory = view.findViewById(R.id.history_event_list);


        Button qrButton = view.findViewById(R.id.create_event_button);

        // TODO: Remove this and add EditEventFragment
        // Open the QR scanner when the button is clicked
//        view.findViewById(R.id.open_qr_scan_button).setOnClickListener((v) -> {
//            navStack.pushScreen(qrFragment);
//        });


        // SEED FAKE EVENTS, ALSO FOR TESTING/DEMO PLEASE IGNORE
        // seedFakeEventsOnce();

        // Load events from Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        eventsListener = db.collection("events").addSnapshotListener((snapshots, e) -> {
            if (e != null) return;

            List<Event> events = new ArrayList<>();
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Event event = doc.toObject(Event.class);
                    if (event != null) {
                        event.setEventID(doc.getId());

                        // only include events that are still open
                        boolean isOpen = !event.isLotteryComplete() && event.getRegistrationEnd() != null && event.getRegistrationEnd().toDate().after(new Date());
                        if (isOpen) {
                            events.add(event);
                        }
                    }
                }
            }

            // store all events
            allEvents = events;

        });

        return view;
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