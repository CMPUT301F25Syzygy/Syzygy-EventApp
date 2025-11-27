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
public class AdminProfileListFragment extends Fragment {

    /// User list view
    private UserListView userListView;
    /// Firestore listener for user data
    private ListenerRegistration userListener;
    /// Navigation stack fragment
    private NavigationStackFragment navStack;

    /// Required empty constructor
    public AdminProfileListFragment() {
        this.navStack = null;
    }

    /**
     * Constructor with navigation stack
     */
    AdminProfileListFragment(NavigationStackFragment navStack) {
        this.navStack = navStack;
    }

    /**
     * Inflates the fragment layout and binds views.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_admin_profile_list, container, false);

        // Bind list view
        userListView = root.findViewById(R.id.user_list_view);

        // Click listener for each user
        userListView.setOnUserClickListener(this::showUserActionDialog);

        // Start listening to Firestore
        setupUserObservers();

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
                .observeAllUsers(
                        this::onUsersChanged,
                        Throwable::printStackTrace
                );
    }

    private void onUsersChanged(List<User> users) {
        userListView.setUsers(users);
    }

    /**
     * Dialog actions
     */
    private class ActionItem {
        String label;
        Runnable action;

        ActionItem(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }

    /**
     * Shows a dialog with actions for the selected user
     */
    private void showUserActionDialog(User user) {
        List<ActionItem> actions = new ArrayList<>();

        switch (user.getRole()) {
            case ENTRANT:
                actions.add(new ActionItem("Promote", () ->
                        UserController.getInstance().setUserRole(user.getUserID(), Role.ORGANIZER)
                ));
                actions.add(new ActionItem("Disable Picture", () -> {}));
                actions.add(new ActionItem("Delete", () ->
                        showConfirmDeleteDialog(user)
                ));
                break;

            case ORGANIZER:
                actions.add(new ActionItem("Promote", () ->
                        UserController.getInstance().setUserRole(user.getUserID(), Role.ADMIN)
                ));
                actions.add(new ActionItem("Demote", () ->
                        UserController.getInstance().setUserRole(user.getUserID(), Role.ENTRANT)
                ));
                actions.add(new ActionItem("Disable Picture", () -> {}));
                actions.add(new ActionItem("Delete", () ->
                        showConfirmDeleteDialog(user)
                ));
                break;

            case ADMIN:
                actions.add(new ActionItem("Demote", () ->
                        UserController.getInstance().setUserRole(user.getUserID(), Role.ORGANIZER)
                ));
                actions.add(new ActionItem("Disable Picture", () -> {}));
                actions.add(new ActionItem("Delete", () ->
                        showConfirmDeleteDialog(user)
                ));
                break;
        }

        CharSequence[] labels = actions.stream()
                .map(a -> a.label)
                .toArray(CharSequence[]::new);

        new AlertDialog.Builder(requireContext())
                .setTitle(user.getName())
                .setItems(labels, (dialog, which) ->
                        actions.get(which).action.run()
                )
                .show();
    }

    /**
     * Confirmation dialog for deleting a user
     */
    private void showConfirmDeleteDialog(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete " + user.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) ->
                        UserController.getInstance().deleteUser(user.getUserID())
                )
                .setNegativeButton("Cancel", null)
                .show();
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
