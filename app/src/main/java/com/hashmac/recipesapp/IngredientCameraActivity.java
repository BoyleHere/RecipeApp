package com.hashmac.recipesapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.hashmac.recipesapp.databinding.ActivityIngredientCameraBinding;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IngredientCameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private ActivityIngredientCameraBinding binding;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private TFLiteImageClassifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIngredientCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }

        classifier = new TFLiteImageClassifier(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        binding.btnCapture.setOnClickListener(view -> takePhoto());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(new ByteArrayOutputStream()).build();

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = ImageUtils.imageProxyToBitmap(image);
                image.close();

                List<String> ingredients = classifier.classify(bitmap);
                if (ingredients.isEmpty()) {
                    Toast.makeText(IngredientCameraActivity.this, "No ingredients detected", Toast.LENGTH_SHORT).show();
                    return;
                }

                String query = String.join(" ", ingredients);
                Intent intent = new Intent(IngredientCameraActivity.this, AllRecipesActivity.class);
                intent.putExtra("type", "search");
                intent.putExtra("query", query);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                super.onError(exception);
                Toast.makeText(IngredientCameraActivity.this, "Capture failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
