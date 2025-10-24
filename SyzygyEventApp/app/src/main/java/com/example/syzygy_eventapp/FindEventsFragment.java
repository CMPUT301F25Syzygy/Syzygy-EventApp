package com.example.syzygy_eventapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A empty placeholder for FindEventsFragment
 */
public class FindEventsFragment extends Fragment {
    final private NavigationStackFragment navStack;
    private QRScanFragment qrFragment;

    FindEventsFragment(NavigationStackFragment navStack) {
        super();
        this.navStack = navStack;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_find_events, container, false);

        qrFragment = new QRScanFragment(navStack);

        view.findViewById(R.id.open_qr_scan_button).setOnClickListener((v) -> {
            navStack.pushScreen(qrFragment);
        });

        return view;
    }
}