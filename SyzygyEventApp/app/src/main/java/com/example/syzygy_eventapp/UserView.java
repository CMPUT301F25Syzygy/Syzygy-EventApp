package com.example.syzygy_eventapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserView#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserView extends Fragment {

    private static final String ARG_USERNAME = "userName";
    private static final String ARG_EMAIL = "email";
    private static final String ARG_PHONENUMBER = "phoneNumber";
    private static final String ARG_ROLE = "role";

    private String userName;
    private String email;
    private String phoneNumber;
    private String role;

    public UserView() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param user The {@link User} to construct this fragment from.
     * @return A new instance of fragment UserView.
     */
    // TODO: Rename and change types and number of parameters
    public static UserView newInstance(User user) {
        UserView fragment = new UserView();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, user.getName());
        args.putString(ARG_EMAIL, user.getEmail());
        args.putString(ARG_PHONENUMBER, user.getPhone());
        args.putString(ARG_ROLE, user.getRole().toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.userName = getArguments().getString(ARG_USERNAME);
            this.email = getArguments().getString(ARG_EMAIL);
            this.phoneNumber = getArguments().getString(ARG_PHONENUMBER);
            this.role = getArguments().getString(ARG_ROLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_view, container, false);
    }
}