package com.example.syzygy_eventapp;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
public class FindEventsFragment extends Fragment {
    private NavigationStackFragment navStack;
    private QRScanFragment qrFragment;
    private EventSummaryListView summaryListView;
    private List<Event> joinableEvents = new ArrayList<>();
    private ListenerRegistration eventsListener;
    private EventFilters currentFilters = new EventFilters();

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
        // Open the filter dialog when it's clicked
        filterButton.setOnClickListener(v -> openFilter());

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

        String currentUserID = AppInstallationId.get(requireContext());

        EventController.getInstance().observeAllEvents((events) -> {
            joinableEvents.clear();

            for (Event event : events) {
                boolean isOwnEvent = currentUserID.equals(event.getOrganizerID());

                if (event.isOpen() && !isOwnEvent) {
                    joinableEvents.add(event);
                }
            }

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

        for (Event e : joinableEvents) {
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
     * A basic filter holder to let us know what filters to apply
     * <p>
     *     0 = none
     *     1 = popularity
     *     2 = soonest
     * </p>
     */
    private static class EventFilters {
        int sortType = 0;
    }

    /**
     * A dialog to open the filter chooser
     */
    private void openFilter() {

        // Inflate the layout and set up buttons + check boxes
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.filter_dialog, null);
        RadioGroup sortGroup = dialogView.findViewById(R.id.sortRadioGroup);

        // Set the currently selected option
        if (currentFilters.sortType == 1) {
            sortGroup.check(R.id.popularityRadio);
        } else if (currentFilters.sortType == 2) {
            sortGroup.check(R.id.soonestRadio);
        } else {
            sortGroup.check(R.id.noSortRadio);
        }

        // Make an alert dialog for choosing the filter(s)
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Filter Events")
                .setView(dialogView)
                .setPositiveButton("Apply", (d, w) -> {
                    int checkedId = sortGroup.getCheckedRadioButtonId();
                    if (checkedId == R.id.popularityRadio) {
                        currentFilters.sortType = 1;
                    } else if (checkedId == R.id.soonestRadio) {
                        currentFilters.sortType = 2;
                    } else {
                        currentFilters.sortType = 0;
                    }
                    applyFullFilter();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Applies all active filters (search text + sorting option) to the list of open events, and
     * updates the {@link EventSummaryListView} with the resulting list.
     */
    private void applyFullFilter() {
        // Start with all events
        List<Event> filtered = new ArrayList<>(joinableEvents);

        // Apply search text filter if there's text in the search box
        EditText searchBox = getView() != null ? getView().findViewById(R.id.searchEvents) : null;
        if (searchBox != null) {
            String query = searchBox.getText().toString().toLowerCase().trim();
            if (!query.isEmpty()) {
                filtered.removeIf(e -> {
                    boolean matchesName = e.getName() != null && e.getName().toLowerCase().contains(query);
                    boolean matchesDesc = e.getDescription() != null && e.getDescription().toLowerCase().contains(query);
                    boolean matchesLocation = e.getLocationName() != null && e.getLocationName().toLowerCase().contains(query);
                    return !(matchesName || matchesDesc || matchesLocation);
                });
            }
        }

        // Apply sorting based on selected type
        if (currentFilters.sortType == 1) {
            // Sort by popularity (most entrants first)
            filtered.sort((a, b) -> {
                int sizeA = a.getWaitingList() != null ? a.getWaitingList().size() : 0;
                int sizeB = b.getWaitingList() != null ? b.getWaitingList().size() : 0;
                return Integer.compare(sizeB, sizeA);
            });
        }

        else if (currentFilters.sortType == 2) {
            // Sort by soonest registration end date (closing soonest first)
            filtered.sort((a, b) -> {
                Date dateA = a.getRegistrationEnd() != null ? a.getRegistrationEnd().toDate() : new Date(Long.MAX_VALUE);
                Date dateB = b.getRegistrationEnd() != null ? b.getRegistrationEnd().toDate() : new Date(Long.MAX_VALUE);
                return dateA.compareTo(dateB);
            });
        }

        // Update the list view with the filtered results
        summaryListView.setItems(filtered, false, v -> {
            Event clicked = (Event) v.getTag();
            navStack.pushScreen(new EventFragment(navStack, clicked.getEventID()));
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