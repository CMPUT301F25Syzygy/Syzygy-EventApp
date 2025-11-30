package com.example.syzygy_eventapp;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener;

import java.util.Stack;
import java.util.UUID;

/**
 * Manages a stack of different screens (fragments) that can be pushed and popped.
 * This allows one screen to open a sub screen and then return back.
 * Displays a bottom navigation bar which screens can customize and then listen to.
 */
public class NavigationStackFragment extends Fragment implements OnItemSelectedListener {
    private static class Screen {
        public Fragment fragment;
        public OnItemSelectedListener listener = null;
        public Integer menuResId = null;
        public Integer menuSelectedItemId = null;
        public UUID uuid = UUID.randomUUID();

        Screen(Fragment fragment) {
            this.fragment = fragment;
        }
    }

    final private Stack<Screen> screenStack;
    private OnBackPressedCallback backCallback;
    private BottomNavigationView navBar;
    private UUID displayedScreenUUID = null;

    public NavigationStackFragment() {
        screenStack = new Stack<Screen>();
        // one empty screen in the stack to hold the main menu
        screenStack.push(new Screen(null));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_navigation_stack, container, false);

        // create nav bar
        navBar = view.findViewById(R.id.bottom_nav_bar);
        navBar.setOnItemSelectedListener(this);
        refreshNavBar();

        // create a listener for the OS back button
        // disabled when there are no screen to pop
        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                popScreen();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);

        return view;
    }

    /**
     * Handles when a nav item is clicked, calling whatever callback is associated.
     *
     * @param item the selected item
     * @return true if the item should be shown as selected
     */
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        Screen menuScreen = getCurrentMenuScreen();
        menuScreen.menuSelectedItemId = id;

        return menuScreen.listener.onNavigationItemSelected(item);
    }

    /**
     * Adds a screen to the stack.
     *
     * @param fragment a fragment with new screen contents
     */
    public void pushScreen(Fragment fragment) {
        FragmentManager manager = getParentFragmentManager();
        manager.beginTransaction()
                .replace(R.id.fragment_frame, fragment)
                .commit();

        if (isEmpty()) {
            // there is always at least one screen with a null fragment to hold the main menu
            // this will give the screen a fragment
            screenStack.get(0).fragment = fragment;
        } else {
            screenStack.push(new Screen(fragment));
        }

        backCallback.setEnabled(screenStack.size() > 1);

        refreshNavBar();
    }

    /**
     * Remove the screen on the top of the stack. Goes back to a previous screen.
     *
     * @throws IllegalStateException when there is only one screen left to pop
     */
    public void popScreen() {
        if (screenStack.size() == 1) {
            throw new IllegalStateException("Shouldn't pop last screen");
        }

        screenStack.pop();
        Screen screen = screenStack.peek();

        backCallback.setEnabled(screenStack.size() > 1);

        FragmentManager manager = getParentFragmentManager();
        manager.beginTransaction()
                .replace(R.id.fragment_frame, screen.fragment)
                .commit();

        refreshNavBar();
    }

    /**
     * Replaces the screen at the top of the stack, keeping whatever nav menu may have had.
     * This means the old screen's onNavigationItemSelected listener can still be called.
     *
     * @param fragment a fragment to replace the screen contents
     */
    public void replaceScreen(Fragment fragment) {

        FragmentManager manager = getParentFragmentManager();
        manager.beginTransaction()
                .replace(R.id.fragment_frame, fragment)
                .commit();

        // there will always be at least one screen on the stack, even if it's fragment may be null
        Screen screen = screenStack.peek();
        screen.fragment = fragment;

        refreshNavBar();
    }

    /**
     * Calls {@link #setScreenNavMenu(int, int, OnItemSelectedListener)} with the index of the top stack item
     */
    public void setScreenNavMenu(int menuResId, OnItemSelectedListener listener) {
        setScreenNavMenu(screenStack.size() - 1, menuResId, listener);
    }

    /**
     * Calls {@link #setScreenNavMenu(int, int, OnItemSelectedListener)} with the index of 0 (the main menu)
     */
    public void setMainNavMenu(int menuResId, OnItemSelectedListener listener) {
        setScreenNavMenu(0, menuResId, listener);
    }

    /**
     * Sets the navigation menu for a screen.
     * Any screens above it without their own navigation menu will use the first menu below them in the stack.
     * Using index 0 will change the main menu, which is guaranteed to work even if there is no screen.
     *
     * @param index     index into the stack where the menu is being added
     * @param menuResId the resource ID of the menu
     * @param listener  a listener to respond to onNavigationItemSelected calls
     */
    private void setScreenNavMenu(int index, int menuResId, OnItemSelectedListener listener) {
        Screen screen = screenStack.get(index);
        screen.listener = listener;
        screen.menuResId = menuResId;
        screen.uuid = UUID.randomUUID();

        refreshNavBar();
    }

    /**
     * Set which button in the nav bar is visually selected.
     *
     * @param itemId the id of the button to select
     * @throws IllegalStateException when no nav menu has been set
     */
    public void selectNavItem(int itemId) {
        if (navBar != null) {
            navBar.setSelectedItemId(itemId);
        }

        Screen menuScreen = getCurrentMenuScreen();
        if (menuScreen != null) {
            menuScreen.menuSelectedItemId = itemId;
        } else {
            throw new IllegalStateException("A nav menu must be set before selection an item on it");
        }
    }

    /**
     * Refreshing the nav menu with any changes that have happened
     */
    private void refreshNavBar() {
        Screen menuScreen = getCurrentMenuScreen();

        if (navBar != null) {
            if (menuScreen == null) {
                clearNavBar();
            } else if (displayedScreenUUID == null || displayedScreenUUID != menuScreen.uuid) {
                inflateIntoNavBar(menuScreen);
            }
        }
    }

    /**
     * Opens a new menu in the bottom nav bar, while clearing the old one
     */
    private void inflateIntoNavBar(Screen screen) {
        clearNavBar();
        navBar.inflateMenu(screen.menuResId);
        displayedScreenUUID = screen.uuid;

        // clear selection
        MenuItem previouslySelected = navBar.getMenu().findItem(navBar.getSelectedItemId());
        if (previouslySelected != null) {
            previouslySelected.setChecked(false);
        }

        if (screen.menuSelectedItemId != null) {
            // wait for the nav bar to finish setting up
            MenuItem toBeSelected = navBar.getMenu().findItem(screen.menuSelectedItemId);
            navBar.post(() -> {
                toBeSelected.setChecked(true);
            });
        }
    }

    /**
     * Clears the nav bar menu so it has no buttons
     */
    private void clearNavBar() {
        navBar.getMenu().clear();
        displayedScreenUUID = null;
    }

    /**
     * Checks if there are no screen fragments.
     * There will still be one empty screen in the stack to hold the main menu
     *
     * @return if there are no screen fragments
     */
    public boolean isEmpty() {
        return screenStack.size() <= 1 && screenStack.get(0).fragment == null;
    }

    /**
     * Finds the top-most screen with a nav menu assigned to it using {@link #setScreenNavMenu(int, int, OnItemSelectedListener)}.
     * This screen has the menu that should be rendered to the nav bar.
     *
     * @return the screen with the current nav menu
     */
    private Screen getCurrentMenuScreen() {
        for (int i = screenStack.size() - 1; i >= 0; i--) {
            Screen screen = screenStack.get(i);
            if (screen.menuResId != null) return screen;
        }

        return null;
    }
}
