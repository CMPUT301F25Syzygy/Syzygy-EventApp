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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Displays the Find Events screen:
 * - Lets the user search for events
 * - Opens the QR scanner
 * - Embeds EventListFragment to display event results
 */
public class FindEventsFragment extends Fragment {
    final private NavigationStackFragment navStack;
    private QRScanFragment qrFragment;
    private EventSummaryListView summaryListView;

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
        seedFakeEventsOnce();

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
                        events.add(event);
                    }
                }
            }

            // Populate EventSummaryListView
            summaryListView.setTitle("Available Events");
            summaryListView.setItems(events, false, v -> {
                Event clickedEvent = (Event) v.getTag();
                navStack.pushScreen(new EventFragment(navStack, clickedEvent.getEventID()));
            });
        });

        return view;
    }

    // ------------------- IGNORE THESE FUNCTIONS, THEY'RE ONLY FOR TESTING/DEMO --------------------
    private void seedFakeEvents() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<Event> fakeEvents = new ArrayList<>();

        Event e1 = new Event();
        e1.setEventID("FAKE_EVENT_1");
        e1.setName("Fake Event 1");
        e1.setDescription("This is a test event for demo purposes.");
        e1.setLocationName("Heaven");
        e1.setRegistrationEnd(new Timestamp(new Date(System.currentTimeMillis() + 86400000))); // tomorrow
        e1.setOrganizerID("FAKE_ORG_1");
        fakeEvents.add(e1);

        Event e2 = new Event();
        e2.setEventID("FAKE_EVENT_2");
        e2.setName("Fake Event 2");
        e2.setDescription("Another test event for demo purposes.");
        e2.setLocationName("Edmonton");
        e2.setRegistrationEnd(new Timestamp(new Date(System.currentTimeMillis() + 172800000))); // in 2 days
        e2.setOrganizerID("FAKE_ORG_2");
        fakeEvents.add(e2);

        for (Event e : fakeEvents) {
            db.collection("events")
                    .document(e.getEventID())
                    .set(e)
                    .addOnSuccessListener(aVoid -> Log.d("FirestoreSeed", "Event added: " + e.getName()))
                    .addOnFailureListener(ex -> Log.e("FirestoreSeed", "Failed to add event: " + e.getName(), ex));
        }

        Toast.makeText(getContext(), "Fake events seeded", Toast.LENGTH_SHORT).show();
    }

    private void seedFakeEventsOnce() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events").document("FAKE_EVENT_1").get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        seedFakeEvents(); // Only add them if they don’t exist
                    }
                });
    }

}