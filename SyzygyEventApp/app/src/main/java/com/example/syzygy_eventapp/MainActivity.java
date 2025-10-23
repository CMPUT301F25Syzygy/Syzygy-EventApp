package com.example.syzygy_eventapp;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener;

public class MainActivity extends AppCompatActivity implements OnItemSelectedListener {
    private NavigationStackFragment navStack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navStack = new NavigationStackFragment();

        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.activity_main, navStack)
                .commit();

        navStack.pushScreen(new Fragment()); // empty screen

        navStack.setScreenNavMenu(0, R.menu.entrant_nav_menu, this);
        // navStack.setScreenNavMenu(0, R.menu.organizer_nav_menu, this);
        // navStack.setScreenNavMenu(0, R.menu.admin_nav_menu, this);
    }

    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        // do screen replacement

        return true;
    }
}