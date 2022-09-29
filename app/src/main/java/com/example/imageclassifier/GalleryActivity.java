package com.example.imageclassifier;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;



public class GalleryActivity extends Activity{

    protected void onCreate(Bundle savedInstanceState, Intent data) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        selectBtn = findViewById(R.id.selectBtn);
        selectBtn.setOnClickListener(v -> getImageFromGallery(data));
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        cls = new ClassifierWithModel(this);
        try {
            cls.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static final String TAG = "[IC]GalleryActivity";
    public static final int GALLERY_IMAGE_REQUEST_CODE = 1;
    private ClassifierWithModel cls;
    private ImageView imageView;
    private TextView textView;
    private Button selectBtn;

    private void getImageFromGallery(Intent data){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        startActivityForResult(intent, GALLERY_IMAGE_REQUEST_CODE,data);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK &&
                requestCode == GALLERY_IMAGE_REQUEST_CODE) {
            if (data == null) {
                return;
            }
            Uri selectedImage = data.getData();
            Bitmap bitmap = null;
            try {
                if(Build.VERSION.SDK_INT >= 29) {
                    Uri fileUri = data.getData();
                    ContentResolver resolver = getContentResolver();
                    InputStream inputStream = resolver.openInputStream(fileUri);
                    bitmap = BitmapFactory.decodeStream(inputStream);
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),
                            selectedImage);
                }
            } catch (IOException ioe) {
                Log.e(TAG, "Failed to read Image", ioe);
            }
            if(bitmap != null) {
                Pair<String, Float> output = cls.classify(bitmap, 90);
                String resultStr = String.format(Locale.ENGLISH,
                        "class : %s, prob : %.2f%%",
                        output.first, output.second * 100);
                textView.setText(resultStr);
                imageView.setImageBitmap(bitmap);
            }
        }
    }


}
