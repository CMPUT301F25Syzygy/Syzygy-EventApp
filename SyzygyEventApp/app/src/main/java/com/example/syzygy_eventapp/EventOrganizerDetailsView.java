package com.example.syzygy_eventapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Fragment class that allows an organizer to create or edit an event's details.
 * Provides functionality for managing event info, including dates, times, posters, and for viewing waiting lists or invitation statuses in real-time
 */
public class EventOrganizerDetailsView extends Fragment {
    // declare UI components
    private TextView eventNameText, eventDescriptionText;
    private TextView locationText, eventTimeText, eventEntrantsText;
    private Button openMapButton;
    private UserListView acceptedListView, pendingListView, waitingListView;
    private ImageView posterImage;
    private Button cancelInvitesButton, setInvitesButton, sendNotificationButton;

    // Controllers for Firebase operations
    private EventController eventController;
    private InvitationController invitationController;

    // Current user and event data
    private Event event;
    private NavigationStackFragment navStack;

    private static final String TAG = "OrganizerEventEdit";

    /**
     * A new instance of this fragment in VIEW-ONLY mode.
     * To be used so the organizer can retain the ability to view old events and their entrant info, without being able to edit it.
     *
     * @param event The event to view organizer details for
     * @param navStack      The nav stack for screen management
     * @return A new instance of OrganizerEventEditDetailsFragment
     */
    public EventOrganizerDetailsView(@Nullable Event event, @Nullable NavigationStackFragment navStack) {
        this.event = event;
        this.navStack = navStack;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: load invites
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_organizer_event_details, container, false);

        // Initialize views
        eventNameText = view.findViewById(R.id.event_title);
        eventDescriptionText = view.findViewById(R.id.event_description);

        locationText = view.findViewById(R.id.event_location);
        eventTimeText = view.findViewById(R.id.event_time);
        eventEntrantsText = view.findViewById(R.id.event_entrants);

        posterImage = view.findViewById(R.id.event_banner);

        acceptedListView = view.findViewById(R.id.accepted_list_view);
        acceptedListView.setTitle("Accepted");
        acceptedListView.setListVisibility(false);

        pendingListView = view.findViewById(R.id.pending_list_view);
        acceptedListView.setTitle("Pending");
        pendingListView.setListVisibility(false);

        waitingListView = view.findViewById(R.id.waiting_list_view);
        acceptedListView.setTitle("Waiting");
        waitingListView.setListVisibility(false);

        openMapButton = view.findViewById(R.id.open_map_button);
        cancelInvitesButton = view.findViewById(R.id.cancel_invites_button);
        setInvitesButton = view.findViewById(R.id.send_invites_button);
        sendNotificationButton = view.findViewById(R.id.send_notification_button);

        // Initialize controllers for Firebase operations
        eventController = EventController.getInstance();
        invitationController = new InvitationController();

        setupListeners();
        setupNavBar();

        return view;
    }

    /**
     * Sets up click listeners for all interactive UI elements
     */
    private void setupListeners() {
        // View map button
        openMapButton.setOnClickListener(v -> {
            // TODO
        });

        cancelInvitesButton.setOnClickListener(v -> {
            // TODO
        });

        setInvitesButton.setOnClickListener(v -> {
            // TODO
        });

        sendNotificationButton.setOnClickListener(v -> {
            // TODO
        });
    }

    /**
     * Sets up nav bar buttons and listener
     */
    private void setupNavBar() {
        navStack.setScreenNavMenu(R.menu.organizer_event_details, (MenuItem item) -> {
            if ( item.getItemId() == R.id.back_nav_button) {
                navStack.popScreen();
            } else if ( item.getItemId() == R.id.generate_qr_nav_button) {
                QRGenerateFragment qrFragment = new QRGenerateFragment(event, navStack);
                navStack.pushScreen(qrFragment);
            } else if ( item.getItemId() == R.id.preview_nav_button) {
                EventFragment entrantEventFragment = new EventFragment(navStack, event.getEventID());
                navStack.pushScreen(entrantEventFragment);
            } else if ( item.getItemId() == R.id.edit_nav_button) {
                // TODO
            }
            return true;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // TODO clean up listeners to prevent memory leaks
    }
}