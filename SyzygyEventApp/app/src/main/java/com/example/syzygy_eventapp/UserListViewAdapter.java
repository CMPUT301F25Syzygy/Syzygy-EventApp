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
public class UserListViewAdapter extends RecyclerView.Adapter<UserListViewAdapter.UserViewHolder> {

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
    public UserListViewAdapter(List<User> users) {
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
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_user_view, parent, false);
        return new UserViewHolder(view);
    }

    /**
     * Binds user data to the ViewHolder.
     * @param holder The UserViewHolder to bind data to.
     * @param position The position of the user in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.nameText.setText(user.getName());
        holder.emailText.setText(user.getEmail());
        holder.phoneText.setText(user.getPhone());
        holder.roleChip.setText(user.getRole().toString());

        // Make sure to always default a UserView's image, otherwise it wont
        // be recycled properly.
        holder.profileImage.setImageResource(R.drawable.ic_person_placeholder);

        // Set the user's profile image
        if (user.getPhotoURL() != null) {
            byte[] decoded = Base64.decode(user.getPhotoURL(), Base64.DEFAULT);
            Bitmap bm = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            holder.profileImage.setImageBitmap(bm);
        }

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
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView profileImage;
        TextView nameText, emailText, phoneText;
        Chip roleChip;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.user_profile_image);
            nameText = itemView.findViewById(R.id.user_name);
            emailText = itemView.findViewById(R.id.user_email);
            phoneText = itemView.findViewById(R.id.user_phone);
            roleChip = itemView.findViewById(R.id.user_role_chip);
        }
    }
}
