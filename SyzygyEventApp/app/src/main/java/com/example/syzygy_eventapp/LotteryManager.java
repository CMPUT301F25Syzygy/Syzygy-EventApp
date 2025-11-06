package com.example.syzygy_eventapp;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles running event lotteries once an event's registration period closes
 * Responsibilities:
 * -> Check Firestore for events whose registration period has ended
 * -> Randomly select winners from the waiting list
 * -> Re-run the lottery if someone declines their invite
 * -> Mark events as LotteryComplete in Firestore once all spots are filled
 */
public class LotteryManager {
    private final EventController eventController;
    private final InvitationController invitationController;
    private final FirebaseFirestore db;

    public LotteryManager() {
        this.eventController = new EventController();
        this.invitationController = new InvitationController();
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Checks if a given event's lottery should run
     * @param event The event to check and process
     */
    public void processEventLottery(Event event) {
       // Timestamps, waiting list, and max attendees
        Timestamp now = Timestamp.now();
        Timestamp end = event.getRegistrationEnd();
        List<String> waitingList = event.getWaitingList();
        Integer maxAttendees = event.getMaxAttendees();

        // A bunch of safety tests to see if we can process the lottery or not //

        // Null event, can't draw
        if (event == null) {
            return;
        }
        // Check if lottery was already done
        if (event.isLotteryComplete()) {
            Log.d("LotteryManager", "Lottery already complete for event: " + event.getEventID());
            return;
        }
        // Too early for lottery (registration period is ongoing), can't draw
        if (end == null || now.compareTo(end) < 0) {
            Log.d("LotteryManager", "Registration still open for event: " + event.getEventID());
            return;
        }
        // Empty waiting list, can't draw
        if (waitingList == null || waitingList.isEmpty()) {
            Log.d("LotteryManager", "No entrants to process for event: " + event.getEventID());
            return;
        }
        // no maxAttendees set, can't draw
        if (maxAttendees == null || maxAttendees <= 0) {
            Log.d("LotteryManager", "Event has no attendee limit set, skipping lottery.");
            return;
        }

        // randomize waiting list using a new ArrayList and Collections
        List<String> shuffled = new ArrayList<>(waitingList);
        Collections.shuffle(shuffled);

        // select winners by making a sublist of the shuffled list from 0 to i, where i is the maxAttendees
        List<String> selected = shuffled.subList(0, Math.min(maxAttendees, shuffled.size()));
    }
}
