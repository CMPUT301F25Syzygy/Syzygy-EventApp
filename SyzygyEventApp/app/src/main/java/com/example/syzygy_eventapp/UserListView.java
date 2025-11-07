package com.example.syzygy_eventapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * A view representing a list of {@link User} objects.
 * Use the {@link UserListView#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserListView extends Fragment {

    /// The argument key for the title.
    private static final String ARG_TITLE = "title";

    /// The list of users to display.
    private List<User> users;
    /// The title of the user list.
    private String title;

    /// The RecyclerView for displaying the user list.
    private RecyclerView recyclerView;
    /// The TextView for displaying the user count.
    private TextView countText;
    /// The ImageView for the expand/collapse icon.
    private ImageView expandIcon;

    /// The adapter for the RecyclerView.
    private UserListViewAdapter adapter;
    /// Whether the list is currently expanded.
    private boolean isExpanded = true;

    /**
     * Required empty public constructor.
     */
    public UserListView() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param users The list of {@link User} objects to display.
     * @return A new instance of fragment UserListView.
     */
    public static UserListView newInstance(List<User> users) {
        return newInstance(users, "Users");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param users The list of {@link User} objects to display.
     * @param title The title of the user list.
     * @return A new instance of fragment UserListView.
     */
    public static UserListView newInstance(List<User> users, String title) {
        UserListView fragment = new UserListView();
        fragment.users = users;
        fragment.title = title;
        return fragment;
    }

    /**
     * Initializes the fragment and retrieves arguments.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate the layout
        View view = inflater.inflate(R.layout.fragment_user_list_view, container, false);

        // Bind UI components
        TextView titleText = view.findViewById(R.id.list_title);
        countText = view.findViewById(R.id.user_count);
        expandIcon = view.findViewById(R.id.expand_icon);
        recyclerView = view.findViewById(R.id.user_recycler_view);
        View headerLayout = view.findViewById(R.id.header_layout);

        // Setup UI
        titleText.setText(title != null ? title : "Users");
        updateCountText();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserListViewAdapter(users);
        recyclerView.setAdapter(adapter);

        // Expand/collapse behavior
        headerLayout.setOnClickListener(v -> toggleListVisibility());

        return view;
    }

    /** 
     * Adds a user at the end of the list and updates RecyclerView.
     * @param user The user to add.
     */
    public void addUser(User user) {
        if (users != null) {
            users.add(user);
            if (adapter != null) {
                adapter.notifyItemInserted(users.size() - 1);
            }
            updateCountText();
        }
    }

    /** 
     * Removes a user from the list and updates RecyclerView.
     * @param user The user to remove.
     */
    public void removeUser(User user) {
        if (users != null) {
            int index = users.indexOf(user);
            if (index != -1) {
                users.remove(index);
                if (adapter != null) {
                    adapter.notifyItemRemoved(index);
                }
                updateCountText();
            }
        }
    }

    /** 
     * Removes a user by index and updates RecyclerView.
     * @param index The index of the user to remove.
     */
    public void removeUserAt(int index) {
        if (users != null && index >= 0 && index < users.size()) {
            users.remove(index);
            if (adapter != null) {
                adapter.notifyItemRemoved(index);
            }
            updateCountText();
        }
    }

    /** 
     * Updates the user count TextView.
     */
    private void updateCountText() {
        if (countText != null && users != null) {
            countText.setText(String.valueOf(users.size()));
        }
    }

    /** 
     * Toggles the visibility of the user list.
     */
    private void toggleListVisibility() {
        isExpanded = !isExpanded;
        recyclerView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Rotate the expand icon for feedback
        expandIcon.animate()
                .rotation(isExpanded ? 0 : 90)
                .setDuration(150)
                .start();
    }
}
