package com.example.syzygy_eventapp;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog fragment that displays a scrollable list of user names.
 * <p>
 * Used to show the organizer a lists of users in:
 * <ul>
 *     <li>Accepted invitations</li>
 *     <li>Pending invitations</li>
 *     <li>Rejected invitations</li>
 *     <li>Cancelled invitations</li>
 *     <li>Waiting list users</li>
 * </ul>
 * </p>
 */
public class UserListDialogFragment extends DialogFragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_USER_IDS = "user_ids";
    private static final String TAG = "UserListDialog";

    private String title;
    private List<String> userIds;
    private ArrayAdapter<String> adapter;
    private List<String> userDisplayNames;
    private UserControllerInterface userController;

    /**
     * Factory emthod to create a new instance of this dialog
     *
     * @param title The title to display at the top of the dialog
     * @param userIds List of user IDs to display in the dialog
     * @return A new instance of {@link UserListDialogFragment}
     */
    public static UserListDialogFragment newInstance(String title, List<String> userIds) {
        UserListDialogFragment fragment = new UserListDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putStringArrayList(ARG_USER_IDS, new java.util.ArrayList<>(userIds));
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called when the fragment is created. Initializes the userController, and retrieves args passed to the fragment
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            userIds = getArguments().getStringArrayList(ARG_USER_IDS);
        }
        userController = UserController.getInstance();
        userDisplayNames = new ArrayList<>();
    }

    /**
     * Creates and returns the view hieracrhy associated with the dialog. Will set up the title, list view, and handles the empty state.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_user_list, container, false);

        // Initialize UI components
        TextView titleView = view.findViewById(R.id.dialog_title);
        ListView listView = view.findViewById(R.id.user_list);
        TextView emptyView = view.findViewById(R.id.empty_text);

        titleView.setText(title);

        // Handle empty state by showing a message if there are no users in the list
        if (userIds == null || userIds.isEmpty()) {
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }
        else {
            listView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            // Initialize adapter with loading placeholders
            for (int i = 0; i < userIds.size(); i++) {
                userDisplayNames.add("Loading...");
            }

            // Set up the list adapter
            adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    userDisplayNames
            );
            listView.setAdapter(adapter);

            // Load user names from Firestore
            loadUserNames();
        }

        // Set up the close button
        view.findViewById(R.id.close_button).setOnClickListener(v -> dismiss());

        return view;
    }

    /**
     * Asychronously loads the user names from Firestore for each userID in the list, and updates the adapter as they are fetched.
     * If a user cannot be found, it will display the userID as a fallback
     */
    private void loadUserNames() {
        for (int i = 0; i < userIds.size(); i++) {
            final int index = i;
            String userId = userIds.get(i);

            // Get user from Firestore
            userController.getUser(userId)
                    .addOnSuccessListener(user -> {
                        // Update with user name (if availble)
                        if (user != null && user.getName() != null && !user.getName().isEmpty()) {
                            userDisplayNames.set(index, user.getName());
                        }
                        else {
                            // Fallback to ID
                            userDisplayNames.set(index, userId);
                        }
                        // Notify the adaper to refresh the list view
                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load user: " + userId, e);
                        // Fallback to displaying the userID if an error occurs and refresh the list
                        userDisplayNames.set(index, userId);
                        adapter.notifyDataSetChanged();
                    });
        }
    }

    /**
     * Called when the dialog is started, and will set the dialog window sixe to match parent width and wrap content height for the best display.
     */
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}