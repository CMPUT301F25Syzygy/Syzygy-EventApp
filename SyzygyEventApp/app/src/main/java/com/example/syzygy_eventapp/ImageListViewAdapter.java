package com.example.syzygy_eventapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
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
 * RecyclerView Adapter for displaying a list of ImageWrappers.
 */
public class ImageListViewAdapter extends RecyclerView.Adapter<ImageListViewAdapter.ImageViewHolder> {

    /// The list of images to display.
    private final List<ImageWrapper> images;

    /// The listener for image item clicks.
    private OnImageClickListener listener;

    /**
     * Interface for handling image item clicks.
     */
    public interface OnImageClickListener {
        void onImageClick(ImageWrapper image);
    }

    /**
     * Constructor for ImageListViewAdapter.
     *
     * @param images The list of images to display.
     */
    public ImageListViewAdapter(List<ImageWrapper> images) {
        this.images = images;
    }

    /**
     * Creates a new ViewHolder for an image item.
     * @param parent The parent ViewGroup.
     * @param viewType The view type of the new View.
     * @return A new ImageViewHolder instance.
     */
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_grid_item, parent, false);
        return new ImageViewHolder(view);
    }

    /**
     * Binds image data to the ViewHolder.
     * @param holder The ImageViewHolder to bind data to.
     * @param position The position of the image in the list
     */
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageWrapper image = images.get(position);

        // Show a temporary placeholder so recycled cells don’t show old images
        holder.imageView.setImageBitmap(null);
        holder.metaText.setText("Loading...");

        switch (image.getImageSourceType()) {
            case USER:
                UserController.getInstance()
                        .getUser(image.getUserID())
                        .addOnSuccessListener(user -> {
                            if (user != null && user.getPhotoURL() != null) {
                                byte[] decoded = Base64.decode(user.getPhotoURL(), Base64.DEFAULT);
                                Bitmap bm = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                                holder.imageView.setImageBitmap(bm);
                            }

                            holder.metaText.setText(user != null ?
                                    "From User: " + user.getName() :
                                    "From User: (Unknown)");
                        })
                        .addOnFailureListener(error -> {
                            holder.metaText.setText("From User: (Error)");
                        });
                break;

            case EVENT:
                EventController.getInstance()
                        .getEvent(image.getEventID())
                        .addOnSuccessListener(event -> {
                            if (event != null && event.getPosterUrl() != null) {
                                byte[] decoded = Base64.decode(event.getPosterUrl(), Base64.DEFAULT);
                                Bitmap bm = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                                holder.imageView.setImageBitmap(bm);
                            }

                            holder.metaText.setText(event != null ?
                                    "From Event: " + event.getName() :
                                    "From Event: (Unknown)");
                        })
                        .addOnFailureListener(error -> {
                            holder.metaText.setText("From Event: (Error)");
                        });
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onImageClick(image);
        });
    }

    /**
     * Returns the total number of images in the list.
     * @return The number of images.
     */
    @Override
    public int getItemCount() {
        return images.size();
    }

    /**
     * Sets the listener for image item clicks.
     * @param listener The OnImageClickListener to set.
     */
    public void setOnImageClickListener(OnImageClickListener listener) {
        this.listener = listener;
    }

    /**
     * Adds an image at the end of the list and updates the RecyclerView
     */
    public void addImage(ImageWrapper image) {
        images.add(image);
        notifyItemInserted(images.size() - 1);
    }

    /**
     * Removes an image by object and updates the RecyclerView
     */
    public void removeImage(ImageWrapper image) {
        int index = images.indexOf(image);
        if (index != -1) {
            images.remove(index);
            notifyItemRemoved(index);
        }
    }

    /**
     * Removes an image by index
     */
    public void removeImageAt(int index) {
        if (index >= 0 && index < images.size()) {
            images.remove(index);
            notifyItemRemoved(index);
        }
    }

    /**
     * ViewHolder class for image items.
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
