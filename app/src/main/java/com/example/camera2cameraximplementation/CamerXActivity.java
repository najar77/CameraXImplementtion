package com.example.camera2cameraximplementation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CamerXActivity extends AppCompatActivity {

    PreviewView previewView;
    Button capture,rotate;

    ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;
    ImageCapture imageCapture;
    File cacheDirectory;
    File dcim;

    boolean camFace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camer_xactivity);

        previewView = findViewById(R.id.previewView);
        capture = findViewById(R.id.captureButton);
        rotate = findViewById(R.id.rotateButton);
        cacheDirectory = new File(getExternalCacheDir(), "image_cache");
        dcim = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camerx");
        if (!cacheDirectory.exists()) {
            if (!cacheDirectory.mkdirs()) {
                Log.e("CameraXActivity", "Failed to create cache directory");
            }
        }
        if (!dcim.exists()) {
            if (!dcim.mkdirs()) {
                Log.e("CameraXActivity", "Failed to create dcim directory");
            }
        }

        camFace = true;
        startCamera();
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        rotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camFace = !camFace;
                startCamera();
            }
        });


    }

    private void takePicture() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        String fileName = "IMG_" + sdf.format(System.currentTimeMillis()) + ".jpg";
        File outputFile = new File(dcim, fileName);
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions
                .Builder(outputFile)
                .build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
        new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                File savedFile = outputFileResults.getSavedUri() != null ?
                        new File(outputFileResults.getSavedUri().getPath()) : outputFile;

                // Load the captured image
                Bitmap capturedBitmap = BitmapFactory.decodeFile(savedFile.getAbsolutePath());

                // Check the EXIF orientation
                int orientation = getExifOrientation(savedFile.getAbsolutePath());

                // Apply orientation correction if needed
                if (orientation != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientation); // Rotate based on EXIF orientation
                    Bitmap correctedBitmap = Bitmap.createBitmap(capturedBitmap, 0, 0, capturedBitmap.getWidth(), capturedBitmap.getHeight(), matrix, true);
                    capturedBitmap.recycle(); // Recycle the original bitmap
                    capturedBitmap = correctedBitmap; // Use the corrected bitmap
                }
                // Flip the image horizontally
                Matrix matrix = new Matrix();
                if (camFace) {
                    matrix.preScale(-1, 1); // Horizontal flip
                }
                Bitmap flippedBitmap = Bitmap.createBitmap(capturedBitmap, 0, 0, capturedBitmap.getWidth(), capturedBitmap.getHeight(), matrix, false);

                // Save the flipped Bitmap to a new file or overwrite the existing one
                File flippedFile = new File(outputFile.getAbsolutePath());
                saveBitmapToFile(flippedBitmap, flippedFile);

                String msg = "Image saved: " + outputFile.getAbsolutePath();
                Toast.makeText(CamerXActivity.this, msg, Toast.LENGTH_LONG).show();
                MediaScannerConnection.scanFile(
                        getBaseContext(), new String[]{savedFile.getAbsolutePath()}, null,
                        (path, uri) -> {
                            // Image has been scanned and is now visible in the gallery
                        }
                );
                Intent resultIntent = new Intent();
                resultIntent.putExtra("data", outputFile.getAbsolutePath());
                // Set the result code to indicate success
                setResult(RESULT_OK, resultIntent);
                finish();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {

            }
        });
    }

    private int getExifOrientation(String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }


    private void saveBitmapToFile(Bitmap bitmap, File file) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> providerListenableFuture = ProcessCameraProvider.getInstance(this);

        providerListenableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = providerListenableFuture.get();
                    bindCameraUses(cameraProvider);

                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUses(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = camFace ?
                CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build();

        try {
            cameraProvider.unbindAll();

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}