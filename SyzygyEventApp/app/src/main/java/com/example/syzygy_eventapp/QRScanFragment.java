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
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;

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

    // permissions launcher
    private ActivityResultLauncher<String> requestPermissionLauncher;

    public QRScanFragment(NavigationStackFragment navStack) {
        this.navStack = navStack;
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

        // back button menu
        navStack.setScreenNavMenu(R.menu.back_nav_menu, (i) -> {
            navStack.popScreen();
            return true;
        });

        // check if perms are granted, otherwise, launch the request (launcher set up in OnCreate)
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
    private void analyzeImage(BarcodeScanner scanner, ImageProxy imageProxy) {

    }


}