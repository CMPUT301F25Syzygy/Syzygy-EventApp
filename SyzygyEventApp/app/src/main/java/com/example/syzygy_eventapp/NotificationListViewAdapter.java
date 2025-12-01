package com.example.syzygy_eventapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.Visibility;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

/**
 * RecyclerView Adapter for displaying a list of Notifications.
 */
public class NotificationListViewAdapter extends RecyclerView.Adapter<NotificationListViewAdapter.ListItemViewHolder> {

    /// The list of notifications to display.
    private final List<Notification> notifications;

    /// The listener for notification item clicks.
    private OnItemClickListener<Notification> listener;

    private EventController eventController;

    /**
     * Interface for handling notification item clicks.
     */
    public interface OnItemClickListener<T> {
        void onItemClick(T item);
    }

    public NotificationListViewAdapter(List<Notification> notifications) {
        this.notifications = notifications;
        this.eventController = EventController.getInstance();
    }

    /**
     * Creates a new ViewHolder for a user item.
     * @param parent The parent ViewGroup.
     * @param viewType The view type of the new View.
     * @return A new NotificationViewHolder instance.
     */
    @NonNull
    @Override
    public ListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_notification_summary_view, parent, false);
        return new ListItemViewHolder(view);
    }

    /**
     * Binds notification data to the ViewHolder.
     * @param holder The NotificationViewHolder to bind data to.
     * @param position The position of the notification in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull ListItemViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.titleText.setText(notification.getTitle());
        holder.descriptionText.setText(notification.getDescription());


        holder.eventText.setVisibility(View.GONE);

        String eventId = notification.getEventId();
        if(eventId != null) {
            this.eventController.getEvent(eventId).addOnSuccessListener((event) -> {
                if (event.getName() != null) {
                    holder.eventText.setText("From Event: " + event.getName());
                }
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(notification);
        });
    }

    /**
     * Returns the total number of notifications in the list.
     * @return The number of notifications.
     */
    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /**
     * Sets the listener for notification item clicks.
     * @param listener The OnNotificationClickListener to set.
     */
    public void setOnItemClickListener(OnItemClickListener<Notification> listener) {
        this.listener = listener;
    }

    /**
     * Adds a notification at the end of the list and updates the RecyclerView
     */
    public void addNotification(Notification notification) {
        notifications.add(notification);
        notifyItemInserted(notifications.size() - 1);
    }

    /**
     * Removes a notification by object and updates the RecyclerView
     */
    public void removeNotification(Notification notification) {
        int index = notifications.indexOf(notification);
        if (index != -1) {
            notifications.remove(index);
            notifyItemRemoved(index);
        }
    }

    /**
     * Removes a notification by index
     */
    public void removeNotificationAt(int index) {
        if (index >= 0 && index < notifications.size()) {
            notifications.remove(index);
            notifyItemRemoved(index);
        }
    }

    /**
     * ViewHolder class for notification items.
     */
    public static class ListItemViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, descriptionText, eventText;

        public ListItemViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.title_text);
            descriptionText = itemView.findViewById(R.id.description_text);
            eventText = itemView.findViewById(R.id.event_text);
        }
    }
}
