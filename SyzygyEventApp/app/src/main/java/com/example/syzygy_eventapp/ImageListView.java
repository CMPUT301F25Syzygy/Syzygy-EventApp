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
 * A view representing a list of {@link ImageWrapper} objects.
 */
public class ImageListView extends LinearLayout {
    /// The list of images (and their metadata) to display
    private List<ImageWrapper> images = new ArrayList<>();

    /// The RecyclerView for displaying the image list.
    private RecyclerView recyclerView;
    /// The TextView for displaying the title.
    private TextView titleText;
    /// The TextView for displaying the image count.
    private TextView countText;
    /// The ImageView for the expand/collapse icon.
    private ImageView expandIcon;

    /// The adapter for the RecyclerView.
    private ImageListViewAdapter adapter;
    /// Whether the list is currently expanded.
    private boolean isExpanded = true;

    /**
     * Constructor with context.
     * @param context The context to use.
     */
    public ImageListView(Context context) {
        super(context);
        init(context);
    }

    /**
     * Constructor with attribute set.
     * @param context The context to use.
     * @param attrs The attribute set.
     */
    public ImageListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructor with style attributes.
     * @param context The context to use.
     * @param attrs The attribute set.
     * @param defStyleAttr The default style attribute.
     */
    public ImageListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Initializes the ImageListView.
     * @param context The context to use.
     */
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.fragment_image_list, this, true);

        titleText = findViewById(R.id.list_title);
        countText = findViewById(R.id.count_text);
        expandIcon = findViewById(R.id.expand_icon);
        recyclerView = findViewById(R.id.image_recycler_view);

        View header = findViewById(R.id.header_layout);
        header.setOnClickListener(v -> toggleListVisibility());

        adapter = new ImageListViewAdapter(images);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Sets the entire image list and updates RecyclerView.
     * @param newImages The new list of images to display.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setImages(List<ImageWrapper> newImages) {
        images.clear();
        images.addAll(newImages);
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
     * Sets the listener for image item clicks.
     * @param listener The listener to set.
     */
    public void setOnImageClickListener(ImageListViewAdapter.OnImageClickListener listener) {
        adapter.setOnImageClickListener(listener);
    }

    /**
     * Updates the image count TextView.
     */
    public void updateCountText() {
        if (countText != null && images != null) {
            countText.setText(String.valueOf(images.size()));
        }
    }

    /**
     * Toggles the visibility of the image list.
     */
    public void toggleListVisibility() {
        setListVisibility(!isExpanded);
    }

    /**
     * Sets the visibility of the image list.
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
