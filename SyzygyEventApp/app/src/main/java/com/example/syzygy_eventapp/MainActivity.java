package com.example.syzygy_eventapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.Tasks;
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity implements OnItemSelectedListener {
    private NavigationStackFragment navStack;
    private Fragment profileFragment;
    private Fragment findFragment;
    private Fragment joinedFragment;
    private Fragment organizerFragment;
    private Fragment adminFragment;
    private UserControllerInterface userController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Log the current AppInstallationId each time
        // the app starts.
        Log.i("AppInstallationId", "The current AppInstallationId is: "
                + AppInstallationId.get(this.getApplicationContext())
        );

        navStack = new NavigationStackFragment();

        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.activity_main, navStack)
                .commit();

        profileFragment = new ProfileFragment();
        findFragment = new FindEventsFragment(navStack);
        joinedFragment = new JoinedEventsFragment(navStack);
        organizerFragment = new OrganizerFragment(navStack);
        adminFragment = new AdministratorFragment();

        String userID = AppInstallationId.get(this);
        userController = UserController.getInstance();

// Ensure user exists before proceeding.
// If user is missing, go back to WelcomeActivity.
        userController.getUser(userID)
                .addOnSuccessListener(this::setupMainNavBar)
                .addOnFailureListener(e -> {
                    // No user found: redirect to welcome / onboarding
                    Intent intent = new Intent(this, WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    private void setupMainNavBar(User user) {
        this.updateMainNavBar(user);
        navStack.selectNavItem(R.id.profile_nav_button);

        userController.observeUser(user.getUserID(),
                this::updateMainNavBar,
                () -> {
                    // User was deleted
                    Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
                    intent.putExtra("profile_deleted", true); // 👈 same flag
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    private void updateMainNavBar(User user) {
        Role role = user.getRole();

        if (role == Role.ENTRANT) {
            navStack.setMainNavMenu(R.menu.entrant_nav_menu, this);
        } else if (role == Role.ORGANIZER) {
            navStack.setMainNavMenu(R.menu.organizer_nav_menu, this);
        } else {
            assert role == Role.ADMIN;
            navStack.setMainNavMenu(R.menu.admin_nav_menu, this);
        }
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