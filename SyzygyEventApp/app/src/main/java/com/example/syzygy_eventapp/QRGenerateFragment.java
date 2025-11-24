package com.example.syzygy_eventapp;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import javax.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.OutputStream;

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

    /**
     * Constructor for QRGenerateFragment
     *
     * @param event The event that needs to have a QR generated
     * @param navStack Navigation stack to manage screens
     */
    public QRGenerateFragment(Event event, NavigationStackFragment navStack) {
        this.event = event;
        this.navStack = navStack;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_qr_generate, container, false);

        // Initialize views
        qrCodeImageView = view.findViewById(R.id.qr_code_image);
        eventTitleText = view.findViewById(R.id.event_title_text);
        exportButton = view.findViewById(R.id.export_button);
        closeButton = view.findViewById(R.id.close_button);

        // Set the event title
        if (event != null) {
            eventTitleText.setText(event.getName());
        }

        // Generate and display QR Code
        generateQRCode();

        // Set up listeners for ther buttons
        exportButton.setOnClickListener(v -> exportQRCode());
        closeButton.setOnClickListener(v -> {
            if (navStack != null) {
                navStack.popScreen();
            }
        });

        return view;

    }

    /**
     * Generates a QR code that contains the eventID for the given event
     * The QR Code data will be in the format "syzygy://event/{eventID}
     */
    private void generateQRCode() {

        if (event == null || event.getEventID() == null) {
            Toast.makeText(requireContext(), "Error: Invalid event", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create QR code data in a deep link format
        String qrData = "syzygy://event/" + event.getEventID();

        try {
            // Generate the QR code
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            // Convert Bit Matrix to Bitmap
            qrCodeBitmap = Bitmap.createBitmap(QR_CODE_SIZE, QR_CODE_SIZE, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < QR_CODE_SIZE; x++) {
                for (int y = 0; y < QR_CODE_SIZE; y++) {
                    qrCodeBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            // Display the QR code
            qrCodeImageView.setImageBitmap(qrCodeBitmap);
        }
        catch (WriterException e) {
            Toast.makeText(requireContext(), "Failed to generate QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportQRCode() {
        if (qrCodeBitmap == null) {
            Toast.makeText(requireContext(), "No QR code to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create filename
            String fileName = "QR_" + event.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".png";

            // Save to MediaStore (works for Android 10+)
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SyzygyQR");

            Uri uri = requireContext().getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();
                    Toast.makeText(requireContext(), "QR code saved to Pictures/SyzygyQR",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
        catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to export: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
