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
 * Dialog fragment that displays a scrollable list of users
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

    public static UserListDialogFragment newInstance(String title, List<String> userIds) {
        UserListDialogFragment fragment = new UserListDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putStringArrayList(ARG_USER_IDS, new java.util.ArrayList<>(userIds));
        fragment.setArguments(args);
        return fragment;
    }

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_user_list, container, false);

        TextView titleView = view.findViewById(R.id.dialog_title);
        ListView listView = view.findViewById(R.id.user_list);
        TextView emptyView = view.findViewById(R.id.empty_text);

        titleView.setText(title);

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

            adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    userDisplayNames
            );
            listView.setAdapter(adapter);

            // Load user names asynchronously
            loadUserNames();
        }

        view.findViewById(R.id.close_button).setOnClickListener(v -> dismiss());

        return view;
    }

    private void loadUserNames() {
        for (int i = 0; i < userIds.size(); i++) {
            final int index = i;
            String userId = userIds.get(i);

            userController.getUser(userId)
                    .addOnSuccessListener(user -> {
                        if (user != null && user.getName() != null && !user.getName().isEmpty()) {
                            userDisplayNames.set(index, user.getName());
                        }
                        else {
                            // Fallback to ID
                            userDisplayNames.set(index, userId);
                        }
                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load user: " + userId, e);
                        // Fallback to ID
                        userDisplayNames.set(index, userId);
                        adapter.notifyDataSetChanged();
                    });
        }
    }

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