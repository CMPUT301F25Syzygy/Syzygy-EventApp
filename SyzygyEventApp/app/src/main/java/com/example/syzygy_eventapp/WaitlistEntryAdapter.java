package com.example.syzygy_eventapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying waitlist entries with timestamps.
 */
public class WaitlistEntryAdapter extends RecyclerView.Adapter<WaitlistEntryAdapter.EntryViewHolder> {

    private final List<WaitlistEntry> entries;
    private final SimpleDateFormat dateFormat;

    public WaitlistEntryAdapter(List<WaitlistEntry> entries) {
        this.entries = entries;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waitlist_entry, parent, false);
        return new EntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        WaitlistEntry entry = entries.get(position);

        holder.nameText.setText(entry.getUserName() != null ? entry.getUserName() : entry.getUserId());

        // Show appropriate timestamp based on status
        if ("cancelled".equals(entry.getStatus()) && entry.getCancellationDate() != null) {
            holder.timestampText.setText("Cancelled: " + dateFormat.format(entry.getCancellationDate().toDate()));
            holder.timestampText.setVisibility(View.VISIBLE);
        }
        else if ("accepted".equals(entry.getStatus()) && entry.getRegistrationDate() != null) {
            holder.timestampText.setText("Accepted: " + dateFormat.format(entry.getRegistrationDate().toDate()));
            holder.timestampText.setVisibility(View.VISIBLE);
        }
        else if ("rejected".equals(entry.getStatus()) && entry.getRegistrationDate() != null) {
            holder.timestampText.setText("Rejected: " + dateFormat.format(entry.getRegistrationDate().toDate()));
            holder.timestampText.setVisibility(View.VISIBLE);
        }
        else if ("pending".equals(entry.getStatus()) && entry.getJoinedAt() != null) {
            holder.timestampText.setText("Invited: " + dateFormat.format(entry.getJoinedAt().toDate()));
            holder.timestampText.setVisibility(View.VISIBLE);
        }
        else if ("waiting".equals(entry.getStatus())) {
            // Waiting list doesn't have timestamps, just the user, so I'll hide the timestamp
            holder.timestampText.setVisibility(View.GONE);
        }
        else {
            holder.timestampText.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public static class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView timestampText;

        public EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.entry_name);
            timestampText = itemView.findViewById(R.id.entry_timestamp);
        }
    }
}