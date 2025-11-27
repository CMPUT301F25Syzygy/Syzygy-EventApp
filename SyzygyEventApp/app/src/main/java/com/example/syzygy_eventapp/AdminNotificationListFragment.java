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

import com.google.firebase.firestore.ListenerRegistration;


/**
 * Fragment displaying a list of notifications for the administrator to manage.
 */
public class AdminNotificationListFragment extends Fragment {

    /// Firestore listener for user data
    private ListenerRegistration userListener;
    /// Navigation stack fragment
    private NavigationStackFragment navStack;

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
}
