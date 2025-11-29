package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public interface UserControllerInterface {
    public Task<User> createEntrant(String userID);
    public Task<Organizer> createOrganizer(String userID);
    public Task<Admin> createAdmin(String userID);
    public Task<User> getUser(String userID);
    public Task<List<User>> getUsers(List<String> userIDs);
    public ListenerRegistration observeUser(String userID, Consumer<User> onUpdate, Runnable onDelete);
    public ListenerRegistration observeAllUsers(Consumer<List<User>> onChange);
    public Task<Void> updateFields(String userID, HashMap<String, Object> fields);
    public Task<User> setUserRole(String userID, Role role);
    public Task<Void> deleteUser(String userID);
}
