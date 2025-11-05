package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.function.Consumer;

public class UserControllerMock implements UserControllerInterface {
    public Task<User> createEntrant() {
        return Tasks.forResult(new User());
    }

    public Task<Organizer> createOrganizer() {
        return Tasks.forResult(new Organizer());
    }

    public Task<Admin> createAdmin() {
        return Tasks.forResult(new Admin());
    }

    public Task<User> getUser(String userID) {
        return Tasks.forResult(new User());
    }

    public ListenerRegistration observeUser(String userID, Consumer<User> onUpdate, Runnable onDelete) {
        return () -> {
        };
    }

    public Task<Void> updateFields(String userID, HashMap<String, Object> fields) {
        return Tasks.forResult(null);
    }

    public Task<User> setUserRole(String userID, Role role) {
        return Tasks.forResult(new User());
    }

    public Task<Void> deleteUser(String userID) {
        return Tasks.forResult(null);
    }
}