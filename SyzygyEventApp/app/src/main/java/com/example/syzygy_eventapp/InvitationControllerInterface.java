package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.function.Consumer;

public interface InvitationControllerInterface {
    public Task<List<String>> createInvites(String event, String organizerID, List<String> recipientIDs);
    public Task<Void> accept(String invitationID, String userID);
    public Task<Void> reject(String invitationID, String userID);
    public Task<Void> cancel(String invitation, String organizerID);
    public ListenerRegistration observeEventInvitations(String event, Consumer<List<Invitation>> onChange, Consumer<Exception> onError);
    public ListenerRegistration observePending(String recipientID, Consumer<List<Invitation>> onChange, Consumer<Exception> onError) ;
}
