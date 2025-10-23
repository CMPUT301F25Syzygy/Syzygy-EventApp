package com.example.syzygy_eventapp;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener;

public class MainActivity extends AppCompatActivity implements OnItemSelectedListener {
    private NavigationStackFragment navStack;
    private Fragment profileFragment;
    private Fragment findFragment;
    private Fragment joinedFragment;
    private Fragment organizerFragment;
    private Fragment adminFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navStack = new NavigationStackFragment();

        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.activity_main, navStack)
                .commit();

        profileFragment = new ProfileFragment();
        findFragment = new FindEventsFragment(navStack);
        joinedFragment = new JoinedEventsFragment();
        organizerFragment = new OrganizerFragment();
        adminFragment = new AdministratorFragment();

        navStack.setScreenNavMenu(0, R.menu.entrant_nav_menu, this);
        // navStack.setScreenNavMenu(0, R.menu.organizer_nav_menu, this);
        // navStack.setScreenNavMenu(0, R.menu.admin_nav_menu, this);

        navStack.selectNavItem(R.id.profile_nav_button);
    }

    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.profile_nav_button) {
            navStack.replaceScreen(profileFragment);
        } else if (id == R.id.find_nav_button) {
            navStack.replaceScreen(findFragment);
        } else if (id == R.id.events_nav_button) {
            navStack.replaceScreen(joinedFragment);
        } else if (id == R.id.organize_nav_button) {
            navStack.replaceScreen(organizerFragment);
        } else if (id == R.id.admin_nav_button) {
            navStack.replaceScreen(adminFragment);
        } else {
            return false;
        }

        return true;
    }
}