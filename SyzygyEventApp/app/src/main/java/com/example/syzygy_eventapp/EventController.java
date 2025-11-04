package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Controller for reading/writing {@link Event} data in Firestore DB.
 * EventViews will call this class to create, update, and observe events.
 * Firestore is the source of truth; views have real-time listeners.
 */
public class EventController {

}
