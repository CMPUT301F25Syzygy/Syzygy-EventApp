package com.example.syzygy_eventapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener;

import java.util.Stack;

/**
 * Manages a stack of different screens (fragments) that can be pushed and popped.
 * This allows one screen to open a sub screen and then return back.
 * Allows displays a bottom navigation bar which screens can customize and then listen to.
 */
public class NavigationStackFragment extends Fragment implements OnItemSelectedListener {
    private static class Screen {
        public Fragment fragment;
        public OnItemSelectedListener listener = null;
        public Integer menuResId = null;
        public Integer menuSelectedItemId = null;

        Screen(Fragment fragment) {
            this.fragment = fragment;
        }
    }

    final private Stack<Screen> screenStack = new Stack<>();
    private BottomNavigationView navBar;
    private Screen displayedScreen = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_navigation_stack, container, false);

        navBar = view.findViewById(R.id.bottom_nav_bar);

        refreshNavBar();

        navBar.setOnItemSelectedListener(this);

        return view;
    }

    /**
     * Handles when a nav item is clicked, calling whatever callback is associated.
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
     * @param fragment a fragment with new screen contents
     */
    public void pushScreen(Fragment fragment) {
        FragmentManager manager = getParentFragmentManager();
        manager.beginTransaction()
                .replace(R.id.fragment_frame, fragment)
                .commit();
        
        screenStack.push(new Screen(fragment));
        refreshNavBar();
    }

    /**
     * Remove the screen on the top of the stack. Goes back to a previous screen.
     */
    public void popScreen() {
        Screen screen = screenStack.pop();

        FragmentManager manager = getParentFragmentManager();
        manager.beginTransaction()
                .replace(R.id.fragment_frame, screen.fragment)
                .commit();

        refreshNavBar();
    }

    /**
     * Replaces the screen at the top of the stack, keeping whatever nav menu may have had.
     * This means the old screen's onNavigationItemSelected listener can still be called.
     * @param fragment a fragment to replace the screen contents
     */
    public void replaceScreen(Fragment fragment) {
        FragmentManager manager = getParentFragmentManager();
        manager.beginTransaction()
                .replace(R.id.fragment_frame, fragment)
                .commit();

        if (screenStack.isEmpty()) {
            screenStack.push(new Screen(fragment));
        } else {
            Screen screen = screenStack.peek();
            screen.fragment = fragment;
        }

        refreshNavBar();
    }

    /**
     * Calls {@link #setScreenNavMenu(int, int, OnItemSelectedListener)} with the index of the top stack item
     */
    public void setScreenNavMenu(int menuResId, OnItemSelectedListener listener) {
        setScreenNavMenu(screenStack.size() - 1 , menuResId, listener);
    }

    /**
     * Sets the navigation menu for a screen.
     * Any screens above it without their own navigation menu will use the first menu below them in the stack.
     * Using index 0 will change the main menu.
     * @param index index into the stack where the menu is being added
     * @param menuResId the resource ID of the menu
     * @param listener a listener to respond to onNavigationItemSelected calls
     */
    public void setScreenNavMenu(int index, int menuResId, OnItemSelectedListener listener) {
        Screen screen = screenStack.get(index);
        screen.listener = listener;
        screen.menuResId = menuResId;

        refreshNavBar();
    }

    /**
     * Refreshing the nav menu with any changes that have happened
     */
    private void refreshNavBar() {
        Screen menuScreen = getCurrentMenuScreen();

        if (navBar != null) {
            if (menuScreen == null) {
                clearNavBar();
            } else if (displayedScreen == null || !displayedScreen.equals(menuScreen)) {
                System.out.println(displayedScreen);
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
        displayedScreen = screen;

        if (screen.menuSelectedItemId != null) {
            // wait for the nav bar to finish setting up
            navBar.post(() -> {
                navBar.setSelectedItemId(screen.menuSelectedItemId);
            });
        } else {
            screen.menuSelectedItemId = navBar.getSelectedItemId();
        }
    }

    /**
     * Clears the nav bar menu so it has no buttons
     */
    private void clearNavBar() {
        navBar.getMenu().clear();
        displayedScreen = null;
    }

    /**
     * Finds the top-most screen with a nav menu assigned to it using {@link #setScreenNavMenu(int, int, OnItemSelectedListener)}.
     * This screen has the menu that should be rendered to the nav bar.
     * @return the screen with the current nav menu
     */
    private Screen getCurrentMenuScreen() {
        for(int i = screenStack.size() - 1; i >= 0; i--) {
            Screen screen = screenStack.get(i);
            if (screen.menuResId != null) return screen;
        }

        return null;
    }
}