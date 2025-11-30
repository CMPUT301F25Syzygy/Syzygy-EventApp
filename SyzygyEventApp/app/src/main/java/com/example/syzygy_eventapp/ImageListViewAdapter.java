package com.example.syzygy_eventapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
 * RecyclerView Adapter for displaying a list of Users.
 */
public class ImageListViewAdapter extends RecyclerView.Adapter<ImageListViewAdapter.ImageViewHolder> {

    /// The list of users to display.
    private final List<User> users;

    /// The listener for user item clicks.
    private OnUserClickListener listener;

    /**
     * Interface for handling user item clicks.
     */
    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    /**
     * Constructor for UserListViewAdapter.
     *
     * @param users The list of users to display.
     */
    public ImageListViewAdapter(List<User> users) {
        this.users = users;
    }

    /**
     * Creates a new ViewHolder for a user item.
     * @param parent The parent ViewGroup.
     * @param viewType The view type of the new View.
     * @return A new UserViewHolder instance.
     */
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_grid_item, parent, false);
        return new ImageViewHolder(view);
    }

    /**
     * Binds user data to the ViewHolder.
     * @param holder The UserViewHolder to bind data to.
     * @param position The position of the user in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        User user = users.get(position);

        if (user.getPhotoURL() != null) {
            byte[] decoded = Base64.decode(user.getPhotoURL(), Base64.DEFAULT);
            Bitmap bm = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            holder.imageView.setImageBitmap(bm);
        }

        holder.metaText.setText(user.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(user);
        });
    }

    /**
     * Returns the total number of users in the list.
     * @return The number of users.
     */
    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * Sets the listener for user item clicks.
     * @param listener The OnUserClickListener to set.
     */
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    /**
     * Adds a user at the end of the list and updates the RecyclerView
     */
    public void addUser(User user) {
        users.add(user);
        notifyItemInserted(users.size() - 1);
    }

    /**
     * Removes a user by object and updates the RecyclerView
     */
    public void removeUser(User user) {
        int index = users.indexOf(user);
        if (index != -1) {
            users.remove(index);
            notifyItemRemoved(index);
        }
    }

    /**
     * Removes a user by index
     */
    public void removeUserAt(int index) {
        if (index >= 0 && index < users.size()) {
            users.remove(index);
            notifyItemRemoved(index);
        }
    }

    /**
     * ViewHolder class for user items.
     */
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imageView;
        TextView metaText;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
                imageView = itemView.findViewById(R.id.image_view);
                metaText = itemView.findViewById(R.id.meta_text);
        }
    }
}
