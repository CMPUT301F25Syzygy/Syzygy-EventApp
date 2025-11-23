package com.example.syzygy_eventapp;

import android.graphics.Bitmap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

/**
 * This fragment will generatr and display QR Codes for events.
 */
public class QRGenerateFragment extends Fragment {

    private static final String TAG = "QRGenerateFragment";
    private static final int QR_CODE_SIZE = 800;

    private ImageView qrCodeImageView;
    private TextView eventTitleText;
    private Button exportButton;
    private Button closeButton;

    private Event event;
    private Bitmap qrCodeBitmap;
    private NavigationStackFragment navStack;
}
