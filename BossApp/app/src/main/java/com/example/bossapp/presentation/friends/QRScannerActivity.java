package com.example.bossapp.presentation.friends;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bossapp.R;
import com.example.bossapp.business.QRCodeManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    public static final String EXTRA_USER_ID = "extra_user_id";

    private DecoratedBarcodeView barcodeView;
    private MaterialToolbar toolbar;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        toolbar = findViewById(R.id.toolbar);
        barcodeView = findViewById(R.id.barcode_scanner);

        setupToolbar();
        checkCameraPermission();
    }

    private void setupToolbar() {
        toolbar.setTitle("Scan QR Code");
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            startScanning();
        }
    }

    private void startScanning() {
        if (isScanning) return;

        isScanning = true;
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null && result.getText() != null) {
                    handleQRCodeScanned(result.getText());
                }
            }
        });
        barcodeView.resume();
    }

    private void handleQRCodeScanned(String qrContent) {
        // Stop scanning
        barcodeView.pause();
        isScanning = false;

        // Validate QR code
        if (!QRCodeManager.isValidUserQR(qrContent)) {
            Toast.makeText(this, "Invalid QR code. Please scan a BossApp user QR code.",
                    Toast.LENGTH_LONG).show();
            // Allow scanning again
            isScanning = false;
            barcodeView.resume();
            return;
        }

        // Extract user ID
        String scannedUserId = QRCodeManager.extractUserIdFromQR(qrContent);

        if (scannedUserId != null) {
            // Return the scanned user ID to the calling activity
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_USER_ID, scannedUserId);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Failed to read QR code", Toast.LENGTH_SHORT).show();
            isScanning = false;
            barcodeView.resume();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isScanning) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        barcodeView.pause();
    }
}