package com.example.syzygy_eventapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.ListenerRegistration;


/**
 * A view representing info about a given {@link User}.
 * Use the {@link UserView#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserView extends Fragment {

    /// The argument key for the user ID.
    private static final String ARG_USER_ID = "userId";
    /// The ID of the user to display.
    private String userID;

    /// The text view for the user's name.
    private TextView nameText;
    /// The text view for the user's email.
    private TextView emailText;
    /// The text view for the user's phone number.
    private TextView phoneText;
    /// The chip displaying the user's role.
    private Chip roleChip;
    /// The image view for the user's profile picture.
    private ShapeableImageView profileImage;

    /// Listener registration for user data updates.
    private ListenerRegistration listenerRegistration;

    /**
     * Required empty public constructor.
     */
    public UserView() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param userID The ID of the {@link User} to construct
     *               this fragment from.
     * @return A new instance of fragment UserView.
     */
    public static UserView newInstance(String userID) {
        UserView fragment = new UserView();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userID);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Initializes the fragment and retrieves the user ID from arguments.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.userID = getArguments().getString(ARG_USER_ID);
        }
    }

    /**
     * Inflates the layout and initializes UI components.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_view, container, false);

        // Initialize UI components
        nameText = view.findViewById(R.id.user_name);
        emailText = view.findViewById(R.id.user_email);
        phoneText = view.findViewById(R.id.user_phone);
        roleChip = view.findViewById(R.id.user_role_chip);
        profileImage = view.findViewById(R.id.user_profile_image);

        fetchUser();

        return view;
    }

    /**
     * Fetches the user data and sets up a listener for real-time updates.
     */
    private void fetchUser() {
        if (userID == null) return;

        UserController.getInstance().observeUser(userID, user -> {
            // Update UI when user changes
            nameText.setText(user.getName());
            emailText.setText(user.getEmail());
            phoneText.setText(user.getPhone());
            roleChip.setText(user.getRole().toString());
            // TODO: set profile image if available
        }, () -> {
            // Handle user deleted
            Toast.makeText(getContext(), "User deleted", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Cleans up the listener when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listener if needed
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
