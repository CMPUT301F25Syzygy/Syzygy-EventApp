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
public class NotificationListView extends LinearLayout {
    /// The list of users to display.
    private List<Notification> notifications = new ArrayList<>();

    /// The RecyclerView for displaying the user list.
    private RecyclerView recyclerView;
    /// The TextView for displaying the title.
    private TextView titleText;
    /// The TextView for displaying the user count.
    private TextView countText;
    /// The ImageView for the expand/collapse icon.
    private ImageView expandIcon;

    /// The adapter for the RecyclerView.
    private NotificationListViewAdapter adapter;
    /// Whether the list is currently expanded.
    private boolean isExpanded = true;

    /**
     * Constructor with context.
     * @param context The context to use.
     */
    public NotificationListView(Context context) {
        super(context);
        init(context);
    }

    /**
     * Constructor with attribute set.
     * @param context The context to use.
     * @param attrs The attribute set.
     */
    public NotificationListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructor with style attributes.
     * @param context The context to use.
     * @param attrs The attribute set.
     * @param defStyleAttr The default style attribute.
     */
    public NotificationListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Initializes the UserListView.
     * @param context The context to use.
     */
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.fragment_notification_list_view, this, true);

        titleText = findViewById(R.id.list_title);
        countText = findViewById(R.id.count_text);
        expandIcon = findViewById(R.id.expand_icon);
        recyclerView = findViewById(R.id.recycler_view);

        View header = findViewById(R.id.header_layout);
        header.setOnClickListener(v -> toggleListVisibility());

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new NotificationListViewAdapter(notifications);
        recyclerView.setAdapter(adapter);
    }


    /**
     * Sets the list of notifications to display.
     *
     * @param newNotifications the list of notifications to display
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setNotifications(List<Notification> newNotifications) {
        notifications.clear();
        notifications.addAll(newNotifications);
        adapter.notifyDataSetChanged();
        updateCountText();
    }

    /**
     * Sets the title shown in the header
     *
     * @param title the title text to display
     */
    public void setTitle(String title) {
        titleText.setText(title);
    }

    /**
     * Removes an item by index and updates RecyclerView.
     * @param index The index of the notification to remove.
     */
    public void removeNotificationAt(int index) {
        if (notifications != null && index >= 0 && index < notifications.size()) {
            notifications.remove(index);
            if (adapter != null) {
                adapter.notifyItemRemoved(index);
            }
            updateCountText();
        }
    }

    /**
     * Sets the listener for notification item clicks.
     * @param listener The listener to set.
     */
    public void setOnListItemClickListener(NotificationListViewAdapter.OnItemClickListener<Notification> listener) {
        adapter.setOnItemClickListener(listener);
    }

    /**
     * Updates the notification count TextView.
     */
    public void updateCountText() {
        if (countText != null && notifications != null) {
            countText.setText(String.valueOf(notifications.size()));
        }
    }

    /**
     * Toggles the visibility of the notification list.
     */
    public void toggleListVisibility() {
        setListVisibility(!isExpanded);
    }

    /**
     * Sets the visibility of the notification list.
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
