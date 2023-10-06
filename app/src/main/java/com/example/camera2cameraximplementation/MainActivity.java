package com.example.camera2cameraximplementation;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Button camera;
    Button frag;
    ImageView image;
    private static final int REQUEST_CAPTURE_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera = findViewById(R.id.Camera);
        image = findViewById(R.id.iv);
        // Check if the app has camera permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Camera permission is not granted; request it
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1);
        } else {
            // Camera permission is already granted; you can proceed with camera-related operations
        }


        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), CamerXActivity.class);
                startActivityForResult(i,REQUEST_CAPTURE_IMAGE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CAPTURE_IMAGE && resultCode == RESULT_OK && data != null) {
            String path =  data.getStringExtra("data");
            RequestOptions requestOptions = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Disable caching
                    .skipMemoryCache(true) // Skip caching in memory
                    .centerCrop(); // Crop the image if needed
            Glide.with(this)
                    .load(path)
//                    .apply(requestOptions)
                    .into(image);
        }
    }
}