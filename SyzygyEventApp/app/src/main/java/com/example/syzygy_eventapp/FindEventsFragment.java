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
 * Displays the Find Events screen:
 * - Lets the user search for events
 * - Opens the QR scanner
 * - Uses the EventSummaryListView to display events
 */
public class FindEventsFragment extends Fragment {
    final private NavigationStackFragment navStack;
    private QRScanFragment qrFragment;
    private EventSummaryListView summaryListView;
    private List<Event> allEvents = new ArrayList<>();
    private ListenerRegistration eventsListener;

    FindEventsFragment(NavigationStackFragment navStack) {
        super();
        this.navStack = navStack;
    }

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

        view.findViewById(R.id.open_qr_scan_button).setOnClickListener((v) -> {
            navStack.pushScreen(qrFragment);
        });

        // SEED FAKE EVENTS, ALSO FOR TESTING/DEMO PLEASE IGNORE
        // seedFakeEventsOnce();

        // Set up the text watcher
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
        db.collection("events").addSnapshotListener((snapshots, e) -> {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }
    }

}