package com.example.syzygy_eventapp;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the connection to the remote Firestore
 * database, including read and write operations.
 */
@RunWith(AndroidJUnit4.class)
public class FirestoreConnectionTest {

    /// The object representing the Firebase application
    private FirebaseApp app;

    /// The object representing the Firestore database.
    private FirebaseFirestore firestore;

    /// The name of the collection to use for testing.
    private static final String TEST_COLLECTION = "test_connection";

    /// The name of the document in the test collection to use for testing.
    private static final String TEST_DOCUMENT = "connection_test_doc";

    /// How long the test should wait before failing due to timeout.
    private static final int TIMEOUT_SECONDS = 10;

    /**
     * Sets up the Firestore instance before each test.
     */
    @Before
    public void setUp() {
        // Initialize Firebase if not already initialized
        try {
            // Initialize Firebase
            app = FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext());
        } catch (IllegalStateException e) {
            // Firebase might already be initialized
        }

        firestore = FirebaseFirestore.getInstance();

        FirebaseOptions options = app.getOptions();
        if (!options.getProjectId().equals("syzygy-eventapp-development")) {
            throw new IllegalStateException("Connected to the wrong database for testing!");
        }
    }

    /**
     * Tests the connection to Firestore by performing
     * basic read and write operations.
     */
    @Test
    public void testFirestoreConnection() {
        // Test Firestore connection and basic operations
        try {
            // Test 1: Basic instance creation
            assertNotNull("Firestore instance should not be null", firestore);

            // Test 2: Write operation
            testWriteOperation();

            // Test 3: Read operation
            testReadOperation();

            // Test 4: Delete operation (cleanup)
            testDeleteOperation();

        } catch (Exception e) {
            // Fail the test if any operation throws an exception
            fail("Firestore connection test failed: " + e.getMessage());
        }
    }

    /**
     * Tests the ability to perform a write operation in
     * the remote Firestore database.
     * @throws ExecutionException The write operation could not complete.
     * @throws TimeoutException The write operation timed out.
     */
    private void testWriteOperation() throws Exception {
        // Prepare test data to write
        Map<String, Object> testData = new HashMap<>();
        testData.put("testField", "testValue");
        testData.put("timestamp", System.currentTimeMillis());
        testData.put("testType", "connection_test");

        // Get reference to the test document
        DocumentReference docRef = firestore.collection(TEST_COLLECTION).document(TEST_DOCUMENT);

        // Execute write operation with timeout
        Tasks.await(docRef.set(testData, SetOptions.merge()), TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // If we reach here, write was successful
        assertTrue("Write operation should complete successfully", true);
    }

    /**
     * Tests the ability to perform a read operation in
     * the remote Firestore database.
     * @throws ExecutionException The read operation could not complete.
     * @throws TimeoutException The read operation timed out.
     */
    private void testReadOperation() throws Exception {
        // Get reference to the test document
        DocumentReference docRef = firestore.collection(TEST_COLLECTION).document(TEST_DOCUMENT);

        // Execute read operation with timeout
        var documentSnapshot = Tasks.await(docRef.get(), TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Verify test document read data
        assertTrue("Document should exist", documentSnapshot.exists());
        assertTrue("Document should contain testField",
                documentSnapshot.contains("testField"));

        // Verify the value of testField
        String testValue = documentSnapshot.getString("testField");
        assertNotNull("testField should not be null", testValue);
        assertEquals("testField should match expected value", "testValue", testValue);
    }

    /**
     * Tests the ability to perform a delete operation in
     * the remote Firestore database.
     * @throws ExecutionException The delete operation could not complete.
     * @throws TimeoutException The delete operation timed out.
     */
    private void testDeleteOperation() throws Exception {
        // Get reference to the test document
        DocumentReference docRef = firestore.collection(TEST_COLLECTION).document(TEST_DOCUMENT);

        // Execute delete operation with timeout
        Tasks.await(docRef.delete(), TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Verify deletion
        var documentSnapshot = Tasks.await(docRef.get(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse("Document should be deleted", documentSnapshot.exists());
    }

    /**
     * Tests the ability to perform batch operations in
     * the remote Firestore database.
     */
    @Test
    public void testFirestoreBatchOperation() {
        try {
            // Test batch operations
            var batch = firestore.batch();

            // Prepare test documents
            DocumentReference doc1 = firestore.collection(TEST_COLLECTION).document("batch_test_1");
            DocumentReference doc2 = firestore.collection(TEST_COLLECTION).document("batch_test_2");

            // Prepare test data
            Map<String, Object> data1 = Collections.singletonMap("batchField", "value1");
            Map<String, Object> data2 = Collections.singletonMap("batchField", "value2");

            // Add set operations to batch
            batch.set(doc1, data1);
            batch.set(doc2, data2);

            // Commit batch
            Tasks.await(batch.commit(), TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Clean up
            batch = firestore.batch();
            batch.delete(doc1);
            batch.delete(doc2);
            Tasks.await(batch.commit(), TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (Exception e) {
            // Fail the test if any operation throws an exception
            fail("Batch operation test failed: " + e.getMessage());
        }
    }

    /**
     * Cleans up any test documents created during the tests.
     */
    @After
    public void tearDown() {
        // Clean up any remaining test documents
        try {
            // Get reference to the test document
            DocumentReference docRef = firestore.collection(TEST_COLLECTION).document(TEST_DOCUMENT);
            Tasks.await(docRef.delete(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}