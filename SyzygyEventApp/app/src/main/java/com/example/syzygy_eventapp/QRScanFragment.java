package com.example.syzygy_eventapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationBarView;

/**
 * A empty placeholder for QRScanFragment
 */
public class QRScanFragment extends Fragment {
    NavigationStackFragment navStack;

    QRScanFragment(NavigationStackFragment navStack) {
        this.navStack = navStack;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_qr_scan, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        navStack.setScreenNavMenu(R.menu.back_nav_menu, (i) -> {
            navStack.popScreen();
            return true;
        });
    }
}