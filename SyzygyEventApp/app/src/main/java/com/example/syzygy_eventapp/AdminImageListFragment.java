package com.example.syzygy_eventapp;

import android.Manifest;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment displaying a list of images in the app for the administrator to manage.
 */
public class AdminImageListFragment extends Fragment {

    /// Image list view
    private ImageListView imageListView;


    /// Firestore listeners for user and event data
    private ListenerRegistration userListener;
    private ListenerRegistration eventListener;

    /// Navigation stack fragment
    private NavigationStackFragment navStack;

    /// Data lists for users and events
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
        imageListView = root.findViewById(R.id.image_list_view);

        // Click listener for each image
        imageListView.setOnImageClickListener(this::showActionDialog);

        // Start listening to Firestore
        setupUserObservers();
        setupEventObservers();

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
     * Sets up Firestore observers for users 
     */
    private void setupUserObservers() {
        userListener = UserController.getInstance()
                .observeAllUsers(this::onUsersChanged);
    }

    /**
     * Sets up Firestore observers for events
     */
    private void setupEventObservers() {
        eventListener = EventController.getInstance()
                .observeAllEvents(this::onEventsChanged);
    }

    /**
     * Callback for when user data changes
     */
    private void onUsersChanged(List<User> updatedUsers) {
        users = new ArrayList<>();
        for (User user : updatedUsers) {
            if (user.getPhotoURL() != null) {
                users.add(user);
            }
        }

        this.updateImages();
    }

    /**
     * Callback for when event data changes
     */
    private void onEventsChanged(List<Event> updatedEvents) {
        events = new ArrayList<>();
        for (Event event : updatedEvents) {
            if (event.getPosterUrl() != null) {
                events.add(event);
            }
        }

        this.updateImages();
    }

    /**
     * Updates the image list view with current users and events
     */
    private void updateImages() {
        List<ImageWrapper> images = new ArrayList<>();

        for (User user : users) {
            images.add(new ImageWrapper(user));
        }

        for (Event event : events) {
            images.add(new ImageWrapper(event));
        }

        imageListView.setImages(images);
    }

    /**
     * Shows a dialog with actions for the selected image
     */
    private void showActionDialog(ImageWrapper image) {
        switch (image.getImageSourceType()) {
            case USER:
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete user profile image?")
                        .setMessage("This will permanently remove the user's profile picture.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // Delete the image
                            HashMap<String, Object> updates = new HashMap<>();
                            updates.put("photoURL", null);
                            UserController.getInstance().updateFields(
                                    image.getUserID(),
                                    updates
                            );
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                break;

            case EVENT:
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete event image?")
                        .setMessage("This will permanently remove this event's image.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // Delete the image
                            HashMap<String, Object> updates = new HashMap<>();
                            updates.put("posterUrl", null);
                            EventController.getInstance().updateEvent(
                                    image.getEventID(),
                                    updates
                            );
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                break;
        }
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
