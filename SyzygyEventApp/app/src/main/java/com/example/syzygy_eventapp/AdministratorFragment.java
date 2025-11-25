package com.example.syzygy_eventapp;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.ListenerRegistration;

import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

/**
 * The administrator dashboard fragment.
 */
public class AdministratorFragment extends Fragment {

    /// User list view
    private UserListView userListView;

    /// Firestore listener for user data
    private ListenerRegistration userListener;


    /**
     * Inflates the fragment layout and binds views.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_administrator, container, false);

        // Bind views
        userListView = root.findViewById(R.id.user_list_view);

        // Set up user list click listener
        userListView.setOnUserClickListener(user -> {
            showUserActionDialog(user);
        });

        // Set up Firestore observers
        setupUserObservers();

        return root;
    }

    /**
     * Sets up Firestore listeners to populate the user lists.
     */
    private void setupUserObservers() {

        userListener = UserController.getInstance()
                .observeAllUsers(
                        this::onUsersChanged,
                        Throwable::printStackTrace
                );
    }

    /**
     * Called whenever Firestore sends updated user data.
     */
    private void onUsersChanged(List<User> users) {
        userListView.setUsers(users);
    }

    /**
     * Represents an action item in the user action dialog.
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
     * Shows a dialog with actions for the selected user.
     */
    private void showUserActionDialog(User user) {

        List<ActionItem> actions = new ArrayList<>();

        switch (user.getRole()) {
            case ENTRANT:
                actions.add(new ActionItem("Promote", () ->
                        UserController.getInstance().setUserRole(user.getUserID(), Role.ORGANIZER)
                ));
                actions.add(new ActionItem("Disable Picture", () ->
                        // TODO: Disable the user's picture
                        {}
                ));
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
                actions.add(new ActionItem("Disable Picture", () ->
                        // TODO: Disable the user's picture
                        {}
                ));
                actions.add(new ActionItem("Delete", () ->
                        showConfirmDeleteDialog(user)
                ));
                break;

            case ADMIN:
                actions.add(new ActionItem("Demote", () ->
                        UserController.getInstance().setUserRole(user.getUserID(), Role.ORGANIZER)
                ));
                actions.add(new ActionItem("Disable Picture", () ->
                        // TODO: Disable the user's picture
                        {}
                ));
                actions.add(new ActionItem("Delete", () ->
                        showConfirmDeleteDialog(user)

                ));
                break;
        }

        // Convert labels to CharSequence[]
        CharSequence[] labels = actions.stream()
                .map(a -> a.label)
                .toArray(CharSequence[]::new);

        new AlertDialog.Builder(requireContext())
                .setTitle(user.getName())
                .setItems(labels, (dialog, which) -> {
                    actions.get(which).action.run();   // elegant dispatch!
                })
                .show();
    }

    /**
     * Shows a confirmation dialog before deleting a user.
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
     * Cleans up Firestore listeners when the view is destroyed.
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