package com.example.imageclassifier;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.util.Locale;

public class CameraActivity extends Activity {
    public static final String TAG = "[IC]CameraActivity";
    public static final int CAMERA_IMAGE_REQUEST_CODE = 1;
    private static final String KEY_SELECTED_URI = "KEY_SELECTED_URI";

    private ClassifierWithModel cls;
    private ImageView imageView;
    private TextView textView;

    Uri selectedImageUri;

    private void getImageFromCamera(){
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_IMAGE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK &&
                requestCode == CAMERA_IMAGE_REQUEST_CODE) {
            Bitmap bitmap = null;
            try {
                if(Build.VERSION.SDK_INT >= 29) {
                    bitmap = (Bitmap) data.getExtras().get("data");
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(
                             getContentResolver(), selectedImageUri);
                }
            } catch (IOException ioe) {
                Log.e(TAG, "Failed to read Image", ioe);
            }
            if(bitmap != null) {
                Pair<String, Float> output = cls.classify(bitmap);
                String resultStr = String.format(Locale.ENGLISH,
                        "class : %s, prob : %.2f%%",
                        output.first, output.second * 100);
                imageView.setImageBitmap(bitmap);
                textView.setText(resultStr);
            }
        }
    }

    //Activity 의 데이터를 유지하기 위한 메소드
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_SELECTED_URI, selectedImageUri);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Button takeBtn = findViewById(R.id.takeBtn);
        takeBtn.setOnClickListener(v -> getImageFromCamera());
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        cls = new ClassifierWithModel(this);
        try {
            cls.init();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        if(savedInstanceState != null) {
            Uri uri = savedInstanceState.getParcelable(KEY_SELECTED_URI);
            if (uri != null)
                selectedImageUri = uri;
        }
    }

    @Override
    protected void onDestroy() {
        cls.finish();
        super.onDestroy();
    }



}
