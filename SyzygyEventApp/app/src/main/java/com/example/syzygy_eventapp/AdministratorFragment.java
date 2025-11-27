package com.example.syzygy_eventapp;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.firebase.firestore.ListenerRegistration;

import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

/**
 * The administrator dashboard fragment.
 */
public class AdministratorFragment extends Fragment {

    /// Firestore listener for user data
    private ListenerRegistration userListener;

    /// Profile button
    private LinearLayout profileButton;

    /// Navigation stack fragment
    private NavigationStackFragment navStack;

    /// Required empty constructor
    public AdministratorFragment() {
        this.navStack = null;
    }

    /// Constructor with navigation stack
    AdministratorFragment(NavigationStackFragment navStack) {
        this.navStack = navStack;
    }

    /**
     * Inflates the fragment layout and binds views.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_administrator, container, false);

        // Bind buttons
        profileButton = root.findViewById(R.id.profile_button);

        // Set listeners
        profileButton.setOnClickListener(v -> navStack.pushScreen(
                new AdminProfileListFragment(navStack)
        ));

        return root;
    }
}