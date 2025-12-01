package com.example.syzygy_eventapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener;
import com.google.firebase.firestore.Filter;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnItemSelectedListener {
    private NavigationStackFragment navStack;
    private Fragment profileFragment;
    private Fragment findFragment;
    private Fragment joinedFragment;
    private Fragment organizerFragment;
    private Fragment adminFragment;

    // controllers
    private UserControllerInterface userController;
    private InvitationController inviteController;

    private Set<String> openedInvites = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent launchIntent = getIntent();

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
        adminFragment = new AdministratorFragment(navStack);

        String userID = AppInstallationId.get(this);
        userController = UserController.getInstance();
        inviteController = InvitationController.getInstance();

        // Ensure user exists before proceeding.
        // If user is missing, go back to WelcomeActivity.
        userController.getUser(userID)
                .addOnSuccessListener((user) -> {
                    this.setupUser(user);

                    navStack.selectNavItem(R.id.profile_nav_button);
                })
                .addOnFailureListener(e -> {
                    // No user found: redirect to welcome / onboarding
                    Intent intent = new Intent(this, WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    private void setupUser(User user) {
        this.updateMainNavBar(user);
        updateMainNavBar(user);
        setupInviteListener(user);

        userController.observeUser(user.getUserID(),
                this::updateMainNavBar,
                () -> {
                    // User was deleted
                    Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
                    intent.putExtra("profile_deleted", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("MainActivity", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();

                    userController.updateFields(user.getUserID(), new HashMap<>(){{
                        put("fcmToken", token);
                    }});
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

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

    private void showInviteFragment(Invitation pendingInvite) {
        if (pendingInvite == null) {
            return;
        }

        Runnable action = () -> {
            InvitationFragment inviteFragment =
                    new InvitationFragment(pendingInvite.getInvitation(), navStack);

            navStack.pushScreen(inviteFragment);
        };

        if (navStack.getView() == null) {
            getWindow().getDecorView().post(action);
        } else {
            action.run();
        }
    }

    /**
     * Checks Firestore for any pending invitations for this user and, if found,
     * opens InvitationActivity for the first pending invite.
     */
    private void setupInviteListener(User user) {
        Filter filter = Filter.and(
                Filter.equalTo("recipientID", user.getUserID()),
                Filter.equalTo("cancelled", false)
        );

        inviteController.observeInvites(filter, invitations -> {
            for (Invitation invite : invitations) {
                if (invite.getResponseTime() == null) {
                    if (!openedInvites.contains(invite.getInvitation())) {
                        showInviteFragment(invite);
                        openedInvites.add(invite.getInvitation());
                    }
                }
            }
        });
    }
}