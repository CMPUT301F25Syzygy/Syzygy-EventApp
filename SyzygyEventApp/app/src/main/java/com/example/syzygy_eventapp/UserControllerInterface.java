package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.function.Consumer;

public interface UserControllerInterface {
    public Task<User> createUser();
    public Task<User> getUser(String userID);
    public ListenerRegistration observeUser(String userID, Consumer<User> onUpdate, Runnable onDelete);
    public Task<Void> updateFields(String userID, HashMap<String, Object> fields);
    public Task<Void> deleteUser(String userID);
}
