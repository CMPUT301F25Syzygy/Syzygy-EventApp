package com.example.syzygy_eventapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.Timestamp;
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
 *         <li>Search bar that filters open events by [TODO] </li>
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
public class FindEventsFragment extends Fragment {
    private NavigationStackFragment navStack;
    private QRScanFragment qrFragment;
    private EventSummaryListView summaryListView;
    private List<Event> allEvents = new ArrayList<>();
    private ListenerRegistration eventsListener;

    // required empty constructor
    public FindEventsFragment() {
        this.navStack = null;
    }

    FindEventsFragment(NavigationStackFragment navStack) {
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
        View view = inflater.inflate(R.layout.fragment_find_events, container, false);

        // Initialize the summary list
        summaryListView = view.findViewById(R.id.event_summary_list);

        qrFragment = new QRScanFragment(navStack);

        EditText searchBox = view.findViewById(R.id.searchEvents);
        Button filterButton = view.findViewById(R.id.filterButton);
        Button qrButton = view.findViewById(R.id.open_qr_scan_button);

        // Open the QR scanner when the button is clicked
        view.findViewById(R.id.open_qr_scan_button).setOnClickListener((v) -> {
            navStack.pushScreen(qrFragment);
        });

        // SEED FAKE EVENTS, ALSO FOR TESTING/DEMO PLEASE IGNORE
        // seedFakeEventsOnce();

        // Set up the text watcher to update the search results as it changes
        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchFilter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

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

            // store all events, and call the helper applySearchFilter to show events based on the search text
            allEvents = events;
            applySearchFilter(searchBox.getText().toString());

        });

        return view;
    }

    /**
     * Filters the list of all events based on a user-provided search query and updates the {@link EventSummaryListView} to display only the matching results.
     * <p>
     *     The search is case-insensitive and matches against the event's name, description, and location name (if available).
     * </p>
     *
     * @param query The search text entered by the user. If empty, all events are displayed.
     */
    private void applySearchFilter(String query) {
        if (summaryListView == null) return;

        List<Event> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (Event e : allEvents) {
            if (e.getName().toLowerCase().contains(lowerQuery) ||
                    (e.getDescription() != null && e.getDescription().toLowerCase().contains(lowerQuery)) ||
                    (e.getLocationName() != null && e.getLocationName().toLowerCase().contains(lowerQuery))) {
                filtered.add(e);
            }
        }

        summaryListView.setTitle("Available Events");
        summaryListView.setItems(filtered, false, v -> {
            Event clickedEvent = (Event) v.getTag();
            navStack.pushScreen(new EventFragment(navStack, clickedEvent.getEventID()));
        });
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