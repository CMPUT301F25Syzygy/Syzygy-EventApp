package com.example.syzygy_eventapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A empty placeholder for OrganizerFragment
 */
public class OrganizerFragment extends Fragment {

    // Creating a custom constructor so that EventListFragment can pass params
    private final NavigationStackFragment navStack;
    private final String eventID;

    // Custom constructor to match how it's called
    public OrganizerFragment(NavigationStackFragment navStack, String eventID) {
        this.navStack = navStack;
        this.eventID = eventID;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_organizer, container, false);
    }
}