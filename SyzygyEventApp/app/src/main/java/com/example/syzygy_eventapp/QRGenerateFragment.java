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
 *
 * The scannable QR code made by this fragment will contain the event's ID in DEEP LINK FORMAT.
 * When scanned, the code will navigate users directly to the event details page.
 * Organizers can export the QR code image to their device (and then print/share it however they like).
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

    /**
     * Inflates the layout and initializes all the UI components
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return The root view for this frament's UI
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_qr_generate, container, false);

        // Initialize views
        qrCodeImageView = view.findViewById(R.id.qr_code_image);
        eventTitleText = view.findViewById(R.id.event_title_text);
        exportButton = view.findViewById(R.id.export_button);
        closeButton = view.findViewById(R.id.close_button);

        // Set the event title, shown at the top of the screen
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
     * Generates a QR code that contains the eventID for the given event in deep link format using the ZXing library
     *
     * <p>
     *     The QR Code data (a string) will be created with the format: "syzygy://event/{eventID}.
     *     The string will then be encoded into a Bit Matrix (2D array of black and white pixels), which is then converted into a Bitmap image.
     *     The Bitmap is what will be shown in the ImageView.
     * </p>
     */
    private void generateQRCode() {
        // Validate the event ID
        if (event == null || event.getEventID() == null) {
            Toast.makeText(requireContext(), "Error: Invalid event", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create QR code data in a deep link format
        String qrData = "syzygy://event/" + event.getEventID();

        try {
            // Generate the QR code with the ZXing library
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            // Convert Bit Matrix to Bitmap by looping through each pixel in the 800x800 grid (black = true, white = false)
            qrCodeBitmap = Bitmap.createBitmap(QR_CODE_SIZE, QR_CODE_SIZE, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < QR_CODE_SIZE; x++) {
                for (int y = 0; y < QR_CODE_SIZE; y++) {
                    qrCodeBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            // Display the QR code in the ImageView
            qrCodeImageView.setImageBitmap(qrCodeBitmap);
        }
        catch (WriterException e) {
            // QR generation failed
            Toast.makeText(requireContext(), "Failed to generate QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Exports the generated QR code to the device's pics, in a dedicated Syzygy subfolder using MediaStore.
     */
    private void exportQRCode() {
        if (qrCodeBitmap == null) {
            // Validate QR code to be exported
            Toast.makeText(requireContext(), "No QR code to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create filename
            String fileName = "QR_" + event.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".png";

            // Save to MediaStore
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SyzygyQR");

            // Write the bitmap data of the QR Code to the URI for a new image entry
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
            // Export failed
            Toast.makeText(requireContext(), "Failed to export: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
