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
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationBarView;
import com.google.common.util.concurrent.ListenableFuture;

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

    // permissions launcher
    private ActivityResultLauncher<String> requestPermissionLauncher;

    public QRScanFragment(NavigationStackFragment navStack) {
        this.navStack = navStack;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_scan, container, false);

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
        // gives ProcessCameraProvider when it's ready
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

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        PreviewView previewView = requireView().findViewById(R.id.previewView);

        // unbind anything before rebinding to prevent crahses
        cameraProvider.unbindAll();

        // 1. Build the preview use case
        androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // 2. Build the image analysis use case for barcode scanning (QR codes are considered as a type of barcode)
        androidx.camera.core.ImageAnalysis imageAnalysis =
                new androidx.camera.core.ImageAnalysis.Builder()
                        .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        // 3. Set up ML Kit Barcode Scanner
        com.google.mlkit.vision.barcode.BarcodeScanner scanner =
                com.google.mlkit.vision.barcode.BarcodeScanning.getClient();
    }
}