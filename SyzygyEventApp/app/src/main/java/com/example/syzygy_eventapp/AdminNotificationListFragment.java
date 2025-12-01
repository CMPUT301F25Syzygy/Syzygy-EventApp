package com.example.syzygy_eventapp;

import android.Manifest;
import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Fragment displaying a list of notifications for the administrator to manage.
 */
public class AdminNotificationListFragment extends Fragment {

    private NotificationListView notificationListView;
    private ListenerRegistration notificationListener;


    /// Navigation stack fragment
    private final NavigationStackFragment navStack;

    /// Required empty constructor
    public AdminNotificationListFragment() {
        this.navStack = null;
    }

    /**
     * Constructor with navigation stack
     */
    AdminNotificationListFragment(NavigationStackFragment navStack) {
        this.navStack = navStack;
    }

    /**
     * Inflates the fragment layout and binds views.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_admin_notification_list, container, false);

        // Bind list view
        notificationListView = root.findViewById(R.id.notification_list_view);
        notificationListView.setOnListItemClickListener(this::showActionDialog);

        // Start listening to Firestore
        setupNotificationListener();

        return root;
    }

    /**
     * Sets up the back button menu when the fragment starts
     */
    @Override
    public void onStart() {
        super.onStart();

        // Back button menu
        navStack.setScreenNavMenu(R.menu.back_nav_menu, (i) -> {
            navStack.popScreen();
            return true;
        });
    }

    /**
     * Sets up Firestore listener for notifications
     */
    private void setupNotificationListener() {
        Filter filter = Filter.notEqualTo("deleted", true);
        notificationListener = NotificationController.getInstance()
                .observeNotifications(filter, this::onNotificationsChanged);
    }


    /**
     * Shows a dialog with actions for the selected image
     */
    private void showActionDialog(Notification notif) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete notification?")
                .setMessage("This will permanently delete the notification.")
                .setPositiveButton("Delete", (dialog, which) -> {
                   NotificationController.getInstance().deleteNotification(notif.getId());
                })
                .setNegativeButton("Cancel", null)
                .show();

    }


    /**
     * Handles changes to the notification list
     */
    private void onNotificationsChanged(List<Notification> notifications) {

        // List<Notification> mockNotifications = new ArrayList<>();

        // mockNotifications.add(new Notification(
        //         "Event Approved",
        //         "Your upcoming event has been approved!",
        //         null,                       // mock Event
        //         null,                       // mock Organizer
        //         new ArrayList<>()           // empty recipients
        // ));

        // mockNotifications.add(new Notification(
        //         "Event Reminder",
        //         "Your event starts in 2 hours.",
        //         null,
        //         null,
        //         new ArrayList<>()
        // ));

        // mockNotifications.add(new Notification(
        //         "Profile Notice",
        //         "Your profile has been updated successfully.",
        //         null,
        //         null,
        //         new ArrayList<>()
        // ));

        notificationListView.setNotifications(notifications);
    }

    /**
     * Cleans up Firestore listeners
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }
}
