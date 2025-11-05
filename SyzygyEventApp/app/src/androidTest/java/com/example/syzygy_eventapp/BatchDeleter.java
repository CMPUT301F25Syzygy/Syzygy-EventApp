package com.example.syzygy_eventapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

public class BatchDeleter {
    static final private int batchSize = 256;

    static final private FirebaseFirestore db = FirebaseFirestore.getInstance();

    static public Task<Void> deleteCollection(String name) {
        CollectionReference collection = db.collection(name);
        return deleteQueryBatch(collection.limit(batchSize), batchSize);
    }

    static private Task<Void> deleteQueryBatch(Query query, int batchSize) {
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
                    return deleteQueryBatch(query, batchSize);
                } else {
                    return Tasks.forResult(null);
                }
            });
        });
    }
}
