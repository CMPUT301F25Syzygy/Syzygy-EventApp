package com.example.syzygy_eventapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * A view representing a list of {@link User} objects.
 */
public class UserListView extends LinearLayout {
    /// The list of users to display.
    private List<User> users = new ArrayList<>();

    /// The RecyclerView for displaying the user list.
    private RecyclerView recyclerView;
    /// The TextView for displaying the title.
    private TextView titleText;
    /// The TextView for displaying the user count.
    private TextView countText;
    /// The ImageView for the expand/collapse icon.
    private ImageView expandIcon;

    /// The adapter for the RecyclerView.
    private UserListViewAdapter adapter;
    /// Whether the list is currently expanded.
    private boolean isExpanded = true;

    /** 
     * Constructor with context.
     * @param context The context to use.
     */
    public UserListView(Context context) {
        super(context);
        init(context);
    }

    /**
     * Constructor with attribute set.
     * @param context The context to use.
     * @param attrs The attribute set.
     */
    public UserListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructor with style attributes.
     * @param context The context to use.
     * @param attrs The attribute set.
     * @param defStyleAttr The default style attribute.
     */
    public UserListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /** 
     * Initializes the UserListView.
     * @param context The context to use.
     */
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.fragment_user_list_view, this, true);

        titleText = findViewById(R.id.list_title);
        countText = findViewById(R.id.user_count);
        expandIcon = findViewById(R.id.expand_icon);
        recyclerView = findViewById(R.id.user_recycler_view);

        View header = findViewById(R.id.header_layout);
        header.setOnClickListener(v -> toggleListVisibility());

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new UserListViewAdapter(users);
        recyclerView.setAdapter(adapter);
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
     * Sets the entire user list and updates RecyclerView.
     * @param newUsers The new list of users.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setUsers(List<User> newUsers) {
        users.clear();
        users.addAll(newUsers);
        adapter.notifyDataSetChanged();
        updateCountText();
    }

    /**
     * Sets the title shown in the header (e.g., "Events", "Upcoming", "History").
     *
     * @param title the title text to display
     */
    public void setTitle(String title) {
        titleText.setText(title);
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
     * Sets the listener for user item clicks.
     * @param listener The listener to set.
     */
    public void setOnUserClickListener(UserListViewAdapter.OnUserClickListener listener) {
        adapter.setOnUserClickListener(listener);
    }

    /**
     * Updates the user count TextView.
     */
    public void updateCountText() {
        if (countText != null && users != null) {
            countText.setText(String.valueOf(users.size()));
        }
    }

    /** 
     * Toggles the visibility of the user list.
     */
    public void toggleListVisibility() {
        setListVisibility(!isExpanded);
    }

    /**
     * Sets the visibility of the user list.
     */
    public void setListVisibility(boolean isExpanded) {
        if (isExpanded == this.isExpanded) return;
        this.isExpanded = isExpanded;

        recyclerView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Rotate the expand icon for feedback
        expandIcon.animate()
                .rotation(isExpanded ? 0 : 90)
                .setDuration(150)
                .start();
    }
}
