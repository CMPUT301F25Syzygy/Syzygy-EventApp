package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;

public class BatchDeleter {
    static final private int batchSize = 256;

    static final private FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Deletes all the documents in a whole collection.
     * runs batches recursively until everything is cleaned up.
     * @param collectionName the name of the collection to delete
     * @return a task that finishes when deletion is done
     */
    static public Task<Void> deleteCollection(String collectionName) {
        CollectionReference collection = db.collection(collectionName);
        return deleteQueryBatch(collection.limit(batchSize));
    }

    /**
     * Deletes a list of documents in a collection in one batch.
     * @param collectionName the name of the collection to delete
     * @param ids the ids of documents to delete in the collection
     * @return a task that finishes when deletion is done
     */
    static public Task<Void> deleteCollectionIds(String collectionName, ArrayList<String> ids) {
        if (ids.size() > batchSize) {
            throw new IllegalArgumentException("Too many documents, can't delete");
        }

        CollectionReference collectionRef = db.collection(collectionName);

        WriteBatch batch = db.batch();

        for (String id : ids) {
            DocumentReference docRef = collectionRef.document(id);
            batch.delete(docRef);
        }

        return batch.commit();
    }

    static private Task<Void> deleteQueryBatch(Query query) {
        return query.get().continueWithTask(task -> {
            QuerySnapshot snapshot = task.getResult();
            WriteBatch batch = db.batch();

            // delete in batch
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                batch.delete(doc.getReference());
            }

            return batch.commit().continueWithTask(commitTask -> {
                // recursively delete more
                if (snapshot.size() >= batchSize) {
                    return deleteQueryBatch(query);
                } else {
                    return Tasks.forResult(null);
                }
            });
        });
    }
}
