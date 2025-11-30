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

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment displaying a list of user profiles for the administrator to manage.
 */
public class AdminImageListFragment extends Fragment {

    private String currentAdminID;

    /// User list view
    private ImageListView userListView;
    /// Firestore listener for user data
    private ListenerRegistration userListener;
    /// Navigation stack fragment
    private NavigationStackFragment navStack;

    List<User> users = new ArrayList<>();
    List<Event> events = new ArrayList<>();


    /// Required empty constructor
    public AdminImageListFragment() {
        this.navStack = null;
    }

    /**
     * Constructor with navigation stack
     */
    AdminImageListFragment(NavigationStackFragment navStack) {
        this.navStack = navStack;
    }

    /**
     * Inflates the fragment layout and binds views.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_admin_image_list, container, false);

        // Bind list view
        userListView = root.findViewById(R.id.image_list_view);

        // Click listener for each user
//        userListView.setOnUserClickListener(this::showUserActionDialog);

        // Start listening to Firestore
        setupUserObservers();

        // Get current admin's user ID
        currentAdminID = AppInstallationId.get(requireContext());

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

    private void setupUserObservers() {
        userListener = UserController.getInstance()
                .observeAllUsers(this::onUsersChanged);
    }

    private void onUsersChanged(List<User> users) {
        // Filter out the current admin
        List<User> filteredUsers = new ArrayList<>();
        for (User user : users) {
//            if (!user.getUserID().equals(currentAdminID)) {
//                filteredUsers.add(user);
//            }

            filteredUsers.add(user);
        }

        userListView.setUsers(filteredUsers);
    }

    /**
     * Cleans up Firestore listeners
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }
}
