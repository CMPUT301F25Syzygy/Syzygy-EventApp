package com.example.syzygy_eventapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationBarView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// https://developer.android.com/media/camera/camerax --> CameraX to capture QR image
// https://developer.android.com/media/camera/camerax/mlkitanalyzer#java --> ML Kit Analyzer
//    --> "Barcodes" should include QR Codes!

/**
 * QRScanFragment: scans QR codes and opens EventView when valid codes are detected
 */
public class QRScanFragment extends Fragment {
    private NavigationStackFragment navStack;
    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private boolean isProcessingQR = false;

    // permissions launcher
    private ActivityResultLauncher<String> requestPermissionLauncher;

    public QRScanFragment(NavigationStackFragment navStack) {
        this.navStack = navStack;
    }

    @Override
    public void onResume() {
        super.onResume();
        // allow scanning again when returning from an EventView
        isProcessingQR = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_scan, container, false);

        previewView = view.findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Setup permission launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(requireContext(),
                                "Camera permission is required to scan QR codes",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Back button menu
        navStack.setScreenNavMenu(R.menu.back_nav_menu, (i) -> {
            navStack.popScreen();
            return true;
        });

        // Check if perms are granted, otherwise, launch the request (launcher set up in OnCreate)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        // Get a camera provider asynchronously
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        // After camera system is ready, retrieve the camera process and run bindCameraUseCases
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * Sets up and binds the CameraX use cases for this fragment:
     * - Clears any existing use cases
     * - Displays a live camera preview inside {@link PreviewView}
     * - Analyzes camera frames using ML Kit to detect QR codes
     * - Automatically binds to this fragment's lifecycle
     * @param cameraProvider the {@link ProcessCameraProvider} managing camera lifecycle and use cases
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        // Unbind anything before rebinding to prevent crahses ("clean slate")
        cameraProvider.unbindAll();

        // Preview view will display the live camera to the feed and connect to the UI
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Use back camera by default
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        // Make a barcode scanner from ML Kit
        BarcodeScanner scanner = BarcodeScanning.getClient();

        // Make an image analysis use case
        // --> A continuous stream of camera frames that will be processed in real time
        // --> Keep only the latest frame
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // attach analyzer function
        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            analyzeImage(scanner, imageProxy);
        });

        // Bind everything to the fragment lifecycle
        cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
        );
    }

    /**
     * This method actually extracts and processes QR codes from the camera feed.
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(BarcodeScanner scanner, ImageProxy imageProxy) {
        // Each image frame sent by CameraX comes as an ImageProxy that contains image (meta)data.
        android.media.Image mediaImage = imageProxy.getImage();

        // If image exists, process it, else close and skip
        // Not closing the ImageProxy will cause CameraX to stall
        if (mediaImage != null) {
            // Get InputImage for ML Kit processing
            InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    // Ensure barcode detection works even if the phone is rotated
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            // Send frame to ML Kit's barcode scanner, call handleDetectedBarcodes if a QR Code is detected
            scanner.process(image)
                    .addOnSuccessListener(barcodes -> handleDetectedBarcodes(barcodes))
                    .addOnFailureListener(Throwable::printStackTrace)
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    /**
     * Handles detected QR codes directly; Will scan and open events.
     */
    private void handleDetectedBarcodes(List<Barcode> barcodes) {
        // Pause scanning so the same QR Code doesn't keep being processed
        if (isProcessingQR) {
            return;
        }

        for (Barcode barcode : barcodes) {
            String rawValue = barcode.getRawValue();
            if (rawValue != null) {
                String eventID = extractEventID(rawValue);

                if (eventID != null) {
                    // Set isProcessingQR to true
                    isProcessingQR = true;

                    // Toast indicating a successful scan
                    Toast.makeText(requireContext(), "QR Code scanned", Toast.LENGTH_SHORT).show();

                    // Get event from firestore
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("events").document(eventID)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot != null && snapshot.exists()) {
                                    // Check if event is within the registration period
                                    Timestamp registrationStart = snapshot.getTimestamp("registrationStart");
                                    Timestamp registrationEnd = snapshot.getTimestamp("registrationEnd");
                                    Date now = new Date();

                                    // No registration date provided
                                    if (registrationStart == null || registrationEnd == null) {
                                        Toast.makeText(requireContext(), "This event has incomplete registration info.", Toast.LENGTH_SHORT).show();
                                    }
                                    // Too early
                                    else if (now.before(registrationStart.toDate())) {
                                        Toast.makeText(requireContext(), "Registration for this event has not started yet.", Toast.LENGTH_SHORT).show();
                                    }
                                    // Too late/expired
                                    else if (now.after(registrationEnd.toDate())) {
                                        Toast.makeText(requireContext(), "Registration for this event has ended.", Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        // Event was found and is within registration period, use the navStack to navigate to the scanned event's details
                                        navStack.replaceScreen(new EventFragment(navStack, eventID));
                                    }
                                }
                                else {
                                    Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
                                }
                                // resume scanning
                                isProcessingQR = false;
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireContext(), "Error loading event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                // resume scanning
                                isProcessingQR = false;
                            });

                    // Stop after first valid QR is scanned
                    break;
                }
            }
        }
    }

    private String extractEventID(String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            return null;
        }

        // Check if it's in deep link format
        if (rawValue.startsWith("syzygy://event/")) {
            String eventID = rawValue.substring("syzygy://event/".length());
            // Validate it's not empty and looks like a Firestore ID
            if (!eventID.isEmpty() && eventID.length() == 20) {
                return eventID;
            }
        }
        // Fallback: check if it's a raw 20-character event ID
        else if (rawValue.length() == 20) {
            return rawValue;
        }

        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

}